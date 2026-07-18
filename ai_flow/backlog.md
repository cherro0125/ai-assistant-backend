# Backlog

Ideas worth remembering but deliberately not acted on yet — not tied to a specific slice/task, revisit when relevant.

## Speed up the Ollama Testcontainers smoke test in CI

`ChatControllerSmokeTest` (Slice 0, task 0.5) uses a Testcontainers-managed `OllamaContainer` that bakes `qwen3:4b` into a local image on first run (~5 min), then reuses it (~20s) on the same Docker host. This is fast for local iteration, but a CI pipeline on ephemeral runners would re-pay the ~5 minute cost on every run since the baked image doesn't persist between runs.

Two options when CI is actually set up for this project:

1. **Pre-bake and publish the image to a registry** (e.g. GHCR): a separate scheduled/manual job builds the `ai-assistant-ollama-qwen3-4b` image and pushes it once; CI then does a fast `docker pull` of the ready-made image instead of pulling the model and committing a new one every run. Tradeoff: adds a small maintenance surface (a publish job to rebuild/push when the base Ollama image or model version changes).
2. **Exclude this test from the default CI run**: tag it as a local/manual-only test (consistent with how the actual required Q&A verification is already treated — see `tech_stack.md`'s Testing section), keeping CI fast with just unit/WireMock-based tests.

**Status:** not acted on — no CI pipeline exists for this project yet. Revisit if/when one is added.
