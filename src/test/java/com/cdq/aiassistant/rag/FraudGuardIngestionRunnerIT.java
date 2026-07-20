package com.cdq.aiassistant.rag;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.mcp.client.common.autoconfigure.McpClientAutoConfiguration;
import org.springframework.ai.mcp.client.common.autoconfigure.McpToolCallbackAutoConfiguration;
import org.springframework.ai.mcp.client.common.autoconfigure.StdioTransportAutoConfiguration;
import org.springframework.ai.mcp.client.common.autoconfigure.annotations.McpClientAnnotationScannerAutoConfiguration;
import org.springframework.ai.mcp.client.httpclient.autoconfigure.SseHttpClientTransportAutoConfiguration;
import org.springframework.ai.mcp.client.httpclient.autoconfigure.StreamableHttpHttpClientTransportAutoConfiguration;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import com.cdq.aiassistant.TestcontainersConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

// MCP clients (SSE to countries-mcp-server, stdio to weather-mcp) aren't relevant to RAG
// ingestion/retrieval and require live external processes this test shouldn't depend on.
// Their autoconfiguration is excluded outright — a no-op ToolCallbackProvider bean is
// supplied for ChatClientConfig's required dependency instead. (A no-op bean alone isn't
// enough: the underlying mcpSyncClients bean is a regular eagerly-instantiated singleton,
// unrelated to whether anything actually consumes it, so it still opens real connections
// unless its autoconfiguration class itself is excluded.)
@EnableAutoConfiguration(exclude = { StdioTransportAutoConfiguration.class, McpClientAutoConfiguration.class,
		McpToolCallbackAutoConfiguration.class, McpClientAnnotationScannerAutoConfiguration.class,
		SseHttpClientTransportAutoConfiguration.class, StreamableHttpHttpClientTransportAutoConfiguration.class })
@Import({ TestcontainersConfiguration.class, FraudGuardIngestionRunnerIT.NoOpMcpToolsConfig.class })
@Testcontainers
@SpringBootTest
class FraudGuardIngestionRunnerIT {

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer(
			DockerImageName.parse("pgvector/pgvector:pg17").asCompatibleSubstituteFor("postgres"));

	@Autowired
	VectorStore vectorStore;

	@Test
	void ingestedDocumentIsRetrievableViaRealEmbeddingSimilaritySearch() {
		List<Document> results = vectorStore
			.similaritySearch(SearchRequest.builder().query("fraud detection trust score bank account").topK(1).build());

		assertThat(results).isNotEmpty();
		assertThat(results.get(0).getText()).contains("CDQ Fraud Guard");
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class NoOpMcpToolsConfig {

		@Bean
		ToolCallbackProvider noOpToolCallbackProvider() {
			return ToolCallbackProvider.from(List.of());
		}

	}

}
