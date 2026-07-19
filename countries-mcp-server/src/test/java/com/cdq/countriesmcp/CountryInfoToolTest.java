package com.cdq.countriesmcp;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cdq.countriesmcp.client.CountriesApiClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CountryInfoToolTest {

	@Mock
	private CountriesApiClient countriesApiClient;

	@Test
	void getCountryInfoReturnsResultWhenFound() {
		CountryInfo germany = new CountryInfo("Germany", "Berlin", "Europe", 83497147L,
				List.of("German"), List.of("Euro"));
		when(countriesApiClient.findByName("Germany")).thenReturn(Optional.of(germany));

		CountryInfoTool tool = new CountryInfoTool(countriesApiClient);

		assertThat(tool.getCountryInfo("Germany")).isEqualTo(germany);
	}

	@Test
	void getCountryInfoThrowsWhenNotFound() {
		when(countriesApiClient.findByName("Wakanda")).thenReturn(Optional.empty());

		CountryInfoTool tool = new CountryInfoTool(countriesApiClient);

		assertThatThrownBy(() -> tool.getCountryInfo("Wakanda"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Wakanda");
	}

}
