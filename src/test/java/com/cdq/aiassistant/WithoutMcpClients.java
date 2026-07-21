package com.cdq.aiassistant;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.ai.mcp.client.common.autoconfigure.McpClientAutoConfiguration;
import org.springframework.ai.mcp.client.common.autoconfigure.McpToolCallbackAutoConfiguration;
import org.springframework.ai.mcp.client.common.autoconfigure.StdioTransportAutoConfiguration;
import org.springframework.ai.mcp.client.common.autoconfigure.annotations.McpClientAnnotationScannerAutoConfiguration;
import org.springframework.ai.mcp.client.httpclient.autoconfigure.SseHttpClientTransportAutoConfiguration;
import org.springframework.ai.mcp.client.httpclient.autoconfigure.StreamableHttpHttpClientTransportAutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * Excludes MCP client autoconfiguration (SSE to countries-mcp-server, stdio to
 * weather-mcp) so tests don't depend on those live external processes, and
 * imports a no-op ToolCallbackProvider so ChatClientConfig's required
 * dependency is still satisfied.
 *
 * A no-op bean alone isn't enough on its own: the underlying mcpSyncClients
 * bean is a regular eagerly-instantiated singleton, unrelated to whether
 * anything actually consumes it, so it still opens real connections unless
 * its autoconfiguration class itself is excluded too (found in task 4.7).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@EnableAutoConfiguration(exclude = { StdioTransportAutoConfiguration.class, McpClientAutoConfiguration.class,
		McpToolCallbackAutoConfiguration.class, McpClientAnnotationScannerAutoConfiguration.class,
		SseHttpClientTransportAutoConfiguration.class, StreamableHttpHttpClientTransportAutoConfiguration.class })
@Import(NoOpMcpToolsConfig.class)
public @interface WithoutMcpClients {

}
