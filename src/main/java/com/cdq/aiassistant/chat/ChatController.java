package com.cdq.aiassistant.chat;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class ChatController {

	private final ChatClient chatClient;

	public ChatController(ChatClient chatClient) {
		this.chatClient = chatClient;
	}

	@PostMapping("/chat")
	public ChatResponse chat(@RequestBody ChatRequest request) {
		if (request.message() == null || request.message().isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "message must not be blank");
		}
		String answer = chatClient.prompt()
				.user(request.message())
				.call()
				.content();
		return new ChatResponse(answer);
	}

}
