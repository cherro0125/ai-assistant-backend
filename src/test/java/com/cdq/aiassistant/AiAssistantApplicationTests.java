package com.cdq.aiassistant;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

// TestcontainersConfiguration provides both Ollama and Postgres: pgvector needs a real
// database since FraudGuardIngestionRunner runs on every startup (including this test's
// context load) and queries the vector store.
@WithoutMcpClients
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class AiAssistantApplicationTests {

	@Test
	void contextLoads() {
	}

}
