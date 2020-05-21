/*
 * Copyright 2016-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.web;

import static org.assertj.core.api.Assertions.*;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;

/**
 * Unit tests for {@link JsonProjectingMethodInterceptorFactory}.
 *
 * @author Oliver Gierke
 * @since 1.13
 */
class JsonProjectingMethodInterceptorFactoryUnitTests {

	ProjectionFactory projectionFactory;
	Customer customer;

	@BeforeEach
	void setUp() {

		String json = "{\"firstname\" : \"Dave\", "//
				+ "\"address\" : { \"zipCode\" : \"01097\", \"city\" : \"Dresden\" }," //
				+ "\"addresses\" : [ { \"zipCode\" : \"01097\", \"city\" : \"Dresden\" }]" + " }";

		SpelAwareProxyProjectionFactory projectionFactory = new SpelAwareProxyProjectionFactory();

		MappingProvider mappingProvider = new JacksonMappingProvider(new ObjectMapper());
		projectionFactory.registerMethodInvokerFactory(new JsonProjectingMethodInterceptorFactory(mappingProvider));

		this.projectionFactory = projectionFactory;
		this.customer = projectionFactory.createProjection(Customer.class, new ByteArrayInputStream(json.getBytes()));
	}

	@Test // DATCMNS-885
	void accessSimpleProperty() {
		assertThat(this.customer.getFirstname()).isEqualTo("Dave");
	}

	@Test // DATCMNS-885
	void accessPropertyWithExplicitAnnotation() {
		assertThat(this.customer.getBar()).isEqualTo("Dave");
	}

	@Test // DATCMNS-885
	void accessPropertyWithComplexReturnType() {
		assertThat(this.customer.getAddress()).isEqualTo(new Address("01097", "Dresden"));
	}

	@Test // DATCMNS-885
	void accessComplexPropertyWithProjection() {
		assertThat(this.customer.getAddressProjection().getCity()).isEqualTo("Dresden");
	}

	@Test // DATCMNS-885
	void accessPropertyWithNestedJsonPath() {
		assertThat(this.customer.getNestedZipCode()).isEqualTo("01097");
	}

	@Test // DATCMNS-885
	void accessCollectionProperty() {
		assertThat(this.customer.getAddresses().get(0)).isEqualTo(new Address("01097", "Dresden"));
	}

	@Test // DATCMNS-885
	void accessPropertyOnNestedProjection() {
		assertThat(this.customer.getAddressProjections().get(0).getZipCode()).isEqualTo("01097");
	}

	@Test // DATCMNS-885
	void accessPropertyThatUsesJsonPathProjectionInTurn() {
		assertThat(this.customer.getAnotherAddressProjection().getZipCodeButNotCity()).isEqualTo("01097");
	}

	@Test // DATCMNS-885
	void accessCollectionPropertyThatUsesJsonPathProjectionInTurn() {

		List<AnotherAddressProjection> projections = this.customer.getAnotherAddressProjections();

		assertThat(projections).hasSize(1);
		assertThat(projections.get(0).getZipCodeButNotCity()).isEqualTo("01097");
	}

	@Test // DATCMNS-885
	void accessAsCollectionPropertyThatUsesJsonPathProjectionInTurn() {

		Set<AnotherAddressProjection> projections = this.customer.getAnotherAddressProjectionAsCollection();

		assertThat(projections).hasSize(1);
		assertThat(projections.iterator().next().getZipCodeButNotCity()).isEqualTo("01097");
	}

	@Test // DATCMNS-885
	void accessNestedPropertyButStayOnRootLevel() {

		Name name = this.customer.getName();

		assertThat(name).isNotNull();
		assertThat(name.getFirstname()).isEqualTo("Dave");
	}

	@Test // DATACMNS-885
	void accessNestedFields() {

		assertThat(this.customer.getNestedCity()).isEqualTo("Dresden");
		assertThat(this.customer.getNestedCities()).hasSize(2);
	}

	@Test // DATACMNS-1144
	void returnsNullForNonExistantValue() {
		assertThat(this.customer.getName().getLastname()).isNull();
	}

	@Test // DATACMNS-1144
	void triesMultipleDeclaredPathsIfNotAvailable() {
		assertThat(this.customer.getName().getSomeName()).isEqualTo(this.customer.getName().getFirstname());
	}

	interface Customer {

		String getFirstname();

		@JsonPath("$")
		Name getName();

		Address getAddress();

		List<Address> getAddresses();

		@JsonPath("$.addresses")
		List<AddressProjection> getAddressProjections();

		@JsonPath("$.firstname")
		String getBar();

		@JsonPath("$.address")
		AddressProjection getAddressProjection();

		@JsonPath("$.address.zipCode")
		String getNestedZipCode();

		@JsonPath("$.address")
		AnotherAddressProjection getAnotherAddressProjection();

		@JsonPath("$.addresses")
		List<AnotherAddressProjection> getAnotherAddressProjections();

		@JsonPath("$.address")
		Set<AnotherAddressProjection> getAnotherAddressProjectionAsCollection();

		@JsonPath("$..city")
		String getNestedCity();

		@JsonPath("$..city")
		List<String> getNestedCities();
	}

	interface AddressProjection {

		String getZipCode();

		String getCity();
	}

	interface Name {

		@JsonPath("$.firstname")
		String getFirstname();

		// Not available in the payload
		@JsonPath("$.lastname")
		String getLastname();

		// First one not available in the payload
		@JsonPath({ "$.lastname", "$.firstname" })
		String getSomeName();
	}

	interface AnotherAddressProjection {

		@JsonPath("$.zipCode")
		String getZipCodeButNotCity();
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	static class Address {
		private String zipCode, city;
	}
}
