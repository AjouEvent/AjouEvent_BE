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
| Repository      | Select       | Option: `AjouEvent/AjouEvent_BE`           |
| Num             | Rich Text    | Issue number (as text)                     |
| Github Issue    | URL          | **Unique key** – used for upsert matching  |
| GitHub State    | Select       | Options: `open`, `closed`                  |
| Author          | Rich Text    | GitHub username of the issue author        |
| Created At      | Date         |                                            |

### PR DB (title property: `Pull Request`)

| Property name        | Type         | Notes                                               |
|----------------------|--------------|-----------------------------------------------------|
| Pull Request         | Title        | PR title                                            |
| Repository           | Select       | Option: `AjouEvent/AjouEvent_BE`                    |
| Num                  | Rich Text    | PR number (as text)                                 |
| Github Pull Request  | URL          | **Unique key** – used for upsert matching           |
| GitHub State         | Select       | Options: `open`, `closed`, `merged`                 |
| Author               | Rich Text    | GitHub username of the PR author                    |
| Created At           | Date         |                                                     |
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

All issue events update the Notion page with the latest issue title, state, author, and creation date.

### Pull Requests

Triggered by: `opened`, `edited`, `labeled`, `unlabeled`, `assigned`, `unassigned`, `reopened`, `closed`, `synchronize`.

All PR events update the Notion page with the latest PR title, state, author, and creation date.

### Related Issue Relation

The script resolves related issues from two sources and de-duplicates:

1. **PR title trailing reference** – a `#NNN` at the end of the PR title, optionally followed by punctuation (e.g., `"Fix login bug #42"` or `"Fix login bug #42."`).
2. **PR body closing keywords** – `close`, `closes`, `closed`, `fix`, `fixes`, `fixed`, `resolve`, `resolves`, `resolved` followed by `#NNN`.

For each referenced issue the script:

1. Looks up the issue in the Issue DB by its GitHub URL.
2. If the issue is not yet in Notion, fetches it from GitHub and creates the page.
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
