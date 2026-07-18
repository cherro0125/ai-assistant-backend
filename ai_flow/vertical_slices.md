# Vertical Slices & Tasks

Based on `ai_flow/detailed_plan.md` and `ai_flow/tech_stack.md`. Each slice is a thin, end-to-end deliverable — chat endpoint → LLM → tool/data source → response — building up to the full required criteria from `plan.md`. Slices are ordered so every one after Slice 0 ends in a working, demoable answer to a required question; nothing is built horizontally (e.g. "all controllers first") before it's needed.

## Slice 0 — Walking Skeleton (no tools, no RAG)
**Goal:** Prove the base chat pipeline works end-to-end via Docker Compose before adding any knowledge source.

- [x] Scaffold Gradle (Kotlin DSL) Spring Boot 4.1.x project with `spring-ai-starter-model-ollama`, `spring-boot-starter-webmvc`, test starter.
- [x] Write `docker-compose.yml` with `ollama` service; add init step to `ollama pull qwen3:4b`.
- [x] Implement `POST /chat` controller: `{ "message": "..." }` → `{ "answer": "..." }`, wired straight to Spring AI `ChatClient` (no tools yet).
- [x] Write `chat.sh "question"` bash script wrapping `curl` against `/chat`.
- [x] Smoke test: send an arbitrary question, confirm a real qwen3:4b response comes back.

**Definition of done:** `docker compose up` + `./chat.sh "hello"` returns a real LLM answer.

---

## Slice 1 — Countries MCP Server + Capital/Berlin Questions
**Goal:** Answer *"What is the capital city of Germany?"* and *"What do you know about Berlin?"* end-to-end.

- [x] New Gradle module: countries MCP server, `spring-ai-starter-mcp-server-webmvc` (SSE transport).
- [ ] Implement `getCountryInfo(countryName)` tool calling `restcountries.com` via Spring `RestClient` (capital, region, population, languages, currencies).
- [ ] Add `countries-mcp-server` service to `docker-compose.yml`.
- [ ] Wire main app's `spring-ai-starter-mcp-client` to connect to it over SSE; register the tool with `ChatClient`.
- [ ] Write a system prompt describing the tool's purpose so the LLM knows to call it for country/capital questions.
- [ ] Unit tests for `getCountryInfo` (mock restcountries.com HTTP calls, e.g. WireMock).
- [ ] Manual verification: ask both required questions via `chat.sh`, capture responses.

**Definition of done:** Both questions answered correctly using live tool calls, not model guesswork.

---

## Slice 2 — Weather MCP Integration + Munich Question
**Goal:** Answer *"What is the temperature currently in Munich?"*

- [ ] Sign up for a free `weatherapi.com` API key; add `example.env` with `WEATHER_API_KEY=your_api_key_here` placeholder; add real key to git-ignored `.env`.
- [ ] Vendor/clone `mcp-weather` into the repo (e.g. `weather-mcp/` subdirectory).
- [ ] Extend main app's Dockerfile to a multi-stage build: Node stage builds `mcp-weather`, final image has JRE + Node runtime.
- [ ] Configure Spring AI MCP client to spawn `mcp-weather` as a **stdio** subprocess on app startup, passing `WEATHER_API_KEY` through.
- [ ] Register the weather tool with `ChatClient`; extend system prompt to mention it.
- [ ] Manual verification: ask the Munich question via `chat.sh`, capture response.

**Definition of done:** Munich temperature question answered via a live call through the stdio-spawned MCP server.

---

## Slice 3 — Multi-Hop Tool Orchestration
**Goal:** Answer *"What is the temperature of the capital of Germany currently?"* — requires the LLM to chain: countries tool (capital lookup) → weather tool (temperature lookup).

- [ ] No new components — this validates that Slices 1+2 compose correctly through the LLM's own tool-calling/reasoning.
- [ ] Tune system prompt if needed so the model reliably chains tools instead of guessing an answer directly.
- [ ] Manual verification with multiple runs (LLM tool-chaining can be inconsistent) — capture a working transcript.
- [ ] If chaining proves unreliable with `qwen3:4b`, document the limitation rather than forcing it (per task's "explain why" allowance).

**Definition of done:** Question answered correctly via genuine two-tool chaining, or the limitation is documented if not achievable.

---

## Slice 4 — RAG Knowledge Base (CDQ Fraud Guard)
**Goal:** Ground answers about CDQ Fraud Guard in the scraped product content.

- [ ] Add `postgres` (pgvector/pgvector:pg17) service to `docker-compose.yml`.
- [ ] Add `spring-ai-starter-vector-store-pgvector`; configure `initialize-schema: true`.
- [ ] Implement startup `ApplicationRunner`: if vector store empty, chunk `ai_flow/data/cdq_fraud_guard.md` with `TokenTextSplitter`, embed (qwen3:4b, fallback `nomic-embed-text`), store in `PgVectorStore`.
- [ ] Wire a RAG `Advisor` into `ChatClient` for retrieval-augmented prompts.
- [ ] Unit test for chunking logic; integration test (Testcontainers) verifying embed+retrieve round-trip.
- [ ] Manual verification: ask a CDQ Fraud Guard product question, confirm the answer reflects the scraped content (not hallucinated).

**Definition of done:** A CDQ-specific question is answered correctly, grounded in the ingested document.

---

## Slice 5 — Custom "Show Off" Questions
**Goal:** Demonstrate combined tool use per task requirement.

- [ ] Finalize 2–3 custom questions (**still pending your input** — e.g. combining RAG + countries + weather, such as "What trust-score feature does CDQ Fraud Guard offer, and what's the weather like at CDQ's headquarters?" or similar).
- [ ] Manual verification and transcript capture for each.

**Definition of done:** Custom questions answered and recorded, showing multi-source reasoning.

---

## Slice 6 — Test Suite Consolidation
**Goal:** Satisfy "provide tests" requirement comprehensively, not just ad hoc per-slice tests.

- [ ] Ensure unit test coverage: countries MCP tool logic, RAG chunking/ingestion logic.
- [ ] Ensure integration tests: Testcontainers-based pgvector store test.
- [ ] Add WireMock-based integration tests for countries MCP server (stubbed restcountries.com) and, if feasible, a stubbed weatherapi.com test.
- [ ] `./gradlew test` runs the full automated suite green, independent of live Ollama/MCP servers.

**Definition of done:** `./gradlew test` passes standalone (no live external dependencies needed for automated tests).

---

## Slice 7 — Documentation & Final Answers
**Goal:** Satisfy all "Task requirements" from `plan.md`.

- [ ] `README.md`: prerequisites, Docker/Ollama/Gradle setup, `.env` setup incl. weatherapi.com sign-up steps, how to run (`docker compose up`), how to run tests, how to use `chat.sh`.
- [ ] `answers.md` (or README section): captured real transcripts for all 4 required questions + custom questions.
- [ ] "How AI was used" section documenting the Claude Code collaboration (planning docs in `ai_flow/`, code generation, etc.).
- [ ] "Known limitations / out of scope" section (long/short-term memory excluded per task; any multi-hop tool-chaining caveats from Slice 3; any embeddings fallback triggered in Slice 4).
- [ ] Final push to `ai-assistant-backend`.

**Definition of done:** All "Task requirements" from `plan.md` are verifiably satisfied by a reviewer following the README alone.
