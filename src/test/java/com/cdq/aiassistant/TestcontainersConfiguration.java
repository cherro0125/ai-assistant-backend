package com.cdq.aiassistant;

import java.io.IOException;

import com.github.dockerjava.api.DockerClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.ollama.OllamaContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Provides an Ollama container for tests, self-contained (no manually-run
 * `docker compose up` required). The qwen3:4b model is pulled once and
 * committed to a local image tag so subsequent test runs reuse it instead
 * of re-pulling ~2.5GB every time; the very first run is slow.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	private static final String BASE_IMAGE = "ollama/ollama:latest";
	private static final String MODEL = "qwen3:4b";
	private static final String BAKED_IMAGE = "ai-assistant-ollama-qwen3-4b:latest";

	@Bean
	@ServiceConnection
	OllamaContainer ollamaContainer() {
		if (dockerImageExists(BAKED_IMAGE)) {
			return new OllamaContainer(DockerImageName.parse(BAKED_IMAGE).asCompatibleSubstituteFor("ollama/ollama"));
		}
		return pullModelAndBakeImage();
	}

	private OllamaContainer pullModelAndBakeImage() {
		OllamaContainer container = new OllamaContainer(BASE_IMAGE);
		container.start();
		try {
			ExecResult result = container.execInContainer("ollama", "pull", MODEL);
			if (result.getExitCode() != 0) {
				throw new IllegalStateException(
						"Failed to pull model " + MODEL + " into the Ollama test container: " + result.getStderr());
			}
		}
		catch (IOException | InterruptedException e) {
			throw new IllegalStateException("Failed to pull model " + MODEL + " into the Ollama test container", e);
		}
		container.commitToImage(BAKED_IMAGE);
		return container;
	}

	private boolean dockerImageExists(String imageTag) {
		DockerClient client = DockerClientFactory.instance().client();
		return !client.listImagesCmd().withReferenceFilter(imageTag).exec().isEmpty();
	}

}
