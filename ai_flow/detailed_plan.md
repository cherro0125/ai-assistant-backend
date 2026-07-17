# AI Assistant — Detailed Implementation Plan

## 1. Goal

Build a Java-based AI Assistant with a chat interface that can answer questions using three integrated knowledge sources: a local RAG vector store, a custom MCP server wrapping a REST API, and an existing MCP weather server — all orchestrated through a locally-run `qwen3:4b` model via Ollama.

## 2. Tech Stack

| Concern | Choice | Notes |
|---|---|---|
| Language / Runtime | Java 21 | LTS, required for modern framework support |
| Framework | Spring Boot 3.x + Spring AI | Spring AI provides Ollama chat client, RAG/vector store abstractions, and MCP client/server support out of the box |
| LLM runtime | Ollama running `qwen3:4b` | Local, no external API key needed |
| Vector DB | PostgreSQL + pgvector (`pgvector/pgvector:pg17` via Docker) | Stores CDQ Fraud Guard product text as embeddings |
| Embeddings model | `qwen3:4b` (tried first) | Attempt embeddings directly via qwen3:4b/Ollama; fall back to a dedicated embedding model (e.g. `nomic-embed-text`) only if qwen3:4b can't produce usable embeddings |
| Custom MCP server | Spring AI MCP Server module | Wraps `restcountries.com` REST API (capital lookup, and general country facts — used for both "capital of Germany" and "what do you know about Berlin" style questions) |
| External MCP server | `mcp-weather` (from mcpservers.org) | Run locally, provides current temperature by city |
| Chat interface | REST endpoint (`/chat`) + a bash script wrapper | No web UI; a simple shell script wraps `curl` calls to the endpoint for convenient manual testing |
| Tests | JUnit 5 + Spring Boot Test + Testcontainers (for pgvector) | Unit + integration tests |
| Containerization | Docker Compose | Spins up Postgres/pgvector, Ollama, and the app together |

## 3. Architecture Overview

```
User ──chat──> Spring Boot App (Chat Controller)
                     │
                     ▼
              Spring AI ChatClient (Ollama qwen3:4b)
                     │
       ┌─────────────┼───────────────────────┐
       ▼             ▼                       ▼
  RAG retrieval   MCP Client (custom)   MCP Client (weather)
  (pgvector)      → Countries MCP        → mcp-weather server
       │             server (wraps          (external tool)
       │             restcountries.com)
       ▼
  CDQ Fraud Guard
  embeddings
```

The LLM acts as an orchestrator: given a user question, it decides (via tool-calling / MCP) whether to query the vector store (RAG), the countries MCP server, and/or the weather MCP server, then synthesizes an answer.

## 4. Implementation Steps

1. **Project scaffolding**
   - Initialize Spring Boot project (Spring Initializr) with dependencies: `spring-ai-ollama`, `spring-ai-pgvector-store`, `spring-ai-mcp-client`, `spring-ai-mcp-server`, `spring-boot-starter-web`, `spring-boot-starter-test`.
   - Set up `docker-compose.yml`: Postgres+pgvector, Ollama (with `qwen3:4b` pulled).

2. **RAG knowledge source (CDQ Fraud Guard)** ✅ *source content ready*
   - Plain-text product content already scraped and saved at `ai_flow/data/cdq_fraud_guard.md`.
   - Build an ingestion step (startup job or CLI command) that chunks this text, generates embeddings, and stores them in the pgvector-backed `VectorStore`.
   - Wire a retrieval-augmented prompt: on relevant queries, fetch top-k similar chunks and inject into the LLM prompt context.

3. **Custom MCP server for restcountries.com**
   - Build a new Spring Boot MCP server module exposing at least one tool, e.g. `getCapital(countryName)`, backed by calls to `https://restcountries.com/v3.1/name/{name}`.
   - Register it as an MCP server the main app's MCP client can connect to (stdio or SSE transport per Spring AI MCP support).

4. **Integrate external MCP weather server**
   - Run `mcp-weather` locally (per mcpservers.org instructions).
   - Configure the app's MCP client to connect to it and expose its `getWeather`/`getTemperature` tool to the chat model.

5. **Chat orchestration**
   - Configure Spring AI `ChatClient` with:
     - System prompt describing available tools/capabilities.
     - Tool/function callbacks registered from both MCP servers.
     - RAG advisor for the vector store.
   - Expose a `/chat` REST endpoint that takes a user message and returns the model's response.
   - Provide a small bash script (e.g. `chat.sh "question"`) that wraps a `curl` call to `/chat` for convenient manual testing, instead of a web UI.

6. **Testing**
   - Unit tests for the countries MCP server tool logic (mock HTTP calls).
   - Unit tests for the RAG ingestion/chunking logic.
   - Integration tests using Testcontainers for pgvector to verify embedding storage/retrieval.
   - End-to-end test(s) hitting `/chat` with the required sample questions, asserting reasonable responses (may need to mock Ollama or run against real local model if available in CI).

7. **Answer the required questions**
   - Run the assistant locally and capture actual responses for:
     - What is the capital city of Germany?
     - What is the temperature currently in Munich?
     - What is the temperature of the capital of Germany currently? (tests multi-hop: capital lookup → weather lookup)
     - What do you know about Berlin? (answered via the custom countries MCP server — fetches Germany's country data from restcountries.com, e.g. capital, region, population, languages, currencies)
     - 2–3 custom questions demonstrating combined tool use (e.g., "Compare the temperature in the capital of Germany and the capital of France" or a CDQ Fraud Guard product question).
   - Record actual output in README or a dedicated `answers.md`.

8. **Documentation**
   - `README.md`: prerequisites (Docker, Ollama, Java version), how to start Postgres/pgvector and Ollama, how to pull `qwen3:4b`, how to run the app, how to run tests, how to ask questions via the chat interface.
   - Section explaining how AI tools were used during development (per task requirement).
   - Section noting any incomplete/out-of-scope items and why.

## 5. Decisions Made

- **Embeddings model**: try `qwen3:4b` directly for embeddings first; fall back to a dedicated Ollama embedding model (e.g. `nomic-embed-text`) only if that doesn't work.
- **Chat interface**: REST endpoint (`/chat`) only, plus a bash script wrapper around `curl` for manual testing — no web UI.
- **Repository**: reuse the existing `ai-assistant-backend` repo.
- **"What do you know about Berlin?"**: answered via the custom countries MCP server (restcountries.com lookup for Germany), not RAG or general LLM knowledge.
- **Docker**: confirmed available and preferred — Docker Compose will be used for Postgres/pgvector and Ollama, and Testcontainers for integration tests, as planned.

## 6. Open Questions / Risks

- **MCP transport**: need to confirm whether Spring AI's MCP client/server support matches the transport (stdio/SSE) used by `mcp-weather` and how we'll expose the custom countries server.
- **Custom "show off" questions**: still need to finalize 2-3 specific questions that demonstrate combined tool use (e.g. RAG + countries MCP + weather MCP together).
