package com.cdq.aiassistant.chat;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientConfig {

	@Bean
	ChatClient chatClient(ChatClient.Builder builder, ToolCallbackProvider mcpToolCallbacks) {
		return builder
				.defaultTools(mcpToolCallbacks)
				.build();
	}

}
