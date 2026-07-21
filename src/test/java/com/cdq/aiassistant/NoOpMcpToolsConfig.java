package com.cdq.aiassistant;

import java.util.List;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration(proxyBeanMethods = false)
public class NoOpMcpToolsConfig {

	@Bean
	ToolCallbackProvider noOpToolCallbackProvider() {
		return ToolCallbackProvider.from(List.of());
	}

}
