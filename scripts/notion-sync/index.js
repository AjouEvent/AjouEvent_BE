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
function buildIssueProperties(issue) {
  return {
    Issue: { title: [{ text: { content: issue.title } }] },
    Repository: { select: { name: REPO_NAME } },
    Num: { rich_text: [{ text: { content: String(issue.number) } }] },
    'Github Issue': { url: issue.html_url },
    'GitHub State': { select: { name: issue.state } },
    Author: { rich_text: [{ text: { content: issue.user.login } }] },
    'Created At': { date: { start: issue.created_at } },
  };
}

// Upsert an issue into the Notion Issue DB
async function upsertIssue(issue) {
  const existing = await queryNotionByUrl(
    ISSUE_DB_ID,
    'Github Issue',
    issue.html_url
  );

  const properties = buildIssueProperties(issue);

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
  const properties = buildIssueProperties(issue);
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

// Parse a PR title for a trailing issue reference: e.g. "Fix bug #123" or "Fix bug #123."
function parseTitleIssueRef(title) {
  if (!title) return [];
  const match = title.match(/#(\d+)\s*[.,!?;:]?\s*$/);
  return match ? [parseInt(match[1], 10)] : [];
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

  const properties = {
    'Pull Request': { title: [{ text: { content: pr.title } }] },
    Repository: { select: { name: REPO_NAME } },
    Num: { rich_text: [{ text: { content: String(pr.number) } }] },
    'Github Pull Request': { url: pr.html_url },
    'GitHub State': { select: { name: ghState } },
    Author: { rich_text: [{ text: { content: pr.user.login } }] },
    'Created At': { date: { start: pr.created_at } },
  };

  // Resolve related issue pages from title trailing ref and body closing keywords
  const refNums = [
    ...new Set([
      ...parseTitleIssueRef(pr.title),
      ...parseReferencedIssues(pr.body),
    ]),
  ];
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
    await upsertIssue(event.issue);
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
