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

- [x] **0.5 — Smoke test** ✅ *Done (2026-07-18)*
  Add a basic `@SpringBootTest` (or manual test) sending an arbitrary question through `/chat` and asserting a non-empty response.
  *Done when:* test passes against the running skeleton.
  *Notes:* `ChatControllerSmokeTest` (`@SpringBootTest(webEnvironment = RANDOM_PORT)`), posts to `/chat` via `TestRestTemplate`, asserts a non-blank answer. Required `@AutoConfigureTestRestTemplate` — in Spring Boot 4, `TestRestTemplate` moved to `org.springframework.boot.resttestclient` and is no longer auto-provided by `@SpringBootTest` alone, it's opt-in via that annotation.

  First implementation depended on a manually-run `docker compose up` Ollama instance — flagged in review as inconsistent with Slice 6's goal of `./gradlew test` passing standalone. Reworked to use a Testcontainers-managed `OllamaContainer` instead (`TestcontainersConfiguration`, `@ServiceConnection`), self-contained and CI-capable. The container is created from a base `ollama/ollama` image only once: the model is pulled and the container committed to a local image tag (`ai-assistant-ollama-qwen3-4b`), reused on later runs to avoid re-pulling ~2.5GB every time. Verified with `docker compose down` (proving no manual stack dependency): first run baked the image in ~4m50s, second run reused it and finished in ~22s. Full `./gradlew build` passes standalone in ~24s.

## Slice 1 — Countries MCP Server + Capital/Berlin Questions

- [x] **1.1 — Scaffold countries MCP server module** ✅ *Done (2026-07-18)*
  New Gradle module/subproject for the countries MCP server using `spring-ai-starter-mcp-server-webmvc` (SSE transport).
  *Done when:* module builds and starts as a standalone Spring Boot app.
  *Notes:* Added as a Gradle subproject (`countries-mcp-server/`, included via root `settings.gradle.kts`) rather than restructuring the existing root app into a nested module — Gradle supports a root project with its own sources plus included subprojects, so nothing about the existing app/docker-compose/chat.sh setup needed to change. Verified real artifact coordinates via a throwaway Spring Initializr probe (`spring-ai-starter-mcp-server-webmvc`, matching what was already in `tech_stack.md`). `CountriesMcpServerApplication` starts standalone on port 8081; logs confirm `McpServerAutoConfiguration` enabling tools/resources/prompts/completions capabilities (with an expected "no tool methods found" warning — the actual tool is task 1.2). Root `./gradlew build` now builds both modules together.

- [x] **1.2 — `getCountryInfo` tool implementation** ✅ *Done (2026-07-19)*
  Implement a tool method `getCountryInfo(countryName)` that calls `restcountries.com` (via Spring `RestClient`) and returns capital, region, population, languages, currencies.
  *Done when:* calling the tool directly (e.g. via a test) returns correct data for "Germany".
  *Notes:* **Major blocker found and resolved**: restcountries.com's legacy API (v1–v4, including the `v3.1` endpoint the task links to) is fully shut down; the new v5 API requires a bearer-token API key for every request (`https://api.restcountries.com/countries/v5/names.common/{name}`, `Authorization: Bearer <key>`, JSON:API-shaped response) — contradicts the task's "free REST service" premise. Resolved: you signed up for a free-tier key, stored as `COUNTRIES_API_KEY` in `.env` (placeholder in `example.env`), same pattern as `WEATHER_API_KEY`.

  Implementation: `CountriesApiClient` (Spring `RestClient`, base URL + bearer auth from `countries.api.*` properties) parses the real v5 response shape into a clean `CountryInfo` record (name, capital, region, population, languages, currencies). `CountryInfoTool` exposes it via `@McpTool`/`@McpToolParam` (Spring AI 2.0's MCP server uses annotation-based tool scanning, not the older `@Tool`/`ToolCallbackProvider` pattern) — confirmed via startup log going from "no tool methods found" to "Registered tools: 1". Also needed `spring-boot-starter-restclient` added explicitly (the main app gets it transitively via `spring-ai-starter-model-ollama`; this module doesn't have that dependency).

  Verification: `CountryInfoToolLiveTest` calls the tool directly against the real API, asserting Germany → capital=Berlin, region=Europe, population>0, languages contains German, currencies contains Euro — passed (confirmed via test XML: `skipped="0"`). Guarded with `@EnabledIfEnvironmentVariable(named = "COUNTRIES_API_KEY", ...)` so `./gradlew build` stays green without the key (caught this before it became a repeat of the Slice 0 task 0.5 issue). A WireMock-based offline unit test is still task 1.3.

- [x] **1.3 — Unit tests for the countries tool** ✅ *Done (2026-07-19)*
  Add unit tests mocking the `restcountries.com` HTTP call (WireMock), covering found-country and not-found cases.
  *Done when:* tests pass without a real network call.
  *Notes:* `CountriesApiClientTest` (WireMock, `org.wiremock:wiremock-standalone` — the bare `wiremock` artifact failed at runtime with "Jetty 11 is not present" due to a classpath conflict with a newer Jetty resolved elsewhere in the Boot 4.1 stack; the standalone artifact shades its own Jetty to avoid this) covers found (Germany, full field mapping) and not-found (empty `objects` → `Optional.empty()`) cases, directly exercising `CountriesApiClient`'s HTTP call and JSON parsing. Added `CountryInfoToolTest` (Mockito) too, covering `CountryInfoTool`'s delegation/exception-throwing logic at the tool layer. Verified: full `./gradlew clean build` passes standalone with `COUNTRIES_API_KEY` unset (22s) — the new tests need no network call, while `CountryInfoToolLiveTest` from task 1.2 correctly self-skips (`skipped="1"`) without the key and still passes when it's set.

- [x] **1.4 — Add countries MCP server to Docker Compose** ✅ *Done (2026-07-19)*
  Add a `countries-mcp-server` service to `docker-compose.yml`, exposing its SSE endpoint on the internal Docker network.
  *Done when:* `docker compose up` starts it alongside the main app.
  *Notes:* Added `countries-mcp-server/Dockerfile` (multi-stage: `eclipse-temurin:21-jdk` build stage running `./gradlew :countries-mcp-server:bootJar`, `eclipse-temurin:21-jre` runtime stage). Since this is a Gradle multi-project build, the Docker build context is the **repo root** (not the module subdirectory) so Gradle can see `settings.gradle.kts` — confirmed the root project's `src/` isn't needed for this, only its `build.gradle.kts` (for configuration-phase evaluation). Pinned `bootJar`'s output to a fixed `app.jar` name (`archiveFileName.set("app.jar")`) to avoid version-string matching in the Dockerfile. Added a repo-root `.dockerignore` (`.git`, `build/`, `.gradle/`, `.env`, etc.) after noticing the build context was needlessly sending ~80MB of untracked build output.

  `docker-compose.yml` gained a `countries-mcp-server` service (port 8081, `COUNTRIES_API_KEY` passed through from `.env`). Verified: `docker compose up -d countries-mcp-server` starts alongside the already-running `ollama` container; container logs show `Registered tools: 1` (the key correctly reaches the containerized app); port 8081 reachable from host; the main app (running locally via `bootRun` on port 8080) kept working the whole time, satisfying "alongside the main app." The main app itself isn't containerized yet — that's task 2.3 per the original plan (Node.js bundling for the weather MCP stdio subprocess forces that dockerization then).

  *Post-review fixes*: (1) `${COUNTRIES_API_KEY}` → `${COUNTRIES_API_KEY:?COUNTRIES_API_KEY must be set in .env}` — verified a missing key now makes `docker compose up` refuse to start with a clear error, instead of silently running with an empty key (confirmed the pre-fix behavior first: only a stderr warning, container would start anyway). (2) Added `spring-boot-starter-actuator` + a `curl -f http://localhost:8081/actuator/health` healthcheck, matching the pattern `ollama` already uses — verified `docker compose ps` shows `Up ... (healthy)`. (3) Dockerfile runtime stage now creates and runs as a non-root `app` user — verified via `docker exec ... whoami` → `app`.

- [x] **1.5 — Wire main app's MCP client to the countries server** ✅ *Done (2026-07-19)*
  Configure `spring-ai-starter-mcp-client` in the main app to connect over SSE to `countries-mcp-server`; register the tool with the main `ChatClient`.
  *Done when:* the main app logs a successful MCP handshake/tool discovery on startup.
  *Notes:* Verified real config shape before writing code (Spring Initializr probe + jar inspection, same lesson as before): `spring.ai.mcp.client.sse.connections.<name>.url` / `.sse-endpoint` (property class `McpSseClientProperties`, prefix `spring.ai.mcp.client.sse`); confirmed `/sse` is the real endpoint path by curling the running `countries-mcp-server` directly (got a genuine open SSE stream, HTTP 200). Spring AI auto-configures a `SyncMcpToolCallbackProvider` bean (`McpToolCallbackAutoConfiguration`) from the discovered MCP clients; wired it into `ChatClientConfig` via `builder.defaultTools(mcpToolCallbacks)` — note `defaultToolCallbacks(...)` (all 3 overloads) is deprecated-for-removal in this Spring AI version, `defaultTools(Object...)` is the current API.

  Verified live: main app startup log shows the MCP handshake (`Server response with Protocol: 2024-11-05, Capabilities: ...tools=ToolCapabilities[listChanged=true]...`), matched on the server side by `countries-mcp-server`'s own log (`Client initialize request ... Implementation[name=spring-ai-mcp-client - countries...]`). Bonus end-to-end sanity check: `./chat.sh "What is the capital of Germany?"` → "The capital of Germany is Berlin." — real answer through the full chain, though not by itself proof the tool was actually invoked (the model could know this fact regardless); a harder, unambiguous test comes later (task 1.7, and the multi-hop weather question in Slice 3).

- [x] **1.6 — System prompt for country/capital questions** ✅ *Done (2026-07-19)*
  Write/extend the system prompt so the LLM knows when to invoke the countries tool.
  *Done when:* the model reliably calls the tool rather than guessing from its own knowledge.
  *Notes:* Added a `SYSTEM_PROMPT` constant in `ChatClientConfig`, wired via `builder.defaultSystem(...)`, instructing the model to always call `getCountryInfo` for capital/region/population/language/currency questions instead of relying on its own (potentially outdated) knowledge.

  Verified with **actual proof of tool invocation**, not just plausible-looking answers (addressing the gap noted at the end of task 1.5's review): restarted the app with `--logging.level.io.modelcontextprotocol=DEBUG`, which logs `DefaultToolCallingManager : Executing tool call: getCountryInfo` on real invocation. Ran 4 questions: "capital of Germany", "capital city of Germany", "What do you know about Berlin?" (all 3 → tool called, confirmed via the debug log line, count went 0→1→2→3) and an unrelated "What is 7 times 8?" (tool NOT called, count stayed at 3, model even explicitly said "no tool calls were made"). 4/4 correct behavior — reliable in both directions, not just eager to call the tool for everything.

- [x] **1.7 — Manual verification: capital + Berlin questions** ✅ *Done (2026-07-20)*
  Run "What is the capital city of Germany?" and "What do you know about Berlin?" through `chat.sh`; capture the transcripts.
  *Done when:* both answers are correct and demonstrably backed by a live tool call (not hallucinated).
  *Notes:* Brought up `ollama` + `countries-mcp-server` via `docker compose up -d` (both reported healthy), ran the main app locally via `./gradlew bootRun --args='--logging.level.io.modelcontextprotocol=DEBUG'`. Confirmed the MCP handshake and `getCountryInfo` tool discovery in the startup log, then ran both questions through `chat.sh`.

  Transcript:
  ```
  $ ./chat.sh "What is the capital city of Germany?"
  The capital city of Germany is Berlin.

  $ ./chat.sh "What do you know about Berlin?"
  Berlin is the capital city of Germany, located in the European region. According to the
  latest data, Germany has a population of approximately 83,467,117 people, with German as
  the official language and the Euro (€) as the currency.
  ```

  Proof of live tool invocation (not hallucinated): the task 1.6 log line (`DefaultToolCallingManager : Executing tool call: ...`) didn't appear this run (different logger active at that level), so verification instead used the raw MCP protocol log, which is unambiguous — each question produced its own `Sending message for method tools/call` immediately followed by a `Received JSON message` containing the real `CountriesApiClient` payload (`{"name":"Germany","capital":"Berlin","region":"Europe","population":83467117,"languages":["German"],"currencies":["Euro"]}`), matching the live data verified back in task 1.2. Two questions → two independent `tools/call` round trips (log count went 0→1→2), each with matching request/response timestamps and the real API payload embedded in the response — not something the model could produce from static knowledge alone.

## Slice 2 — Weather MCP Integration + Munich Question

- [x] **2.1 — Obtain and configure `WEATHER_API_KEY`** ✅ *Done (2026-07-20)*
  Sign up at weatherapi.com for a free key; create `example.env` with a placeholder; add the real key to a git-ignored `.env`.
  *Done when:* `.env` is populated and `example.env` is committed with a placeholder only.
  *Notes:* You signed up for a free weatherapi.com account and added `WEATHER_API_KEY` to the git-ignored `.env`, same pattern as `COUNTRIES_API_KEY`. Added the `WEATHER_API_KEY=your_weatherapi_api_key_here` placeholder line to `example.env`. Verified live: `curl "https://api.weatherapi.com/v1/current.json?key=$WEATHER_API_KEY&q=Munich"` returned real current-conditions data for Munich (18.3°C, Sunny) — confirms the key is valid and the endpoint/response shape needed for task 2.4's tool implementation.

- [x] **2.2 — Vendor `mcp-weather` into the repo** ✅ *Done (2026-07-20)*
  Clone/copy the `mcp-weather` source into the repo (e.g. `weather-mcp/`), pinned to a known-working commit/version.
  *Done when:* `npm install && npm start` runs it standalone and it responds to a manual MCP stdio call.
  *Notes:* Located the real upstream repo (`plan.md` only linked the mcpservers.org listing page): `https://github.com/semdin/mcp-weather`, a single-commit repo (`8bb7bd1b8fa7364e6f0ea7772be48c25f4a38038`, the only commit/HEAD on `main` — that's the "known-working commit" pinned). Vendored as a plain subdirectory (`weather-mcp/`), not a git submodule — simpler for this project, no submodule init/update step for anyone cloning the recruiting repo. Copied `src/`, `package.json`, `package-lock.json`, `tsconfig.json`, `README.md`, and its own `.gitignore` (excludes `node_modules`, `build`, `.env`); `node_modules` confirmed excluded via `git check-ignore -v`.

  Two real gotchas found by inspecting the actual source (not just the README), relevant to task 2.4's wiring:
  1. `src/index.ts` calls `dotenv.config({ path: "C:/your/path/.env" })` — a hardcoded Windows placeholder path left in by upstream, contradicting the README's claim that it defaults to the project root. Harmless for our use case: `dotenv.config()` silently no-ops when the path doesn't exist and does not clear already-set `process.env` vars, and our deployment plan passes `WEATHER_API_URL`/`WEATHER_API_KEY` as real process env vars to the spawned subprocess anyway (not via its own `.env` file), so this doesn't need patching — flagging it so it isn't mistaken for a bug in task 2.4's wiring if the env vars ever silently fail to reach the process.
  2. `npm start` (→ `tsx src/index.ts`) prints npm's own banner lines to stdout *before* the JSON-RPC stream starts, which would break a real MCP client expecting only JSON-RPC on stdio. Invoking `npx tsx src/index.ts` directly (skipping the `npm start` wrapper) avoids this — verified both ways. Task 2.4 should spawn via `npx tsx src/index.ts`, not `npm start`.
  3. The tool's own schema description says "get weather info from openweather" even though it actually calls whatever `WEATHER_API_URL` points at (weatherapi.com, per our `.env`) — cosmetic upstream inaccuracy, not something we control since we're vendoring as-is; worth keeping in mind when writing task 2.5's system prompt (don't let the misleading description couple our prompt wording to "OpenWeather").

  Verified live end-to-end: `npm install` (114 packages, no errors) then a manual Node script spawned the server (`npx tsx src/index.ts`, `WEATHER_API_URL`/`WEATHER_API_KEY` passed as env vars) and drove real newline-delimited JSON-RPC over stdio — `initialize` → real MCP handshake response (`protocolVersion 2024-11-05`, `serverInfo semdin-weather-mcp`), `tools/list` → `get-weather` tool discovered, `tools/call` with `{"city":"Munich"}` → `"the weather in Munich is currently: 18.3"`, matching the live weatherapi.com value confirmed in task 2.1. Confirms the vendored copy is genuinely functional standalone, not just present.

- [x] **2.3 — Multi-stage Dockerfile for the main app** ✅ *Done (2026-07-20)*
  Extend the main app's Dockerfile: a Node build stage for `mcp-weather`, final image containing both the JRE and Node runtime.
  *Done when:* the built image can spawn the `mcp-weather` process internally.
  *Notes:* Main app had no Dockerfile yet (only `countries-mcp-server` did) — added a repo-root `Dockerfile`, three stages: `java-build` (`eclipse-temurin:21-jdk`, builds root project's `bootJar` — pinned to `archiveFileName = "app.jar"` in `build.gradle.kts`, same pattern as `countries-mcp-server`), `node-build` (`node:22-bookworm-slim`, `npm ci` for `weather-mcp`, keeping devDependencies since `tsx` — needed at runtime per task 2.2's finding — lives there), and a final `eclipse-temurin:21-jre` stage with `nodejs`/`npm` installed via `apt` (Ubuntu 26.04's own repo already carries Node 22.22.1/npm 9.2.0, no NodeSource needed) plus the built `app.jar` and the `node-build` stage's `/weather-mcp` (including its `node_modules`) copied in; runs as a non-root `app` user, matching `countries-mcp-server`'s pattern.

  Hit and fixed one real build failure: `./gradlew bootJar` (no leading colon) runs the `bootJar` task in *every* subproject including `countries-mcp-server`, which failed since only root `src` is copied into this image — fixed by scoping it to `:bootJar` (root project only), mirroring how `countries-mcp-server/Dockerfile` already scopes its own build to `:countries-mcp-server:bootJar`. Also added `weather-mcp/node_modules` and `weather-mcp/build` to `.dockerignore` (58MB local `node_modules` was otherwise being sent into the build context needlessly, since `node-build` reinstalls it anyway).

  Verified live, not just "docker build succeeded": `docker build` completed successfully; inside the built image confirmed `whoami` → `app` (non-root), `node --version` → `v22.22.1`, and `weather-mcp/node_modules` present (including `esbuild`, `tsx`'s native dependency — the cross-stage copy from Debian's `node-build` into the final Ubuntu-based JRE image works fine since both are glibc-linux, same arch). Real functional proof: ran a container with `WEATHER_API_URL`/`WEATHER_API_KEY` env vars set and piped a manual newline-delimited JSON-RPC `initialize` → `tools/list` → `tools/call` sequence into `npx tsx src/index.ts` *inside the container* — got a real MCP handshake, `get-weather` tool discovery, and a live weatherapi.com reading for Munich (`"the weather in Munich is currently: 18.1"`), i.e. the built image can genuinely spawn and talk to `mcp-weather` internally, not just contain the files. Also sanity-checked the Java side: running the container standalone (no Ollama/MCP network) showed Tomcat starting cleanly on port 8080 and the expected MCP SSE connection error (host unreachable) — confirms the JAR itself is packaged correctly; full startup requires the Compose network, which is task 2.4's concern.

- [x] **2.4 — Wire MCP client to spawn `mcp-weather` via stdio** ✅ *Done (2026-07-20)*
  Configure the main app's MCP client to launch `mcp-weather` as a stdio subprocess on startup, passing `WEATHER_API_KEY` through from the environment.
  *Done when:* app startup logs show a successful stdio MCP handshake with the weather tool discovered.
  *Notes:* Pure `application.yml` change, no Java code — `spring.ai.mcp.client.stdio.connections.weather.{command,args,env}` (confirmed the real property shape by decompiling `McpStdioClientProperties`/`StdioTransportAutoConfiguration` from `spring-ai-autoconfigure-mcp-client-common-2.0.0.jar`, prefix `spring.ai.mcp.client.stdio`). `ChatClientConfig`'s existing `mcpToolCallbacks` bean picked up the new stdio client automatically alongside the SSE one — Spring AI's `McpClientAutoConfiguration` aggregates all `NamedClientMcpTransport` beans (SSE + stdio) into the same tool-callback provider, no wiring code needed.

  Two things discovered by reading the actual `ServerParameters`/`StdioClientTransport` source (`mcp-core-2.0.0-sources.jar`) rather than assuming, both critical to getting this right:
  1. `ServerParameters` has **no working-directory field**, and `StdioClientTransport` spawns via a bare `new ProcessBuilder()` (no `.directory(...)` call) — the subprocess always inherits the **JVM's own cwd**. Used relative paths (`command: weather-mcp/node_modules/.bin/tsx`, `args: [weather-mcp/src/index.ts]`) instead of `npx tsx` (task 2.2's approach) specifically because `npx`'s local-package resolution walks *upward* from cwd and would never find `weather-mcp/node_modules` if cwd were the JVM's directory and weather-mcp were a subdirectory of it — a direct path to the binary sidesteps that resolution entirely. Verified this relative-path form works because `weather-mcp/` happens to be a direct child of the JVM's cwd in both places this runs: locally via `./gradlew bootRun` (cwd = repo root) and inside the Docker image (`WORKDIR /app`, `weather-mcp/` copied to `/app/weather-mcp`).
  2. `ServerParameters`' default `env` only inherits a small safe allowlist (`HOME`, `LOGNAME`, `PATH`, `SHELL`, `TERM`, `USER` on Linux) — **not** the full parent environment, so `WEATHER_API_KEY` would silently *not* reach the subprocess without explicitly setting it in the `env:` map. `WEATHER_API_URL` is hardcoded directly in `application.yml` (not a secret, same pattern as `countries-mcp-server`'s `base-url`); `WEATHER_API_KEY: ${WEATHER_API_KEY}` pulls the real key from the environment, no default (matches `countries.api.key: ${COUNTRIES_API_KEY}`'s required-no-fallback pattern).

  Verified live end-to-end, not just "config looks right": ran `./gradlew bootRun --args='--logging.level.io.modelcontextprotocol=DEBUG'` (with `ollama` + `countries-mcp-server` up via `docker compose up -d`) — startup log shows `StdioClientTransport : MCP server starting` → `MCP server started` → a real `initialize` handshake (`serverInfo={name=semdin-weather-mcp, version=1.0.0}`), same pattern as the countries SSE client already showed. Tool discovery is lazy (matches task 1.7's finding for the countries client) — triggered on the first `/chat` call, log shows `tools/list` → `get-weather` discovered. Bonus: also ran the actual Munich question through `chat.sh` right away (task 2.6's question, ahead of schedule) — *"The current temperature in Munich is 20.2°C."* — with the debug log showing a real `tools/call` → `{"content":[{"type":"text","text":"the weather in Munich is currently: 20.2"}]}`, i.e. the full chain already works correctly even before task 2.5's system prompt update. Left tasks 2.5/2.6 as formally separate per the "one task at a time" convention, but this is a strong signal 2.5 may turn out to be a no-op or light touch.

- [x] **2.5 — System prompt update for weather questions** ✅ *Done (2026-07-20)*
  Extend the system prompt to describe the weather tool's purpose (current temperature by city).
  *Done when:* the model reliably calls the weather tool for temperature questions.
  *Notes:* Added a paragraph to `ChatClientConfig.SYSTEM_PROMPT` describing the `get-weather` tool ("returns the current temperature for a given city") and instructing the model to always call it for current weather/temperature questions rather than answering from its own knowledge. Deliberately did not mention "OpenWeather" in the wording, despite the tool's own MCP schema description saying "get weather info from openweather" — flagged in task 2.2 as a cosmetic upstream inaccuracy (it actually calls whatever `WEATHER_API_URL` points at, weatherapi.com for us); didn't want our prompt coupled to a wrong provider name.

  Verified live with the same methodology as task 1.6 (debug logging, checking for real tool-call log lines, both positive and negative cases) — ran 4 questions through `chat.sh` against the running app (`ollama` + `countries-mcp-server` via `docker compose up -d`, main app via `./gradlew bootRun --args='--logging.level.io.modelcontextprotocol=DEBUG'`): "What is the temperature currently in Berlin?" and "What's the weather like in Paris right now?" both correctly triggered `get-weather` `tools/call`s (confirmed in the debug log, real weatherapi.com values returned — 19.4°C and 22.3°C), "What is the capital of France?" correctly triggered `getCountryInfo` instead (not `get-weather` — confirms no cross-contamination between the two tool descriptions), and "What is 12 times 7?" triggered no tool call at all (confirmed by the log ending with no further `tools/call` entries after the France lookup, and the model's own answer explicitly stating no tools were called). 4/4 correct routing.

  Hit an unrelated environment issue while testing, not a defect in this change: Ollama's `llama-server` subprocess got OOM-killed mid-request (`dmesg` confirmed real kernel OOM kills) due to host memory pressure from an unrelated JetBrains RemoteDev process plus a stale Gradle daemon; freed memory by stopping the stale daemon (`./gradlew --stop`) and retried successfully. Worth knowing for future test runs in this environment if Ollama requests start timing out unexpectedly.

- [x] **2.6 — Manual verification: Munich question** ✅ *Done (2026-07-20)*
  Run "What is the temperature currently in Munich?" through `chat.sh`; capture the transcript.
  *Done when:* the answer reflects a live weatherapi.com value for Munich.
  *Notes:* Already got a strong preliminary signal for this in task 2.4 (before the task 2.5 system-prompt update even existed), but ran a formal, clean verification now with the current code.

  Transcript:
  ```
  $ ./chat.sh "What is the temperature currently in Munich?"
  The current temperature in Munich is 21°C.
  ```

  Proof of live tool invocation, not hallucinated: debug log (`--logging.level.io.modelcontextprotocol=DEBUG`) shows a real `tools/call` → `Received JSON message: {"result":{"content":[{"type":"text","text":"the weather in Munich is currently: 21"}]}...}`. Independently cross-checked against a fresh direct curl to weatherapi.com moments later — `"temp_c":21.2` — confirms the value is genuinely live (small drift between the two readings a couple minutes apart is expected, not a discrepancy).

  Hit the same environment memory issue as task 2.5 while testing (Ollama's `llama-server` OOM-killed mid-generation this time, confirmed via `dmesg`) — retried after the crashed process freed memory and got a clean run. Also checked for a process-leak concern this raised: confirmed the `weather-mcp` stdio subprocess from the app instance killed at the end of task 2.5 did *not* linger as an orphan (`ps aux` showed no stray `node .../tsx` processes before starting this task's fresh run) — the OOM kills are a real but unrelated host memory-pressure issue in this environment, not a subprocess-lifecycle bug in the app.

  **Slice 2 is now fully complete**: all three required questions (capital of Germany, Berlin, Munich temperature) are answered correctly through live tool calls.

## Slice 3 — Multi-Hop Tool Orchestration

- [x] **3.1 — Manual verification: capital-of-Germany temperature question** ✅ *Done (2026-07-20)*
  Run "What is the temperature of the capital of Germany currently?" through `chat.sh` multiple times; check whether the model chains countries-tool → weather-tool correctly.
  *Done when:* a correct, tool-chained transcript is captured, or inconsistent behavior is documented.
  *Notes:* Ran it 5 times against the running app (`ollama` + `countries-mcp-server` via Docker, main app via `./gradlew bootRun --args='--logging.level.io.modelcontextprotocol=DEBUG'`), no additional prompt changes — the existing system prompt from tasks 1.6/2.5 already covers both tools individually, and that turned out to be sufficient for the model to chain them on its own.

  **Chaining reliability: 5/5 correct.** Every single run's debug log shows the exact same correct sequence — `getCountryInfo("Germany")` → `{"capital":"Berlin",...}` → `get-weather("Berlin")` → real temperature — never skipping the country lookup, never passing the wrong city (e.g. "Germany" instead of "Berlin") to the weather tool. Sample transcripts:
  ```
  $ ./chat.sh "What is the temperature of the capital of Germany currently?"
  The capital of Germany is Berlin, and the current temperature there is **18°C**.

  $ ./chat.sh "What is the temperature of the capital of Germany currently?"
  The current temperature in Berlin, the capital of Germany, is 18.3 degrees.

  $ ./chat.sh "What is the temperature of the capital of Germany currently?"
  The capital of Germany is Berlin, and the current temperature there is **19°C**.
  ```

  **Separate, unrelated finding: response latency, not reasoning.** One of the five `chat.sh` calls (150s client-side timeout) returned `exit 124` even though its debug log shows the chaining completed correctly server-side (`tools/call` pair present, right city) — the final natural-language answer just took longer than my client timeout to generate. This environment's Ollama is CPU-only (~16 tokens/sec observed) and had already hit real OOM kills during tasks 2.5/2.6 testing; slow/queued generation under load here is an infrastructure characteristic of this sandbox, not a defect in the multi-hop tool-chaining logic itself — the underlying reasoning was correct in that run too, confirmed via the log.

- [x] **3.2 — Prompt tuning for reliable chaining (if needed)** ✅ *Done (2026-07-20)*
  If chaining is unreliable, adjust the system prompt (e.g. explicit reasoning instructions) to improve consistency.
  *Done when:* repeated manual runs show materially improved chaining reliability, or the residual limitation is written up.
  *Notes:* Not needed — no-op. Task 3.1 already showed 5/5 reliable chaining with the existing per-tool system prompt (from tasks 1.6/2.5), so the "if needed" condition was never triggered. No changes made to `ChatClientConfig`.

- [x] **3.3 — Document outcome** ✅ *Done (2026-07-20)*
  Record the final behavior (reliable / partially reliable / not achievable) and rationale in `ai_flow/` notes, to feed into the README's "limitations" section later.
  *Done when:* the note exists and matches actual observed behavior.
  *Notes:* **Verdict: reliable.** Multi-hop tool chaining ("What is the temperature of the capital of Germany currently?") works correctly with `qwen3:4b` and required no special prompting — the per-tool system prompt descriptions already in place from tasks 1.6/2.5 (one sentence each for `getCountryInfo` and `get-weather`, no explicit chaining/reasoning instructions) were sufficient for the model to sequence them correctly on its own. Task 3.1 observed 5/5 correct chainings (`getCountryInfo("Germany")` → `Berlin` → `get-weather("Berlin")` → real temperature), so task 3.2 (prompt tuning) turned out to be a no-op.

  Residual caveat for the future README "Known limitations" section (task 7.4): reliability was about the *reasoning*, not response *latency* — this sandbox's Ollama is CPU-only (~16 tokens/sec observed) and occasionally OOM-killed under memory pressure during earlier testing (tasks 2.5/2.6), so multi-hop questions (two sequential tool round-trips plus a longer final generation) can take well over a minute and occasionally exceed a short client-side timeout, even though the underlying tool orchestration itself is correct every time it's given room to finish. Worth flagging as an infrastructure/hardware caveat, not a limitation of the tool-chaining approach itself.

## Slice 4 — RAG Knowledge Base (CDQ Fraud Guard)

- [x] **4.1 — Add `app` service to Docker Compose** ✅ *Done (2026-07-20)*
  Add an `app` service (built from the repo-root `Dockerfile` added in task 2.3) to `docker-compose.yml`, wired to `ollama` and `countries-mcp-server` on the Compose network, so `docker compose up --build` brings up the entire stack in one command instead of requiring the main app to be run separately via `./gradlew bootRun`.
  *Done when:* `docker compose up --build` starts all services including `app`, and `chat.sh` gets a real answer with no manually-started process.
  *Notes:* Originally deferred to bundle with task 4.2's `docker-compose.yml` edit, but done now on request, ahead of Slice 3. Added the `app` service: built from the repo-root `Dockerfile`, environment points at the Compose-internal service DNS names rather than `localhost` (`OLLAMA_BASE_URL=http://ollama:11434`, `COUNTRIES_MCP_URL=http://countries-mcp-server:8081`), `WEATHER_API_KEY` passed through from `.env` with the same required-no-default pattern as `countries-mcp-server`'s `COUNTRIES_API_KEY`. `depends_on` waits on `ollama` (healthy), `ollama-pull-model` (`service_completed_successfully` — so the app doesn't come up before `qwen3:4b` is actually pulled), and `countries-mcp-server` (healthy) — without this, the app's eager MCP client initialization at startup would race the other services.

  Verified live: `docker compose up --build -d` built and started all four containers with no manual steps; `app` container logs show both MCP handshakes succeeding purely over the Compose network (`serverInfo={name=semdin-weather-mcp,...}` for the stdio-spawned weather subprocess *inside* the container, and the countries SSE handshake). Ran both `./chat.sh "What is the capital city of Germany?"` → *"The capital city of Germany is Berlin."* and `./chat.sh "What is the temperature currently in Munich?"` → *"The current temperature in Munich is 21.3°C."* against the containerized app with zero manually-started local processes — confirms the full stack really does come up from `docker compose up --build` alone now. Updated the README's "Running the Application" section to lead with this simpler workflow.

- [x] **4.2 — Add pgvector service to Docker Compose** ✅ *Done (2026-07-20)*
  Add a `postgres` service using `pgvector/pgvector:pg17` to `docker-compose.yml`.
  *Done when:* the container starts and the pgvector extension is available.
  *Notes:* Added a `postgres` service (`pgvector/pgvector:pg17`, port 5432 published for local debugging convenience, `pg_isready` healthcheck, `postgres_data` named volume for persistence, matching the `ollama_data` pattern). Used fixed dev-only credentials (`postgres`/`postgres`, db `ai_assistant`) rather than the `.env`-sourced-secret pattern used for the real third-party API keys — this Postgres instance isn't a real external account/secret requiring sign-up, it's a local dev/CI database with no external exposure by default.

  Verified live, not just "container starts": `docker compose up -d postgres` → healthy; `docker exec ... psql -c "CREATE EXTENSION IF NOT EXISTS vector; SELECT extname, extversion FROM pg_extension..."` → `vector | 0.8.5`, confirming the extension genuinely works, not just that the image built. Went a step further and smoke-tested actual vector functionality: created a `vector(3)` column, inserted two rows, ran a `<->` (L2 distance) similarity query, got the mathematically correct nearest-neighbor ordering back (distance `0` for the identical vector, `5.196...` for the other) — real proof the vector index/operator machinery works, not just that the extension is installed.

- **4.3 — Add `PgVectorStore` dependency and config**
  Add `spring-ai-starter-vector-store-pgvector`; configure connection + `initialize-schema: true`.
  *Done when:* app startup creates the vector schema with no errors.

- **4.4 — Ingestion runner for `cdq_fraud_guard.md`**
  Implement an `ApplicationRunner` that, if the vector store is empty, chunks `ai_flow/data/cdq_fraud_guard.md` with `TokenTextSplitter`, embeds it, and stores it.
  *Done when:* on first startup, the vector store contains the expected number of chunks; on subsequent startups, ingestion is skipped.

- **4.5 — Embeddings model wiring (qwen3:4b, with fallback)**
  Configure Spring AI to use `qwen3:4b` for embeddings; if it fails to produce usable vectors, switch config to `nomic-embed-text` (pulled via Ollama) instead.
  *Done when:* embeddings are successfully generated and stored end-to-end with whichever model works.

- **4.6 — RAG advisor wiring**
  Add a retrieval advisor to the main `ChatClient` so relevant chunks are injected into the prompt for matching queries.
  *Done when:* a CDQ-specific question's prompt (traceable via logs) includes retrieved chunk content.

- **4.7 — Tests for ingestion + retrieval**
  Unit test for the chunking logic; Testcontainers-based integration test verifying embed-then-retrieve round-trip against a real pgvector instance.
  *Done when:* both tests pass via `./gradlew test`.

- **4.8 — Manual verification: CDQ Fraud Guard question**
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
