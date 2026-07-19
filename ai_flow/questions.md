# Clarification Questions & Answers

This document records every clarification question asked during planning (via Claude Code), the answers given, and what was changed in `ai_flow/detailed_plan.md` (or other files) as a result.

---

## 1. Scope of `plan.md`

**Question:** Should `plan.md` be a plain markdown transcription of the PDF's task description, or an actual implementation plan (architecture, tech stack, step-by-step build order) for completing this task?

**Answer:** Verbatim transcription.

**Change made:** `plan.md` was created as a clean markdown transcription of the original `task.pdf` content, preserving the original wording and structure (Task Description, Task requirements). No architecture or planning content was added to this file — that was split out into `ai_flow/detailed_plan.md` instead.

---

## 2. Embeddings model for RAG

**Question:** `qwen3:4b` likely can't generate embeddings for RAG. How should embeddings be handled?

**Answer:** Try `qwen3:4b` for embeddings first.

**Change made:** Updated the Tech Stack table in `ai_flow/detailed_plan.md` to specify `qwen3:4b` (tried first) as the embeddings model, with a fallback to a dedicated embedding model (e.g. `nomic-embed-text`) only if `qwen3:4b` doesn't work. Added this as a confirmed decision in the "Decisions Made" section.

---

## 3. Chat interface type

**Question:** What kind of chat interface should the assistant expose (REST endpoint only, REST + web UI, or CLI)?

**Answer:** REST endpoint + a bash script to simplify the call.

**Change made:** Updated the Tech Stack table and step 5 ("Chat orchestration") in `ai_flow/detailed_plan.md`: the interface is a `/chat` REST endpoint with a bash script wrapper (e.g. `chat.sh "question"`) around `curl`, instead of a web UI. Removed the "minimal HTML/JS or CLI front-end" option.

---

## 4. Repository choice

**Question:** Which repository should hold this project's source code — reuse `ai-assistant-backend` (already created earlier this session) or create a new one?

**Answer:** Reuse `ai-assistant-backend`.

**Change made:** Confirmed as a decision in the "Decisions Made" section of `ai_flow/detailed_plan.md`; removed from the open risks list.

---

## 5. "What do you know about Berlin?" question source

**Question:** This question isn't covered by any of the three knowledge sources as originally scoped (RAG only has CDQ Fraud Guard content). How should the assistant answer it?

**Answer:** Fetch it from `https://restcountries.com/` using the custom-built countries MCP server.

**Change made:** Updated step 7 ("Answer the required questions") in `ai_flow/detailed_plan.md` to note this question is answered via the custom countries MCP server (restcountries.com lookup for Germany — capital, region, population, languages, currencies), not RAG or general LLM knowledge. Also updated the Tech Stack row for the custom MCP server to mention it's used for both the "capital of Germany" and "Berlin" style questions. Added as a confirmed decision.

---

## 6. Docker availability

**Question:** The plan assumes Docker Compose (Postgres/pgvector, Ollama) and Testcontainers for integration tests — is Docker available in the run/test environment?

**Answer:** Yes, Docker is available and welcome/preferred.

**Change made:** Moved this item from "Open Questions / Risks" into "Decisions Made" in `ai_flow/detailed_plan.md`, confirming Docker Compose and Testcontainers will be used as originally planned — no change to the technical approach, just removed the uncertainty.

---

## 7. Custom "show off" questions

**Question:** Do you have specific custom questions in mind to demonstrate the assistant, or should Claude Code propose some that combine RAG + both MCP servers?

**Answer:** *Pending — not yet answered.*

**Change made:** None yet. This remains an open item in the "Open Questions / Risks" section of `ai_flow/detailed_plan.md` until answered.

---

## 8. WEATHER_API_KEY handling

**Question:** `mcp-weather` requires a free `WEATHER_API_KEY` from weatherapi.com to function at all — this has to be obtained by you, not Claude Code. How should it be handled?

**Answer:** Combined approach: store it in a `.env` file; commit an `example.env` with a placeholder; README should include step-by-step instructions for getting a free key and where to put it.

**Change made:** Added to `ai_flow/tech_stack.md` under "Decisions Made (follow-up)" and updated the "External MCP Server" table row. This will also need to be reflected in `README.md` once implementation docs are written (sign-up steps + `.env` setup).

---

## 9. Weather MCP server deployment (stdio transport constraint)

**Question:** `mcp-weather` only supports stdio transport, so it can't run as its own Docker Compose network service like the custom countries MCP server can. How should it be deployed?

**Answer:** Bundle Node.js into the main app's Docker image so the Spring Boot app can spawn `mcp-weather` as a local stdio subprocess.

**Change made:** Added to `ai_flow/tech_stack.md` under "Decisions Made (follow-up)" — confirms the multi-stage Docker build approach (JRE + Node runtime in the same final image) so `docker compose up` remains a single entry point.

---

## 10. Build tool correction (Maven → Gradle)

**Not a question Claude Code asked** — a correction to a default I picked. `tech_stack.md` initially chose Maven with the reasoning "no strong reason to prefer Gradle here." You pointed out that by that same logic, there's also no strong reason to prefer Maven, and you'd rather use Gradle.

**Change made:** Updated the Build tool row in `ai_flow/tech_stack.md` from Maven to Gradle (Kotlin DSL).

---

## 11. restcountries.com free API no longer exists (Slice 1, task 1.2)

**Not a question Claude Code asked initially — a blocker discovered mid-implementation.** While implementing the `getCountryInfo` tool, live-checked `restcountries.com` before writing code and found its legacy free/keyless API (v1–v4, including the `v3.1` endpoint the original task links to) has been fully shut down. The current v5 API returns `401 Authorization key required` for every request, including basic lookups — no free/keyless tier appears to remain for the country-data API itself (a separate free flag-image CDN exists but is unrelated). This directly contradicts the task's premise: *"Remote free REST Service (restcountries.com)."*

**Question:** How to proceed — switch to a different free API, have you sign up for a paid/free-tier key yourself, or document this as a task limitation per the task's own "if you were not able to fulfill a task, explain why" allowance?

**Answer:** Sign up yourself and provide the key (same pattern as `WEATHER_API_KEY`). You then provided a real key (`rc_live_...`).

**Change made:** Confirmed the signup page does offer a free tier ("Create a free account to access your API key", 500 requests/month). Determined the real v5 auth mechanism (bearer token) and endpoint shape (`https://api.restcountries.com/countries/v5/names.common/{name}`, JSON:API-style response) by testing directly against the live API with the provided key. Stored the key as `COUNTRIES_API_KEY` in `.env` (placeholder in `example.env`). Updated `ai_flow/tech_stack.md`'s "Custom MCP Server" section (API auth row, corrected from "None required — free/keyless") to reflect the real auth mechanism and endpoint.
