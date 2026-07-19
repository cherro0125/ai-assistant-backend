# AI Assistant — Claude Code Guide

Java/Spring AI recruiting-task project: a chat assistant using RAG, a custom MCP server, and an external MCP server, orchestrated via Ollama (`qwen3:4b`). See `plan.md` for the original task requirements.

## Start here

Before doing anything, read these in order:

1. **`ai_flow/tasks.md`** — the source of truth for progress. Every task has a checkbox, a Definition of Done, and a *Notes* section explaining what was actually built, gotchas hit, and why. Find the first unchecked task — that's what's next.
2. **`ai_flow/tech_stack.md`** — concrete technology decisions and why, including corrections made after real-world verification (e.g. actual dependency versions, actual third-party API behavior).
3. **`ai_flow/vertical_slices.md`** — the overall build plan, slice by slice.
4. **`ai_flow/questions.md`** — history of clarifications and blockers, with what changed as a result. Check this before re-deciding something that was already settled.
5. **`ai_flow/backlog.md`** — ideas deliberately deferred, not forgotten. Don't act on these unless asked.

## Working conventions established in this project

- **One task at a time.** Only implement the specific task requested from `ai_flow/tasks.md`, even if a follow-up task would be a trivial, directly-dependent shortcut. Ask before bundling.
- **Verify, don't guess, especially for library/API surfaces.** This project has repeatedly hit real mistakes from assuming a remembered API shape was still current: Spring Boot 3.3/Spring AI 1.0 turned out to be Spring Boot 4.1/Spring AI 2.0; `TestRestTemplate` moved packages; Jackson split into `tools.jackson.*` while `jackson-annotations` stayed under `com.fasterxml`; the plain `wiremock` artifact fails at runtime and needs `wiremock-standalone`; restcountries.com's entire free API was shut down and replaced with a paid-looking-but-actually-free-tier v5 API with a different auth mechanism and response shape. Before writing code against a library or external API, check the actual resolved classpath (`./gradlew :module:dependencies`), inspect the real jar/class, or curl the real live endpoint — don't rely on training-data memory of what "should" be true.
- **After implementing a task, run a code-review pass** before moving to the next one (verify claims live where practical — curl a real endpoint, run the real test, don't just read the diff). Fix confirmed findings, then re-review once.
- **Update `ai_flow/tasks.md` (and `vertical_slices.md`, `tech_stack.md`, `questions.md` where relevant) as you go** — check off the task, add a *Notes* entry describing what was actually built and any deviations from the original plan.
- **Commit messages**: `[S<slice>][T<task>] Description` (e.g. `[S1][T4] Add countries MCP server to Docker Compose with healthcheck and non-root user`).

## Secrets

`.env` (git-ignored) holds real API keys — `example.env` documents the required keys with placeholders. See the README's "Setup — API Keys" section for how to obtain them.
