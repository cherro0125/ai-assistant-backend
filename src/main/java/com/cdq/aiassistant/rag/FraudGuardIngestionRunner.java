package com.cdq.aiassistant.rag;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class FraudGuardIngestionRunner implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(FraudGuardIngestionRunner.class);

	private static final Path SOURCE_PATH = Path.of("ai_flow/data/cdq_fraud_guard.md");

	private final VectorStore vectorStore;

	public FraudGuardIngestionRunner(VectorStore vectorStore) {
		this.vectorStore = vectorStore;
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {
		if (alreadyIngested()) {
			log.info("Vector store already contains CDQ Fraud Guard content, skipping ingestion.");
			return;
		}

		String content = Files.readString(SOURCE_PATH);
		List<Document> chunks = TokenTextSplitter.builder().build().split(new Document(content));
		vectorStore.add(chunks);

		log.info("Ingested {} chunk(s) from {} into the vector store.", chunks.size(), SOURCE_PATH);
	}

	private boolean alreadyIngested() {
		List<Document> existing = vectorStore
			.similaritySearch(SearchRequest.builder().query("CDQ Fraud Guard").topK(1).build());
		return !existing.isEmpty();
	}

}
