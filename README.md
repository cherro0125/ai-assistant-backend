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

## Answers to Required Questions

Real transcripts captured via `./chat.sh` (i.e. the actual `/chat` endpoint) against the live stack, backed by real tool calls (`getCountryInfo`/`get-weather` MCP tools) and, for the last one, the RAG-grounded CDQ Fraud Guard content — not hand-written or hallucinated. Weather answers reflect live conditions at the time they were captured (2026-07-20) and will differ on a later run.

**What is the capital city of Germany?**
> The capital city of Germany is Berlin.

**What do you know about Berlin?**
> Berlin is the capital city of Germany, located in Europe. The population of Germany is approximately 83.47 million, with German as the official language and the Euro as the currency.

**What is the temperature currently in Munich?**
> The current temperature in Munich is **16.2°C**.

**What is the temperature of the capital of Germany currently?**
> The capital of Germany is Berlin. The current temperature in Berlin is **17 degrees**.

**Custom question — What does CDQ Fraud Guard do?** (demonstrates the RAG knowledge base described in "Setup — API Keys" and `ai_flow/data/cdq_fraud_guard.md`; the full list of "show off" custom questions is still being finalized, see `ai_flow/tasks.md` task 5.1)
> **CDQ Fraud Guard** is a fraud detection tool designed to protect businesses from payment fraud and inaccurate bank account information. It works by:
>
> 1. **Verifying bank accounts** against a shared database of validated accounts to prevent fraudulent transactions.
> 2. **Assigning trust scores** to bank accounts based on transaction history (customizable per business risk tolerance).
> 3. **Sending real-time fraud alerts** for potential threats using community-shared fraud case data.
> 4. **Managing fraud cases** to identify and mitigate risks proactively.
> 5. **Integrating seamlessly** with existing financial systems via an API to streamline compliance and security.
>
> As highlighted in the customer testimonial, it significantly reduces onboarding time (from up to one month to near-instant) by eliminating redundant documentation through automated trust scoring and verification.

## How this project was built with Claude Code

1. Initialized this repository locally and created it as a public GitHub repo (`ai-assistant-backend`) via the GitHub CLI.
2. Converted the recruiting task PDF (`task.pdf`) into a plain markdown transcription, `plan.md`.
3. Asked Claude Code to turn `plan.md` into a more detailed implementation plan; reviewed it before saving.
4. Had Claude Code scrape the CDQ Fraud Guard product page and save the plain text as the RAG source document (`ai_flow/data/cdq_fraud_guard.md`).
5. Saved the reviewed implementation plan as `ai_flow/detailed_plan.md`.
6. Went through a clarification round with Claude Code on ambiguous points in the plan (embeddings model, chat interface shape, repository choice, how to answer the Berlin question, Docker availability, custom demo questions) — answers and resulting plan changes are recorded in `ai_flow/questions.md`.
7. Updated this README to document the `ai_flow` folder and the planning steps taken so far.
