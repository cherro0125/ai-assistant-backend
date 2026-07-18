package com.cdq.aiassistant.chat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.context.annotation.Import;

import com.cdq.aiassistant.TestcontainersConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class ChatControllerSmokeTest {

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	void chatEndpointReturnsARealAnswer() {
		ChatResponse response = restTemplate.postForObject(
				"/chat", new ChatRequest("What is 2 plus 2? Answer in one short sentence."), ChatResponse.class);

		assertThat(response).isNotNull();
		assertThat(response.answer()).isNotBlank();
	}

}
