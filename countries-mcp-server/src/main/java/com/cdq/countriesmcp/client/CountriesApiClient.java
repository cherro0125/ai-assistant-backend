package com.cdq.countriesmcp.client;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.cdq.countriesmcp.CountryInfo;

@Component
public class CountriesApiClient {

	private final RestClient restClient;

	public CountriesApiClient(RestClient.Builder restClientBuilder,
			@Value("${countries.api.base-url}") String baseUrl,
			@Value("${countries.api.key}") String apiKey) {
		this.restClient = restClientBuilder
				.baseUrl(baseUrl)
				.defaultHeader("Authorization", "Bearer " + apiKey)
				.build();
	}

	public Optional<CountryInfo> findByName(String countryName) {
		RestCountriesResponse response = restClient.get()
				.uri("/countries/v5/names.common/{name}", countryName)
				.retrieve()
				.body(RestCountriesResponse.class);

		if (response == null || response.data() == null || response.data().objects().isEmpty()) {
			return Optional.empty();
		}

		RestCountriesResponse.CountryObject country = response.data().objects().get(0);
		return Optional.of(toCountryInfo(country));
	}

	private CountryInfo toCountryInfo(RestCountriesResponse.CountryObject country) {
		String capital = country.capitals() == null || country.capitals().isEmpty()
				? null
				: country.capitals().get(0).name();

		List<String> languages = country.languages() == null
				? List.of()
				: country.languages().stream().map(RestCountriesResponse.CountryObject.Language::name).toList();

		List<String> currencies = country.currencies() == null
				? List.of()
				: country.currencies().stream().map(RestCountriesResponse.CountryObject.Currency::name).toList();

		return new CountryInfo(
				country.names().common(),
				capital,
				country.region(),
				country.population(),
				languages,
				currencies);
	}

}
