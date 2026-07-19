package com.cdq.countriesmcp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Calls the real restcountries.com API (requires COUNTRIES_API_KEY in the
 * environment). Skipped entirely when the key isn't set, so `./gradlew
 * build` stays green without it. Task 1.2's verification step; a
 * WireMock-based, offline unit test is added separately in task 1.3.
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "COUNTRIES_API_KEY", matches = ".+")
class CountryInfoToolLiveTest {

	@Autowired
	private CountryInfoTool countryInfoTool;

	@Test
	void getCountryInfoReturnsCorrectDataForGermany() {
		CountryInfo info = countryInfoTool.getCountryInfo("Germany");

		assertThat(info.name()).isEqualTo("Germany");
		assertThat(info.capital()).isEqualTo("Berlin");
		assertThat(info.region()).isEqualTo("Europe");
		assertThat(info.population()).isGreaterThan(0);
		assertThat(info.languages()).contains("German");
		assertThat(info.currencies()).contains("Euro");
	}

}
