# AI Assistant

## `ai_flow` folder

This folder documents the planning process behind this project, produced together with Claude Code before implementation started:

- **`detailed_plan.md`** — the implementation plan: tech stack, architecture overview, step-by-step build order, and the decisions/open risks tracked along the way.
- **`questions.md`** — every clarification question Claude Code asked during planning, the answers given, and exactly what was changed in the plan as a result.
- **`data/cdq_fraud_guard.md`** — plain-text product content scraped from https://www.cdq.com/products/cdq-fraud-guard, used as the source document for the RAG/vector-store knowledge base.
- **`backlog.md`** — ideas worth remembering but deliberately deferred (not tied to a current task), so they aren't lost or acted on prematurely.

## Setup — API Keys

Some components call external services that require a free API key. Copy `example.env` to `.env` and fill in the real values — `.env` is git-ignored and never committed.

### Countries MCP server (`restcountries.com`)

The custom countries MCP server (`countries-mcp-server`) calls `restcountries.com`'s v5 API, which requires a free-tier account and API key (their older v1–v4 free/keyless API has been shut down).

1. Go to https://restcountries.com/sign-up and create a free account (full name, email, password).
2. Once logged in, find your API key in your account/dashboard (the free tier allows up to 2 keys, 500 requests/month).
3. Copy it into `.env`:
   ```
   COUNTRIES_API_KEY=your_restcountries_api_key_here
   ```

## How this project was built with Claude Code

1. Initialized this repository locally and created it as a public GitHub repo (`ai-assistant-backend`) via the GitHub CLI.
2. Converted the recruiting task PDF (`task.pdf`) into a plain markdown transcription, `plan.md`.
3. Asked Claude Code to turn `plan.md` into a more detailed implementation plan; reviewed it before saving.
4. Had Claude Code scrape the CDQ Fraud Guard product page and save the plain text as the RAG source document (`ai_flow/data/cdq_fraud_guard.md`).
5. Saved the reviewed implementation plan as `ai_flow/detailed_plan.md`.
6. Went through a clarification round with Claude Code on ambiguous points in the plan (embeddings model, chat interface shape, repository choice, how to answer the Berlin question, Docker availability, custom demo questions) — answers and resulting plan changes are recorded in `ai_flow/questions.md`.
7. Updated this README to document the `ai_flow` folder and the planning steps taken so far.
