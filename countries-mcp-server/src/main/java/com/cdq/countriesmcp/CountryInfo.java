package com.cdq.countriesmcp;

import java.util.List;

public record CountryInfo(
		String name,
		String capital,
		String region,
		long population,
		List<String> languages,
		List<String> currencies) {
}
