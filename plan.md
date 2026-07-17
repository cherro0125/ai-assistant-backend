# Recruiting task

## Task Description

- Implement an AI Assistant with Java Frameworks and provide a chat interface
- Integrate the following knowledge sources:
  - Local Vector database pgvector/pgvector:pg17 populated with CDQ Product information (RAG, convert to vector embeddings, take plain text from here https://www.cdq.com/products/cdq-fraud-guard)
  - Remote free REST Service (https://restcountries.com/) → write own MCP server
  - Local free MCP server (https://mcpservers.org/servers/semdin/mcp-weather)
- Use a local model qwen3:4b with Ollama
- Provide tests
- Provide answers to the following questions:
  - What is the capital city of Germany?
  - What is the temperature currently in Munich?
  - What is the temperature of the capital of Germany currently?
  - What do you know about Berlin?
  - \<your own questions to show off\>
- Out of scope:
  - No solution for long/short-term memory required

## Task requirements

- Provide the source code of the AI Assistant in a public repository of your choice
- Run the AI assistant and provide the answers
- Please provide a README that describes how to run the service and execute the tests
- Using AI is explicitly allowed; explain how you used AI to fulfill the task
- If you were not able to fulfill a task, then explain why
