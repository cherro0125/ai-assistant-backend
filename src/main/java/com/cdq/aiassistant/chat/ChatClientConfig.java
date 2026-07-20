package com.cdq.aiassistant.chat;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientConfig {

	private static final String SYSTEM_PROMPT = """
			You are an AI assistant with access to tools for looking up live, authoritative data.

			You have a `getCountryInfo` tool that returns a country's capital city, region, \
			population, languages, and currencies. For any question about a country's capital, \
			region, population, languages, or currencies, always call this tool rather than \
			answering from your own knowledge — your training data may be outdated, and the tool \
			returns current, authoritative data.

			You also have a `get-weather` tool that returns the current temperature for a given \
			city. For any question about the current weather or temperature in a city, always \
			call this tool rather than answering from your own knowledge — your training data has \
			no access to live conditions.
			""";

	@Bean
	ChatClient chatClient(ChatClient.Builder builder, ToolCallbackProvider mcpToolCallbacks) {
		return builder
				.defaultSystem(SYSTEM_PROMPT)
				.defaultTools(mcpToolCallbacks)
				.build();
	}

}
