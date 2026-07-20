package com.cdq.aiassistant.rag;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.ApplicationArguments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FraudGuardIngestionRunnerTest {

	@Test
	void chunksAndStoresTheRealDocumentWhenVectorStoreIsEmpty() throws Exception {
		VectorStore vectorStore = mock(VectorStore.class);
		when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

		new FraudGuardIngestionRunner(vectorStore).run(mock(ApplicationArguments.class));

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<Document>> captor = ArgumentCaptor.forClass(List.class);
		verify(vectorStore).add(captor.capture());

		List<Document> chunks = captor.getValue();
		assertThat(chunks).isNotEmpty();
		assertThat(chunks.get(0).getText()).contains("CDQ Fraud Guard");
	}

	@Test
	void skipsIngestionWhenVectorStoreAlreadyHasContent() throws Exception {
		VectorStore vectorStore = mock(VectorStore.class);
		when(vectorStore.similaritySearch(any(SearchRequest.class)))
			.thenReturn(List.of(new Document("existing content")));

		new FraudGuardIngestionRunner(vectorStore).run(mock(ApplicationArguments.class));

		verify(vectorStore, never()).add(any());
	}

}
