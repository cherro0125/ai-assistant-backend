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
