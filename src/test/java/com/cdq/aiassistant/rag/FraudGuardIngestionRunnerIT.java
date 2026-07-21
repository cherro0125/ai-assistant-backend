package com.cdq.aiassistant.rag;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.cdq.aiassistant.TestcontainersConfiguration;
import com.cdq.aiassistant.WithoutMcpClients;

import static org.assertj.core.api.Assertions.assertThat;

// MCP clients (SSE to countries-mcp-server, stdio to weather-mcp) aren't relevant to RAG
// ingestion/retrieval and require live external processes this test shouldn't depend on.
@WithoutMcpClients
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class FraudGuardIngestionRunnerIT {

	@Autowired
	VectorStore vectorStore;

	@Test
	void ingestedDocumentIsRetrievableViaRealEmbeddingSimilaritySearch() {
		List<Document> results = vectorStore
			.similaritySearch(SearchRequest.builder().query("fraud detection trust score bank account").topK(1).build());

		assertThat(results).isNotEmpty();
		assertThat(results.get(0).getText()).contains("CDQ Fraud Guard");
	}

}
