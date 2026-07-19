# Tech Stack

Concrete technology decisions for the AI Assistant, derived from `ai_flow/detailed_plan.md`. Each entry states the decision and why; items still needing your input are called out explicitly at the bottom instead of being guessed.

## Language & Build

| Concern | Decision | Rationale |
|---|---|---|
| Language | Java 21 | LTS; required by task ("Java Frameworks") |
| Build tool | Gradle (Kotlin DSL) | No strong reason to prefer Maven; using your preferred build tool |
| Framework | Spring Boot 4.1.x | Current default on start.spring.io as of 2026-07; Spring AI's `ollama`, `mcp-client`, `mcp-server`, `vectordb-pgvector` modules require Spring Boot in range `[4.0.0, 4.2.0.M1)`, so this fits |
| AI framework | Spring AI 2.0.x (GA) | Current GA line aligned with Spring Boot 4; supersedes the originally-assumed 1.0.x/Spring Boot 3.3 pairing (corrected after checking start.spring.io's live metadata before scaffolding) |

## LLM Runtime

| Concern | Decision | Rationale |
|---|---|---|
| Chat model | Ollama, `qwen3:4b` | Per task requirement, run locally via the `spring-ai-ollama` starter |
| Embeddings | `qwen3:4b` via Ollama, tried first | Per your earlier answer; fallback to `nomic-embed-text` (pulled via Ollama) only if `qwen3:4b` can't produce usable embeddings — no code branching needed for this, just a config swap of the embedding model name if the fallback is triggered |
| Ollama deployment | `ollama/ollama` Docker image, as a service in `docker-compose.yml` | Keeps the whole stack under one `docker compose up`; models pulled via an init step (`ollama pull qwen3:4b`) |

## RAG / Vector Store

| Concern | Decision | Rationale |
|---|---|---|
| Vector DB | PostgreSQL via `pgvector/pgvector:pg17` Docker image | Per task requirement |
| Vector store integration | Spring AI `PgVectorStore` (`spring-ai-vectordb-pgvector` starter) | Auto-handles schema creation (`initialize-schema: true`) and similarity search |
| Source document | `ai_flow/data/cdq_fraud_guard.md` (already scraped) | Plain text used for chunking + embedding |
| Chunking | Spring AI `TokenTextSplitter` | Standard default splitter shipped with Spring AI, no need for a custom implementation |
| Ingestion trigger | `ApplicationRunner` bean that ingests the document on startup if the vector store is empty | Keeps ingestion simple and idempotent without a separate CLI tool |

## Custom MCP Server (restcountries.com)

| Concern | Decision | Rationale |
|---|---|---|
| Implementation | Separate Spring Boot module, built with the `spring-ai-mcp-server` starter (WebMVC/SSE variant) | Own module keeps it independently runnable/testable and mirrors "write own MCP server" as a distinct deliverable |
| Transport | SSE over HTTP (WebMVC) | Unlike the Node-based weather server, we control this implementation, so HTTP/SSE lets it run as its own container in `docker-compose.yml` and be reached over the network — cleaner in a multi-container Docker setup than a stdio subprocess |
| HTTP client (outbound to restcountries.com) | Spring 6 `RestClient` | Modern, synchronous, no need for reactive `WebClient` since call volume is trivial |
| Exposed tool(s) | `getCountryInfo(countryName)` → capital, region, population, languages, currencies | Covers both the "capital of Germany" question and the "what do you know about Berlin" question (via Germany's country data) |
| API auth | Bearer token (`Authorization: Bearer <key>`), key in `COUNTRIES_API_KEY` env var / `.env` | **Revised**: restcountries.com's legacy free/keyless API (v1–v4) is fully shut down; the current v5 API requires a free-tier account and API key for every request. Endpoint is `https://api.restcountries.com/countries/v5/names.common/{name}`, JSON:API-shaped response |
| Tool registration mechanism | `@McpTool` / `@McpToolParam` (annotation-based scanning) on a plain `@Component` bean | Spring AI 2.0's MCP server module scans for `@McpTool`-annotated methods automatically (confirmed via startup log: "Registered tools: 1") — different from the older `@Tool`/`ToolCallbackProvider` pattern from Spring AI 1.0 |

## External MCP Server (mcp-weather)

| Concern | Decision | Rationale |
|---|---|---|
| Runtime | Node.js (v16+) / TypeScript, per its own repo | Not a Java component — used as-is, git-cloned into the project (e.g. `mcp-weather/` submodule or plain subdirectory) |
| Transport | stdio | This server has no documented HTTP/SSE mode — it's built for Claude Desktop's stdio-based MCP config, so the Spring AI MCP client connects to it as a stdio subprocess (`spring-ai-mcp-client` starter, stdio transport, spawning `npm start`) |
| Packaging | Node.js installed inside the main app's Docker image (multi-stage build: Node stage builds/copies `mcp-weather`, final image has both JRE and Node runtime) | Keeps `docker compose up` as the single entry point instead of requiring a manually-started host process; the stdio subprocess is spawned by the Spring Boot app itself at runtime |
| Backing weather API | weatherapi.com, via `WEATHER_API_KEY` env var, loaded from a git-ignored `.env` file (`example.env` committed with a placeholder) | Required by mcp-weather itself; README documents the free sign-up step |

## Chat Interface

| Concern | Decision | Rationale |
|---|---|---|
| Endpoint | `POST /chat` (Spring MVC REST controller) | Simple request/response; no streaming required per task scope |
| Convenience wrapper | `chat.sh "question"` bash script wrapping `curl -s -X POST localhost:8080/chat ...` | Per your earlier answer — no web UI |
| Request/response format | JSON `{ "message": "..." }` → `{ "answer": "..." }` | Minimal, sufficient for the task |

## Testing

| Concern | Decision | Rationale |
|---|---|---|
| Unit tests | JUnit 5 + Mockito | Standard Spring Boot Test defaults |
| Integration tests (pgvector) | Testcontainers (`postgres` module, pointed at `pgvector/pgvector:pg17` image) | Docker confirmed available; avoids needing a real always-on DB for tests |
| Integration tests (MCP servers) | WireMock (or a local stub) for restcountries.com and weatherapi.com in tests | Keeps tests deterministic and offline; real MCP servers are exercised manually for the required Q&A, not in the automated test suite |
| End-to-end smoke test | `@SpringBootTest` hitting `/chat`, asserting only a non-blank response, backed by a Testcontainers-managed `OllamaContainer` (`testcontainers-ollama` + `spring-ai-spring-boot-testcontainers` `@ServiceConnection`) | Self-contained and CI-capable — no manually-run `docker compose up` needed. The model is pulled once and committed to a local image tag (`ai-assistant-ollama-qwen3-4b`) so only the first run is slow (~5 min); subsequent runs reuse the baked image (~20s). Revised from the original plan of "run manually, not in CI" after review caught that a hard dependency on a manually-started stack contradicted Slice 6's stated goal of `./gradlew test` passing standalone. |
| End-to-end required Q&A | The 4 required questions (+ custom ones) run manually against the real local stack (`docker compose up` + `chat.sh`/`curl`), not asserted in the automated suite | Verifying the *content* of a live LLM's answer isn't a stable automated assertion; these are captured and documented separately (e.g. `answers.md`) rather than asserted as strict test expectations |

## Containerization

| Concern | Decision | Rationale |
|---|---|---|
| Orchestration | Single `docker-compose.yml` at repo root | One command (`docker compose up`) starts everything |
| Services | `postgres` (pgvector), `ollama`, `countries-mcp-server`, `app` (main Spring Boot app, bundles Node.js for the weather MCP stdio subprocess) | Matches the architecture in `detailed_plan.md`; weather MCP is a subprocess of `app`, not its own container, due to stdio transport |
| Configuration | `application.yml` (not `.properties`), environment-specific values (DB host, Ollama host, `WEATHER_API_KEY`) injected via Compose environment variables | Standard Spring Boot convention, keeps secrets out of the JAR |

## Decisions Made (follow-up)

- **`WEATHER_API_KEY`**: stored in a git-ignored `.env` file. The repo includes an `example.env` with a placeholder (`WEATHER_API_KEY=your_api_key_here`) committed instead. `README.md` documents the step-by-step to sign up at weatherapi.com for a free key and where to put it (copy `example.env` to `.env`).
- **Weather MCP deployment**: confirmed — Node.js is bundled into the main app's Docker image (multi-stage build), and the Spring Boot app spawns `mcp-weather` as a local stdio subprocess. `docker compose up` remains the single entry point for the whole stack.
- **Version correction (Spring Boot 3.3/Spring AI 1.0 → 4.1/2.0)**: before starting implementation (Slice 0, task 0.1), checked `start.spring.io`'s live metadata and found Spring Boot 4.1.x is now the default line, with Spring AI's `ollama`/`mcp-client`/`mcp-server`/`vectordb-pgvector` modules requiring Spring Boot `[4.0.0, 4.2.0.M1)`. Updated all version references above accordingly. Local environment confirmed compatible: Java 21.0.2, Gradle 9.3.0 installed.
