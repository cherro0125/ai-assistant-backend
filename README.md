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

### Weather MCP server (`weatherapi.com`)

The vendored `weather-mcp` server (`weather-mcp/`, from https://github.com/semdin/mcp-weather) calls `weatherapi.com`'s current-conditions API, which requires a free account and API key.

1. Go to https://www.weatherapi.com/ and sign up for a free account.
2. Once logged in, your API key is shown on your account dashboard.
3. Copy it into `.env`:
   ```
   WEATHER_API_KEY=your_weatherapi_api_key_here
   ```

## Running the Application

### Quickest way: Docker Compose

**Prerequisites:** Docker (with Compose).

1. Configure API keys:
   ```sh
   cp example.env .env
   ```
   Then fill in `COUNTRIES_API_KEY` and `WEATHER_API_KEY` in `.env` — see "Setup — API Keys" above for how to obtain them.

2. Bring up the whole stack (Ollama, the countries MCP server, and the main app — the main app's image bundles Node.js and spawns the weather MCP server internally as a subprocess, so nothing extra is needed for that):
   ```sh
   docker compose up --build
   ```
   Docker Compose reads `.env` automatically. The first run also pulls the `qwen3:4b` model (~2.5GB) and builds two images, so it can take a few minutes; watch `docker compose ps` for all services to report `healthy`/`Up`.

3. Ask a question:
   ```sh
   ./chat.sh "What is the capital city of Germany?"
   ```

### Running the main app locally instead

Useful if you're iterating on the Java code and don't want to rebuild the Docker image on every change — Ollama and the countries MCP server still run in Docker, but the main app runs directly via Gradle.

**Prerequisites:** Java 21 (a Gradle wrapper is included, no separate Gradle install needed), Node.js v16+ and npm (needed to run the vendored `weather-mcp/` server, which the main app spawns as a local subprocess).

1. Configure API keys as above (`cp example.env .env` and fill in the real values).

2. Install `weather-mcp`'s dependencies (one-time — the main app spawns `weather-mcp/node_modules/.bin/tsx` directly, so this needs to have been run at least once):
   ```sh
   cd weather-mcp && npm install && cd ..
   ```

3. Start Ollama and the countries MCP server only:
   ```sh
   docker compose up -d ollama countries-mcp-server
   ```
   Wait for both to report `healthy` (`docker compose ps`).

4. Run the main app:
   ```sh
   set -a && source .env && set +a
   ./gradlew bootRun
   ```
   The `.env` values need to be in the shell environment here (unlike Docker Compose, the JVM doesn't read `.env` files on its own) — `WEATHER_API_KEY` in particular is passed through to the `weather-mcp` subprocess.

5. Ask a question:
   ```sh
   ./chat.sh "What is the capital city of Germany?"
   ```

## Running the Tests

```sh
./gradlew test
```

Runs the full suite standalone — no manually-started `docker compose` services needed. Docker is still required, though: the main app's integration tests use Testcontainers to spin up their own throwaway Ollama and `pgvector/pgvector:pg17` Postgres containers, and the countries MCP server's tests use WireMock (no Docker needed there).

- First run bakes a local Ollama image with `qwen3:4b` and `nomic-embed-text` pre-pulled (a few minutes); later runs reuse it (seconds).
- `COUNTRIES_API_KEY` isn't required for tests — the one test that calls the real restcountries.com API self-skips without it, so the suite still passes green either way.

## Answers

See [`answers.md`](answers.md) for real, captured transcripts of the 4 required questions plus the 3 finalized custom "show off" questions — each backed by real MCP tool calls and/or RAG retrieval, not hand-written or hallucinated.

## How this project was built with Claude Code

1. Initialized this repository locally and created it as a public GitHub repo (`ai-assistant-backend`) via the GitHub CLI.
2. Converted the recruiting task PDF (`task.pdf`) into a plain markdown transcription, `plan.md`.
3. Asked Claude Code to turn `plan.md` into a more detailed implementation plan; reviewed it before saving.
4. Had Claude Code scrape the CDQ Fraud Guard product page and save the plain text as the RAG source document (`ai_flow/data/cdq_fraud_guard.md`).
5. Saved the reviewed implementation plan as `ai_flow/detailed_plan.md`.
6. Went through a clarification round with Claude Code on ambiguous points in the plan (embeddings model, chat interface shape, repository choice, how to answer the Berlin question, Docker availability, custom demo questions) — answers and resulting plan changes are recorded in `ai_flow/questions.md`.
7. Updated this README to document the `ai_flow` folder and the planning steps taken so far.
8. Broke the plan down into `ai_flow/vertical_slices.md` (8 slices, each ending in a working, demoable answer to a required question) and `ai_flow/tasks.md` (36 granular tasks with their own Definition of Done), and worked through them with Claude Code one at a time from there — implementation, not just planning.

### How the implementation phase worked

Every one of the 36 tasks across all 8 slices (Docker/Ollama setup → the countries MCP server → the weather MCP server → multi-hop tool chaining → the RAG knowledge base → custom demo questions → test suite consolidation → this documentation) was implemented by Claude Code, one task at a time, following this loop:

1. **Read the task's Definition of Done** from `ai_flow/tasks.md` — never batched ahead to a "trivial" follow-up task without asking first.
2. **Verified real API/library shapes before writing code against them**, rather than trusting a remembered API surface — this caught several real, would-have-been-wrong assumptions along the way: restcountries.com's free API had been shut down entirely and replaced with a paid-looking v5 API; `TestRestTemplate` had moved packages in this Spring Boot version; the bare `wiremock` artifact failed at runtime and needed `wiremock-standalone`; Spring AI's actual RAG advisor artifact (`spring-ai-vector-store-advisor`) wasn't the one that seemed obvious from its Initializr listing name; Testcontainers 2.x renamed several artifacts and packages (`testcontainers-postgresql`, `org.testcontainers.postgresql.PostgreSQLContainer`); `qwen3:4b` turned out to have no embedding capability at all (confirmed via `ollama show`, not assumed), requiring a fallback to `nomic-embed-text` exactly as the task anticipated. Each of these is recorded with what was actually checked (curling a live endpoint, decompiling a jar, inspecting a real Docker image) in that task's *Notes* in `ai_flow/tasks.md`.
3. **Implemented the change**, then **verified it live** wherever practical — a real `curl`, a real `docker compose up`, a real `chat.sh` question with debug logging enabled to prove a tool was actually called or a RAG chunk actually retrieved, not just that the answer looked plausible.
4. **Ran a self-review pass** after implementing, checking the diff for correctness, security, and unnecessary scope creep, fixing anything found before moving on.
5. **Recorded what was actually built** — including gotchas, deviations from the original plan, and dead ends — back into that task's *Notes* in `ai_flow/tasks.md`, so the reasoning behind each decision stays discoverable later instead of only living in a chat transcript.
6. **Committed with a `[S<slice>][T<task>]` message** describing the change, one task per commit.

Some decisions were explicitly left to you (the human) rather than decided unilaterally by Claude Code — e.g. the final wording of the 3 custom "show off" questions (several rounds of back-and-forth before landing on the RAG+countries+weather combination used), whether to add the main app to `docker-compose.yml` immediately or defer it to a later slice, and how to resolve a task-ordering mistake Claude Code made and flagged itself (embeddings model wiring needed to happen before the RAG ingestion runner that depends on it, not after, as originally numbered).
