# Notion Sync – Setup Guide

Automatically syncs GitHub Issues and Pull Requests into Notion databases using GitHub Actions.

---

## Prerequisites

1. A Notion integration with **read/write** access to the two databases below.  
   Create one at <https://www.notion.so/my-integrations> and copy the **Internal Integration Token**.
2. Two Notion databases shared with the integration (open each database → **⋯ menu → Add connections**).

---

## Required Notion Database Properties

### Issue DB (title property: `Issue`)

| Property name   | Type         | Notes                                      |
|-----------------|--------------|--------------------------------------------|
| Issue           | Title        | Issue title                                |
| Repository      | Rich Text    | e.g. `AjouEvent/AjouEvent_BE`              |
| Num             | Number       | Issue number                               |
| Github Issue    | URL          | **Unique key** – used for upsert matching  |
| Status          | Select       | Options: `Todo`, `In Progress`, `Done`     |
| GitHub State    | Select       | Options: `open`, `closed`                  |
| Labels          | Multi-select | Mirrors GitHub labels                      |
| Assignees       | Multi-select | GitHub usernames of assignees              |
| Author          | Rich Text    | GitHub username of the issue author        |
| Created At      | Date         |                                            |
| Updated At      | Date         |                                            |

### PR DB (title property: `Pull Request`)

| Property name        | Type         | Notes                                               |
|----------------------|--------------|-----------------------------------------------------|
| Pull Request         | Title        | PR title                                            |
| Repository           | Rich Text    | e.g. `AjouEvent/AjouEvent_BE`                       |
| Num                  | Number       | PR number                                           |
| Github Pull Request  | URL          | **Unique key** – used for upsert matching           |
| Status               | Select       | Options: `Todo`, `In Progress`, `Done`              |
| GitHub State         | Select       | Options: `open`, `closed`, `merged`                 |
| Labels               | Multi-select | Mirrors GitHub labels                               |
| Assignees            | Multi-select | GitHub usernames of assignees                       |
| Author               | Rich Text    | GitHub username of the PR author                    |
| Created At           | Date         |                                                     |
| Updated At           | Date         |                                                     |
| Related Issue        | Relation     | Relates to the **Issue DB**                         |

---

## GitHub Repository Secrets

Add the following secrets under **Settings → Secrets and variables → Actions**:

| Secret name          | Value                                      |
|----------------------|--------------------------------------------|
| `NOTION_TOKEN`       | Notion Internal Integration Token          |
| `NOTION_ISSUE_DB_ID` | ID of the Issue database (32-char hex)     |
| `NOTION_PR_DB_ID`    | ID of the PR database (32-char hex)        |

> **How to find a database ID:** Open the database in Notion, copy the URL.  
> The ID is the 32-character string between the last `/` and `?v=...`.  
> Example: `https://www.notion.so/**abc123…**?v=…`

The `GITHUB_TOKEN` secret is provided automatically by GitHub Actions – no manual setup needed.

---

## Sync Behaviour

### Issues

Triggered by: `opened`, `edited`, `labeled`, `unlabeled`, `assigned`, `unassigned`, `reopened`, `closed`.

| Situation               | Status field       | GitHub State field |
|-------------------------|--------------------|--------------------|
| New issue (not in DB)   | Set to **Todo**    | From GitHub        |
| Existing issue updated  | **Not changed**    | From GitHub        |
| Issue closed            | Set to **Done**    | `closed`           |

### Pull Requests

Triggered by: `opened`, `edited`, `labeled`, `unlabeled`, `assigned`, `unassigned`, `reopened`, `closed`, `synchronize`.

| Situation                     | Status field            | GitHub State field |
|-------------------------------|-------------------------|--------------------|
| New PR (not in DB)            | Set to **In Progress**  | From GitHub        |
| Existing PR updated           | **Not changed**         | From GitHub        |
| PR closed **without** merge   | **Not changed**         | `closed`           |
| PR closed **with** merge      | Set to **Done**         | `merged`           |

### Related Issue Relation

When a PR body contains closing keywords (`close`, `closes`, `closed`, `fix`, `fixes`, `fixed`, `resolve`, `resolves`, `resolved`) followed by `#NNN`, the sync script:

1. Looks up the referenced issue in the Issue DB by its GitHub URL.
2. If the issue is not yet in Notion, it fetches it from GitHub and creates the page.
3. Sets the **Related Issue** relation on the PR page to point to all resolved issue pages.

---

## Workflow File

The workflow is located at `.github/workflows/notion-sync.yml`.  
The sync script is at `scripts/notion-sync/index.js` and uses the `@notionhq/client` Node.js SDK.

---

## Local Development / Testing

```bash
cd scripts/notion-sync
npm install

# Set environment variables
export NOTION_TOKEN=secret_...
export NOTION_ISSUE_DB_ID=<db-id>
export NOTION_PR_DB_ID=<db-id>
export GITHUB_TOKEN=ghp_...
export GITHUB_EVENT_NAME=issues          # or pull_request
export GITHUB_EVENT_PATH=/tmp/event.json # path to a sample event payload

node index.js
```

A sample `issues` event payload can be downloaded from any GitHub Actions run that triggered the workflow (Actions → run → event.json artifact).
