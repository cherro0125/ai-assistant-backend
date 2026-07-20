package com.cdq.aiassistant.chat;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
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

	// The default QuestionAnswerAdvisor template unconditionally instructs the model to
	// refuse anything not found in the retrieved context, even when no documents were
	// retrieved (e.g. unrelated questions) — this replacement only asks it to prefer the
	// context when relevant, otherwise fall back to its normal behavior (including tools).
	private static final PromptTemplate RAG_PROMPT_TEMPLATE = new PromptTemplate("""
			{query}

			If relevant, use the following context to inform your answer. If it's empty or \
			not relevant to the question, ignore it and answer normally.

			---------------------
			{question_answer_context}
			---------------------
			""");

	@Bean
	ChatClient chatClient(ChatClient.Builder builder, ToolCallbackProvider mcpToolCallbacks, VectorStore vectorStore) {
		QuestionAnswerAdvisor ragAdvisor = QuestionAnswerAdvisor.builder(vectorStore)
				.searchRequest(SearchRequest.builder().similarityThreshold(0.5).topK(3).build())
				.promptTemplate(RAG_PROMPT_TEMPLATE)
				.order(0)
				.build();
		SimpleLoggerAdvisor loggerAdvisor = SimpleLoggerAdvisor.builder().order(1).build();

		return builder
				.defaultSystem(SYSTEM_PROMPT)
				.defaultTools(mcpToolCallbacks)
				.defaultAdvisors(ragAdvisor, loggerAdvisor)
				.build();
	}

}
