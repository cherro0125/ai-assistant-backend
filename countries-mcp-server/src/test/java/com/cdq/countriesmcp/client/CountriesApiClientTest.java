package com.cdq.countriesmcp.client;

import java.util.Optional;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.web.client.RestClient;

import com.cdq.countriesmcp.CountryInfo;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

class CountriesApiClientTest {

	@RegisterExtension
	static WireMockExtension wireMock = WireMockExtension.newInstance().build();

	private CountriesApiClient client() {
		return new CountriesApiClient(RestClient.builder(), wireMock.baseUrl(), "test-key");
	}

	@Test
	void findByNameReturnsCountryInfoWhenFound() {
		wireMock.stubFor(get(urlEqualTo("/countries/v5/names.common/Germany"))
				.withHeader("Authorization", equalTo("Bearer test-key"))
				.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Content-Type", "application/json")
						.withBody("""
								{
								  "data": {
								    "objects": [
								      {
								        "names": {"common": "Germany"},
								        "capitals": [{"name": "Berlin"}],
								        "region": "Europe",
								        "population": 83497147,
								        "languages": [{"name": "German"}],
								        "currencies": [{"code": "EUR", "name": "Euro", "symbol": "€"}]
								      }
								    ]
								  }
								}
								""")));

		Optional<CountryInfo> result = client().findByName("Germany");

		assertThat(result).isPresent();
		CountryInfo info = result.get();
		assertThat(info.name()).isEqualTo("Germany");
		assertThat(info.capital()).isEqualTo("Berlin");
		assertThat(info.region()).isEqualTo("Europe");
		assertThat(info.population()).isEqualTo(83497147L);
		assertThat(info.languages()).containsExactly("German");
		assertThat(info.currencies()).containsExactly("Euro");
	}

	@Test
	void findByNameReturnsEmptyWhenNotFound() {
		wireMock.stubFor(get(urlEqualTo("/countries/v5/names.common/Wakanda"))
				.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Content-Type", "application/json")
						.withBody("""
								{"data": {"objects": []}}
								""")));

		Optional<CountryInfo> result = client().findByName("Wakanda");

		assertThat(result).isEmpty();
	}

}
