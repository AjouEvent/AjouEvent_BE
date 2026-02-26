'use strict';

const { Client } = require('@notionhq/client');
const fs = require('fs');
const https = require('https');

const REPO_NAME = 'AjouEvent/AjouEvent_BE';

const notion = new Client({ auth: process.env.NOTION_TOKEN });

const ISSUE_DB_ID = process.env.NOTION_ISSUE_DB_ID;
const PR_DB_ID = process.env.NOTION_PR_DB_ID;
const GITHUB_TOKEN = process.env.GITHUB_TOKEN;
const EVENT_NAME = process.env.GITHUB_EVENT_NAME;
const EVENT_PATH = process.env.GITHUB_EVENT_PATH;

// Fetch a resource from the GitHub REST API
function githubRequest(path) {
  return new Promise((resolve, reject) => {
    const options = {
      hostname: 'api.github.com',
      path,
      headers: {
        Authorization: `Bearer ${GITHUB_TOKEN}`,
        Accept: 'application/vnd.github.v3+json',
        'User-Agent': 'notion-sync-action',
      },
    };
    https
      .get(options, (res) => {
        let data = '';
        res.on('data', (chunk) => (data += chunk));
        res.on('end', () => {
          try {
            resolve(JSON.parse(data));
          } catch (e) {
            reject(e);
          }
        });
      })
      .on('error', reject);
  });
}

// Query a Notion database for a page whose URL property matches the given value
async function queryNotionByUrl(dbId, propName, url) {
  const response = await notion.databases.query({
    database_id: dbId,
    filter: { property: propName, url: { equals: url } },
  });
  return response.results[0] || null;
}

// Build the Notion property object for an issue
function buildIssueProperties(issue, statusValue) {
  const props = {
    Issue: { title: [{ text: { content: issue.title } }] },
    Repository: { rich_text: [{ text: { content: REPO_NAME } }] },
    Num: { number: issue.number },
    'Github Issue': { url: issue.html_url },
    'GitHub State': { select: { name: issue.state } },
    Labels: {
      multi_select: (issue.labels || []).map((l) => ({ name: l.name })),
    },
    Assignees: {
      multi_select: (issue.assignees || []).map((a) => ({ name: a.login })),
    },
    Author: { rich_text: [{ text: { content: issue.user.login } }] },
    'Created At': { date: { start: issue.created_at } },
    'Updated At': { date: { start: issue.updated_at } },
  };
  if (statusValue !== undefined) {
    props.Status = { select: { name: statusValue } };
  }
  return props;
}

// Upsert an issue into the Notion Issue DB
async function upsertIssue(issue, action) {
  const existing = await queryNotionByUrl(
    ISSUE_DB_ID,
    'Github Issue',
    issue.html_url
  );

  let statusValue;
  if (!existing) {
    statusValue = 'Todo';
  } else if (action === 'closed') {
    statusValue = 'Done';
  }
  // statusValue remains undefined when updating without a status change

  const properties = buildIssueProperties(issue, statusValue);

  if (!existing) {
    const page = await notion.pages.create({
      parent: { database_id: ISSUE_DB_ID },
      properties,
    });
    console.log(`Created Notion page for issue #${issue.number}`);
    return page;
  } else {
    const page = await notion.pages.update({
      page_id: existing.id,
      properties,
    });
    console.log(`Updated Notion page for issue #${issue.number}`);
    return page;
  }
}

// Ensure a referenced issue exists in Notion; create it if missing.
// Returns the Notion page object.
async function ensureIssueInNotion(issueNumber) {
  const issueUrl = `https://github.com/${REPO_NAME}/issues/${issueNumber}`;
  let page = await queryNotionByUrl(ISSUE_DB_ID, 'Github Issue', issueUrl);
  if (page) return page;

  // Fetch the issue from GitHub and create it in Notion
  const issue = await githubRequest(
    `/repos/${REPO_NAME}/issues/${issueNumber}`
  );
  if (!issue || issue.message) {
    console.warn(
      `Could not fetch issue #${issueNumber} from GitHub: ${
        issue ? issue.message : 'unknown error'
      }`
    );
    return null;
  }
  const properties = buildIssueProperties(issue, 'Todo');
  page = await notion.pages.create({
    parent: { database_id: ISSUE_DB_ID },
    properties,
  });
  console.log(
    `Created Notion page for referenced issue #${issueNumber}`
  );
  return page;
}

// Parse a PR body for closing-keyword references to issues in the same repo
// Matches: close/closes/closed/fix/fixes/fixed/resolve/resolves/resolved #NNN
function parseReferencedIssues(body) {
  if (!body) return [];
  const nums = new Set();
  const regex =
    /(?:close[sd]?|fix(?:es|ed)?|resolve[sd]?)\s+#(\d+)/gi;
  let match;
  while ((match = regex.exec(body)) !== null) {
    nums.add(parseInt(match[1], 10));
  }
  return [...nums];
}

// Upsert a PR into the Notion PR DB
async function syncPR(pr, action) {
  const existing = await queryNotionByUrl(
    PR_DB_ID,
    'Github Pull Request',
    pr.html_url
  );

  const isMerged = pr.merged === true || pr.merged_at != null;
  const ghState =
    action === 'closed' && isMerged ? 'merged' : pr.state;

  let statusValue;
  if (!existing) {
    statusValue = 'In Progress';
  } else if (action === 'closed' && isMerged) {
    statusValue = 'Done';
  }
  // statusValue stays undefined for updates that should not touch Status

  const properties = {
    'Pull Request': { title: [{ text: { content: pr.title } }] },
    Repository: { rich_text: [{ text: { content: REPO_NAME } }] },
    Num: { number: pr.number },
    'Github Pull Request': { url: pr.html_url },
    'GitHub State': { select: { name: ghState } },
    Labels: {
      multi_select: (pr.labels || []).map((l) => ({ name: l.name })),
    },
    Assignees: {
      multi_select: (pr.assignees || []).map((a) => ({ name: a.login })),
    },
    Author: { rich_text: [{ text: { content: pr.user.login } }] },
    'Created At': { date: { start: pr.created_at } },
    'Updated At': { date: { start: pr.updated_at } },
  };

  if (statusValue !== undefined) {
    properties.Status = { select: { name: statusValue } };
  }

  // Resolve related issue pages from closing keywords in the PR body
  const refNums = parseReferencedIssues(pr.body);
  const relatedPages = [];
  for (const num of refNums) {
    const page = await ensureIssueInNotion(num);
    if (page) relatedPages.push({ id: page.id });
  }
  if (relatedPages.length > 0) {
    properties['Related Issue'] = { relation: relatedPages };
  }

  if (!existing) {
    const page = await notion.pages.create({
      parent: { database_id: PR_DB_ID },
      properties,
    });
    console.log(`Created Notion page for PR #${pr.number}`);
    return page;
  } else {
    const page = await notion.pages.update({
      page_id: existing.id,
      properties,
    });
    console.log(`Updated Notion page for PR #${pr.number}`);
    return page;
  }
}

async function main() {
  if (!process.env.NOTION_TOKEN) {
    throw new Error('NOTION_TOKEN is not set');
  }
  if (!ISSUE_DB_ID) {
    throw new Error('NOTION_ISSUE_DB_ID is not set');
  }
  if (!PR_DB_ID) {
    throw new Error('NOTION_PR_DB_ID is not set');
  }
  if (!EVENT_PATH) {
    throw new Error('GITHUB_EVENT_PATH is not set');
  }

  const event = JSON.parse(fs.readFileSync(EVENT_PATH, 'utf8'));
  const action = event.action;

  if (EVENT_NAME === 'issues') {
    await upsertIssue(event.issue, action);
    console.log(`Issue #${event.issue.number} synced to Notion (action: ${action})`);
  } else if (EVENT_NAME === 'pull_request') {
    await syncPR(event.pull_request, action);
    console.log(`PR #${event.pull_request.number} synced to Notion (action: ${action})`);
  } else {
    console.log(`Unsupported event: ${EVENT_NAME}`);
  }
}

main().catch((err) => {
  console.error('Notion sync failed:', err);
  process.exit(1);
});
