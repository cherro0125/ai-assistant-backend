package com.cdq.countriesmcp.client;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RestCountriesResponse(Data data) {

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Data(List<CountryObject> objects) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record CountryObject(
			Names names,
			List<Capital> capitals,
			String region,
			long population,
			List<Language> languages,
			List<Currency> currencies) {

		@JsonIgnoreProperties(ignoreUnknown = true)
		public record Names(String common) {
		}

		@JsonIgnoreProperties(ignoreUnknown = true)
		public record Capital(String name) {
		}

		@JsonIgnoreProperties(ignoreUnknown = true)
		public record Language(String name) {
		}

		@JsonIgnoreProperties(ignoreUnknown = true)
		public record Currency(String code, String name, String symbol) {
		}
	}

}
