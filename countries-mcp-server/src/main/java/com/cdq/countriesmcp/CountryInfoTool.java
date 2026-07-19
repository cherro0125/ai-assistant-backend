package com.cdq.countriesmcp;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import com.cdq.countriesmcp.client.CountriesApiClient;

@Component
public class CountryInfoTool {

	private final CountriesApiClient countriesApiClient;

	public CountryInfoTool(CountriesApiClient countriesApiClient) {
		this.countriesApiClient = countriesApiClient;
	}

	@McpTool(name = "getCountryInfo",
			description = "Look up country information (capital city, region, population, languages, currencies) by country name.")
	public CountryInfo getCountryInfo(
			@McpToolParam(description = "The country name, e.g. 'Germany'", required = true) String countryName) {
		return countriesApiClient.findByName(countryName)
				.orElseThrow(() -> new IllegalArgumentException("No country found matching: " + countryName));
	}

}
