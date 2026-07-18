# Tasks

Detailed, actionable tasks grouped by vertical slice (see `ai_flow/vertical_slices.md` for slice goals and Definition of Done). Each task lists what to build and its acceptance criteria so it can be picked up independently.

## Slice 0 — Walking Skeleton

- [x] **0.1 — Scaffold the Gradle project** ✅ *Done (2026-07-17)*
  Create the Spring Boot 4.1.x project with Gradle (Kotlin DSL), dependencies: `spring-ai-starter-model-ollama`, `spring-boot-starter-webmvc`, test starter.
  *Done when:* `./gradlew build` succeeds with an empty app.
  *Notes:* Generated via the real Spring Initializr API (start.spring.io) to get current, correct coordinates — surfaced the Spring Boot 3.3→4.1 / Spring AI 1.0→2.0 version correction recorded in `tech_stack.md`. Project files live at the repo root (not a nested `app/` subdirectory).

- [x] **0.2 — Docker Compose: Ollama service** ✅ *Done (2026-07-18)*
  Write `docker-compose.yml` with an `ollama` service; add a one-shot init step/script that runs `ollama pull qwen3:4b` after the container is up.
  *Done when:* `docker compose up` starts Ollama and `qwen3:4b` is available inside it.
  *Notes:* `ollama` service has a healthcheck (`ollama list`); a separate `ollama-pull-model` one-shot service (`depends_on: condition: service_healthy`) pulls `qwen3:4b` and exits. Verified via `docker exec ai-assistant-ollama-1 ollama list` — model present, 2.5GB. Model persists in the `ollama_data` named volume across restarts.

- [x] **0.3 — `/chat` endpoint (no tools)** ✅ *Done (2026-07-18)*
  Implement `POST /chat` REST controller, request `{ "message": "..." }`, response `{ "answer": "..." }`, calling Spring AI `ChatClient` configured for Ollama `qwen3:4b` directly, no tools/RAG.
  *Done when:* a manual POST returns a real qwen3:4b-generated answer.
  *Notes:* `ChatController` + `ChatClientConfig` (builds `ChatClient` from the auto-configured `ChatClient.Builder`) + `ChatRequest`/`ChatResponse` records, under `com.cdq.aiassistant.chat`. Verified live: `curl -X POST localhost:8080/chat -d '{"message": "..."}'` against the running app + Ollama container returned a real qwen3:4b answer.

- [x] **0.4 — `chat.sh` convenience script** ✅ *Done (2026-07-18)*
  Write a bash script `chat.sh "question"` that wraps a `curl -X POST` call to `/chat` and prints the answer.
  *Done when:* `./chat.sh "hello"` prints a readable answer without manual curl flags.
  *Notes:* Initially used `python3` for JSON encode/decode, then reworked to pure `sed` (no `python3` dependency) per follow-up request. Handles quote/backslash escaping correctly (verified with an embedded-quote input); doesn't handle embedded literal newlines or `\uXXXX` escapes in the answer — documented as a limitation in the script. Verified: `./chat.sh "What is 3 plus 5?"` and a quote-containing input both round-tripped correctly.

- **0.5 — Smoke test**
  Add a basic `@SpringBootTest` (or manual test) sending an arbitrary question through `/chat` and asserting a non-empty response.
  *Done when:* test passes against the running skeleton.

## Slice 1 — Countries MCP Server + Capital/Berlin Questions

- **1.1 — Scaffold countries MCP server module**
  New Gradle module/subproject for the countries MCP server using `spring-ai-starter-mcp-server-webmvc` (SSE transport).
  *Done when:* module builds and starts as a standalone Spring Boot app.

- **1.2 — `getCountryInfo` tool implementation**
  Implement a tool method `getCountryInfo(countryName)` that calls `restcountries.com` (via Spring `RestClient`) and returns capital, region, population, languages, currencies.
  *Done when:* calling the tool directly (e.g. via a test) returns correct data for "Germany".

- **1.3 — Unit tests for the countries tool**
  Add unit tests mocking the `restcountries.com` HTTP call (WireMock), covering found-country and not-found cases.
  *Done when:* tests pass without a real network call.

- **1.4 — Add countries MCP server to Docker Compose**
  Add a `countries-mcp-server` service to `docker-compose.yml`, exposing its SSE endpoint on the internal Docker network.
  *Done when:* `docker compose up` starts it alongside the main app.

- **1.5 — Wire main app's MCP client to the countries server**
  Configure `spring-ai-starter-mcp-client` in the main app to connect over SSE to `countries-mcp-server`; register the tool with the main `ChatClient`.
  *Done when:* the main app logs a successful MCP handshake/tool discovery on startup.

- **1.6 — System prompt for country/capital questions**
  Write/extend the system prompt so the LLM knows when to invoke the countries tool.
  *Done when:* the model reliably calls the tool rather than guessing from its own knowledge.

- **1.7 — Manual verification: capital + Berlin questions**
  Run "What is the capital city of Germany?" and "What do you know about Berlin?" through `chat.sh`; capture the transcripts.
  *Done when:* both answers are correct and demonstrably backed by a live tool call (not hallucinated).

## Slice 2 — Weather MCP Integration + Munich Question

- **2.1 — Obtain and configure `WEATHER_API_KEY`**
  Sign up at weatherapi.com for a free key; create `example.env` with a placeholder; add the real key to a git-ignored `.env`.
  *Done when:* `.env` is populated and `example.env` is committed with a placeholder only.

- **2.2 — Vendor `mcp-weather` into the repo**
  Clone/copy the `mcp-weather` source into the repo (e.g. `weather-mcp/`), pinned to a known-working commit/version.
  *Done when:* `npm install && npm start` runs it standalone and it responds to a manual MCP stdio call.

- **2.3 — Multi-stage Dockerfile for the main app**
  Extend the main app's Dockerfile: a Node build stage for `mcp-weather`, final image containing both the JRE and Node runtime.
  *Done when:* the built image can spawn the `mcp-weather` process internally.

- **2.4 — Wire MCP client to spawn `mcp-weather` via stdio**
  Configure the main app's MCP client to launch `mcp-weather` as a stdio subprocess on startup, passing `WEATHER_API_KEY` through from the environment.
  *Done when:* app startup logs show a successful stdio MCP handshake with the weather tool discovered.

- **2.5 — System prompt update for weather questions**
  Extend the system prompt to describe the weather tool's purpose (current temperature by city).
  *Done when:* the model reliably calls the weather tool for temperature questions.

- **2.6 — Manual verification: Munich question**
  Run "What is the temperature currently in Munich?" through `chat.sh`; capture the transcript.
  *Done when:* the answer reflects a live weatherapi.com value for Munich.

## Slice 3 — Multi-Hop Tool Orchestration

- **3.1 — Manual verification: capital-of-Germany temperature question**
  Run "What is the temperature of the capital of Germany currently?" through `chat.sh` multiple times; check whether the model chains countries-tool → weather-tool correctly.
  *Done when:* a correct, tool-chained transcript is captured, or inconsistent behavior is documented.

- **3.2 — Prompt tuning for reliable chaining (if needed)**
  If chaining is unreliable, adjust the system prompt (e.g. explicit reasoning instructions) to improve consistency.
  *Done when:* repeated manual runs show materially improved chaining reliability, or the residual limitation is written up.

- **3.3 — Document outcome**
  Record the final behavior (reliable / partially reliable / not achievable) and rationale in `ai_flow/` notes, to feed into the README's "limitations" section later.
  *Done when:* the note exists and matches actual observed behavior.

## Slice 4 — RAG Knowledge Base (CDQ Fraud Guard)

- **4.1 — Add pgvector service to Docker Compose**
  Add a `postgres` service using `pgvector/pgvector:pg17` to `docker-compose.yml`.
  *Done when:* the container starts and the pgvector extension is available.

- **4.2 — Add `PgVectorStore` dependency and config**
  Add `spring-ai-starter-vector-store-pgvector`; configure connection + `initialize-schema: true`.
  *Done when:* app startup creates the vector schema with no errors.

- **4.3 — Ingestion runner for `cdq_fraud_guard.md`**
  Implement an `ApplicationRunner` that, if the vector store is empty, chunks `ai_flow/data/cdq_fraud_guard.md` with `TokenTextSplitter`, embeds it, and stores it.
  *Done when:* on first startup, the vector store contains the expected number of chunks; on subsequent startups, ingestion is skipped.

- **4.4 — Embeddings model wiring (qwen3:4b, with fallback)**
  Configure Spring AI to use `qwen3:4b` for embeddings; if it fails to produce usable vectors, switch config to `nomic-embed-text` (pulled via Ollama) instead.
  *Done when:* embeddings are successfully generated and stored end-to-end with whichever model works.

- **4.5 — RAG advisor wiring**
  Add a retrieval advisor to the main `ChatClient` so relevant chunks are injected into the prompt for matching queries.
  *Done when:* a CDQ-specific question's prompt (traceable via logs) includes retrieved chunk content.

- **4.6 — Tests for ingestion + retrieval**
  Unit test for the chunking logic; Testcontainers-based integration test verifying embed-then-retrieve round-trip against a real pgvector instance.
  *Done when:* both tests pass via `./gradlew test`.

- **4.7 — Manual verification: CDQ Fraud Guard question**
  Ask a CDQ Fraud Guard product question via `chat.sh`; confirm the answer reflects the scraped content.
  *Done when:* transcript captured and content-accurate.

## Slice 5 — Custom "Show Off" Questions

- **5.1 — Finalize custom question list**
  Agree on 2-3 custom questions demonstrating combined tool/RAG use *(pending your input — not yet finalized)*.
  *Done when:* the question list is confirmed.

- **5.2 — Manual verification of custom questions**
  Run each finalized custom question through `chat.sh`; capture transcripts.
  *Done when:* all custom questions produce correct, demonstrably multi-source answers.

## Slice 6 — Test Suite Consolidation

- **6.1 — Review/consolidate unit test coverage**
  Ensure countries-tool and RAG-chunking unit tests (from Slices 1 & 4) are complete and well-organized.
  *Done when:* coverage includes happy-path and at least one error/edge case per component.

- **6.2 — Consolidate integration tests**
  Ensure the Testcontainers pgvector test and WireMock-based countries MCP server test are both present and reliable.
  *Done when:* both run repeatably without flakiness.

- **6.3 — Full suite run**
  Run `./gradlew test` end-to-end with no live Ollama/MCP dependency required.
  *Done when:* the full suite is green in a clean environment (e.g. fresh clone + `./gradlew test`).

## Slice 7 — Documentation & Final Answers

- **7.1 — Write README setup/run instructions**
  Document prerequisites (Docker, Gradle, Java 21), `.env` setup incl. weatherapi.com sign-up steps, `docker compose up`, and test execution.
  *Done when:* a reviewer following the README alone can run the stack from a fresh clone.

- **7.2 — Write `answers.md`**
  Record final transcripts for all 4 required questions plus the finalized custom questions.
  *Done when:* all required + custom questions have real, captured answers.

- **7.3 — "How AI was used" section**
  Document the Claude Code collaboration: planning artifacts in `ai_flow/`, code generation assistance, etc.
  *Done when:* section accurately reflects the actual process used.

- **7.4 — "Known limitations" section**
  Document out-of-scope items (memory) and any unresolved issues from Slice 3 (tool-chaining reliability) or Slice 4 (embeddings fallback triggered or not).
  *Done when:* section reflects actual final system behavior, not assumptions.

- **7.5 — Final push**
  Push all commits to `ai-assistant-backend`.
  *Done when:* remote `main` reflects the final state and CI/tests (if any) pass.
