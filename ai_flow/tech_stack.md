# Tech Stack

Concrete technology decisions for the AI Assistant, derived from `ai_flow/detailed_plan.md`. Each entry states the decision and why; items still needing your input are called out explicitly at the bottom instead of being guessed.

## Language & Build

| Concern | Decision | Rationale |
|---|---|---|
| Language | Java 21 | LTS; required by task ("Java Frameworks") |
| Build tool | Gradle (Kotlin DSL) | No strong reason to prefer Maven; using your preferred build tool |
| Framework | Spring Boot 3.3+ | Current stable line compatible with Spring AI 1.0 GA |
| AI framework | Spring AI 1.0.x (GA) | First GA line with stable MCP client + MCP server starters (`spring-ai-starter-mcp-client`, `spring-ai-starter-mcp-server`) and `PgVectorStore` support |

## LLM Runtime

| Concern | Decision | Rationale |
|---|---|---|
| Chat model | Ollama, `qwen3:4b` | Per task requirement, run locally via `spring-ai-starter-model-ollama` |
| Embeddings | `qwen3:4b` via Ollama, tried first | Per your earlier answer; fallback to `nomic-embed-text` (pulled via Ollama) only if `qwen3:4b` can't produce usable embeddings — no code branching needed for this, just a config swap of the embedding model name if the fallback is triggered |
| Ollama deployment | `ollama/ollama` Docker image, as a service in `docker-compose.yml` | Keeps the whole stack under one `docker compose up`; models pulled via an init step (`ollama pull qwen3:4b`) |

## RAG / Vector Store

| Concern | Decision | Rationale |
|---|---|---|
| Vector DB | PostgreSQL via `pgvector/pgvector:pg17` Docker image | Per task requirement |
| Vector store integration | Spring AI `PgVectorStore` (`spring-ai-starter-vector-store-pgvector`) | Auto-handles schema creation (`initialize-schema: true`) and similarity search |
| Source document | `ai_flow/data/cdq_fraud_guard.md` (already scraped) | Plain text used for chunking + embedding |
| Chunking | Spring AI `TokenTextSplitter` | Standard default splitter shipped with Spring AI, no need for a custom implementation |
| Ingestion trigger | `ApplicationRunner` bean that ingests the document on startup if the vector store is empty | Keeps ingestion simple and idempotent without a separate CLI tool |

## Custom MCP Server (restcountries.com)

| Concern | Decision | Rationale |
|---|---|---|
| Implementation | Separate Spring Boot module, built with `spring-ai-starter-mcp-server-webmvc` | Own module keeps it independently runnable/testable and mirrors "write own MCP server" as a distinct deliverable |
| Transport | SSE over HTTP (WebMVC) | Unlike the Node-based weather server, we control this implementation, so HTTP/SSE lets it run as its own container in `docker-compose.yml` and be reached over the network — cleaner in a multi-container Docker setup than a stdio subprocess |
| HTTP client (outbound to restcountries.com) | Spring 6 `RestClient` | Modern, synchronous, no need for reactive `WebClient` since call volume is trivial |
| Exposed tool(s) | `getCountryInfo(countryName)` → capital, region, population, languages, currencies | Covers both the "capital of Germany" question and the "what do you know about Berlin" question (via Germany's country data) |
| API auth | None required | restcountries.com is free/keyless |

## External MCP Server (mcp-weather)

| Concern | Decision | Rationale |
|---|---|---|
| Runtime | Node.js (v16+) / TypeScript, per its own repo | Not a Java component — used as-is, git-cloned into the project (e.g. `mcp-weather/` submodule or plain subdirectory) |
| Transport | stdio | This server has no documented HTTP/SSE mode — it's built for Claude Desktop's stdio-based MCP config, so the Spring AI MCP client connects to it as a stdio subprocess (`spring-ai-starter-mcp-client` stdio transport, spawning `npm start`) |
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
| End-to-end | A small suite of `@SpringBootTest` tests hitting `/chat` with the required questions, run against the real local stack manually (not in CI) since it needs Ollama + real MCP servers running | The required Q&A answers are captured by running the live stack, documented separately (e.g. `answers.md`), rather than asserted as strict automated test expectations against a live LLM's output |

## Containerization

| Concern | Decision | Rationale |
|---|---|---|
| Orchestration | Single `docker-compose.yml` at repo root | One command (`docker compose up`) starts everything |
| Services | `postgres` (pgvector), `ollama`, `countries-mcp-server`, `app` (main Spring Boot app, bundles Node.js for the weather MCP stdio subprocess) | Matches the architecture in `detailed_plan.md`; weather MCP is a subprocess of `app`, not its own container, due to stdio transport |
| Configuration | `application.yml` (not `.properties`), environment-specific values (DB host, Ollama host, `WEATHER_API_KEY`) injected via Compose environment variables | Standard Spring Boot convention, keeps secrets out of the JAR |

## Decisions Made (follow-up)

- **`WEATHER_API_KEY`**: stored in a git-ignored `.env` file. The repo includes an `example.env` with a placeholder (`WEATHER_API_KEY=your_api_key_here`) committed instead. `README.md` documents the step-by-step to sign up at weatherapi.com for a free key and where to put it (copy `example.env` to `.env`).
- **Weather MCP deployment**: confirmed — Node.js is bundled into the main app's Docker image (multi-stage build), and the Spring Boot app spawns `mcp-weather` as a local stdio subprocess. `docker compose up` remains the single entry point for the whole stack.
