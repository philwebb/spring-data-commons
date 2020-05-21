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
package org.springframework.data.domain;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.data.domain.ExampleMatcher.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit test for {@link ExampleMatcher}.
 *
 * @author Mark Paluch
 * @author Oliver Gierke
 */
class ExampleMatcherUnitTests {

	ExampleMatcher matcher;

	@BeforeEach
	void setUp() {
		this.matcher = matching();
	}

	@Test // DATACMNS-810
	void defaultStringMatcherShouldReturnDefault() {
		assertThat(this.matcher.getDefaultStringMatcher()).isEqualTo(StringMatcher.DEFAULT);
	}

	@Test // DATACMNS-810
	void ignoreCaseShouldReturnFalseByDefault() {
		assertThat(this.matcher.isIgnoreCaseEnabled()).isFalse();
	}

	@Test // DATACMNS-810
	void ignoredPathsIsEmptyByDefault() {
		assertThat(this.matcher.getIgnoredPaths()).isEmpty();
	}

	@Test // DATACMNS-810
	void nullHandlerShouldReturnIgnoreByDefault() {
		assertThat(this.matcher.getNullHandler()).isEqualTo(NullHandler.IGNORE);
	}

	@Test // DATACMNS-810
	void ignoredPathsIsNotModifiable() {
		assertThatExceptionOfType(UnsupportedOperationException.class)
				.isThrownBy(() -> this.matcher.getIgnoredPaths().add("¯\\_(ツ)_/¯"));
	}

	@Test // DATACMNS-810
	void ignoreCaseShouldReturnTrueWhenIgnoreCaseEnabled() {

		this.matcher = matching().withIgnoreCase();

		assertThat(this.matcher.isIgnoreCaseEnabled()).isTrue();
	}

	@Test // DATACMNS-810
	void ignoreCaseShouldReturnTrueWhenIgnoreCaseSet() {

		this.matcher = matching().withIgnoreCase(true);

		assertThat(this.matcher.isIgnoreCaseEnabled()).isTrue();
	}

	@Test // DATACMNS-810
	void nullHandlerShouldReturnInclude() {

		this.matcher = matching().withIncludeNullValues();

		assertThat(this.matcher.getNullHandler()).isEqualTo(NullHandler.INCLUDE);
	}

	@Test // DATACMNS-810
	void nullHandlerShouldReturnIgnore() {

		this.matcher = matching().withIgnoreNullValues();

		assertThat(this.matcher.getNullHandler()).isEqualTo(NullHandler.IGNORE);
	}

	@Test // DATACMNS-810
	void nullHandlerShouldReturnConfiguredValue() {

		this.matcher = matching().withNullHandler(NullHandler.INCLUDE);

		assertThat(this.matcher.getNullHandler()).isEqualTo(NullHandler.INCLUDE);
	}

	@Test // DATACMNS-810
	void ignoredPathsShouldReturnCorrectProperties() {

		this.matcher = matching().withIgnorePaths("foo", "bar", "baz");

		assertThat(this.matcher.getIgnoredPaths()).contains("foo", "bar", "baz");
		assertThat(this.matcher.getIgnoredPaths()).hasSize(3);
	}

	@Test // DATACMNS-810
	void ignoredPathsShouldReturnUniqueProperties() {

		this.matcher = matching().withIgnorePaths("foo", "bar", "foo");

		assertThat(this.matcher.getIgnoredPaths()).contains("foo", "bar");
		assertThat(this.matcher.getIgnoredPaths()).hasSize(2);
	}

	@Test // DATACMNS-810
	void withCreatesNewInstance() {

		this.matcher = matching().withIgnorePaths("foo", "bar", "foo");
		ExampleMatcher configuredExampleSpec = this.matcher.withIgnoreCase();

		assertThat(this.matcher).isNotSameAs(configuredExampleSpec);
		assertThat(this.matcher.getIgnoredPaths()).hasSize(2);
		assertThat(this.matcher.isIgnoreCaseEnabled()).isFalse();

		assertThat(configuredExampleSpec.getIgnoredPaths()).hasSize(2);
		assertThat(configuredExampleSpec.isIgnoreCaseEnabled()).isTrue();
	}

	@Test // DATACMNS-879
	void defaultMatcherRequiresAllMatching() {

		assertThat(matching().isAllMatching()).isTrue();
		assertThat(matching().isAnyMatching()).isFalse();
	}

	@Test // DATACMNS-879
	void allMatcherRequiresAllMatching() {

		assertThat(matchingAll().isAllMatching()).isTrue();
		assertThat(matchingAll().isAnyMatching()).isFalse();
	}

	@Test // DATACMNS-879
	void anyMatcherYieldsAnyMatching() {

		assertThat(matchingAny().isAnyMatching()).isTrue();
		assertThat(matchingAny().isAllMatching()).isFalse();
	}

	@Test // DATACMNS-900
	void shouldCompareUsingHashCodeAndEquals() {

		this.matcher = matching() //
				.withIgnorePaths("foo", "bar", "baz") //
				.withNullHandler(NullHandler.IGNORE) //
				.withIgnoreCase("ignored-case") //
				.withMatcher("hello", GenericPropertyMatchers.contains().caseSensitive()) //
				.withMatcher("world", GenericPropertyMatcher::endsWith);

		ExampleMatcher sameAsMatcher = matching() //
				.withIgnorePaths("foo", "bar", "baz") //
				.withNullHandler(NullHandler.IGNORE) //
				.withIgnoreCase("ignored-case") //
				.withMatcher("hello", GenericPropertyMatchers.contains().caseSensitive()) //
				.withMatcher("world", GenericPropertyMatcher::endsWith);

		ExampleMatcher different = matching() //
				.withIgnorePaths("foo", "bar", "baz") //
				.withNullHandler(NullHandler.IGNORE) //
				.withMatcher("hello", GenericPropertyMatchers.contains().ignoreCase());

		assertThat(this.matcher.hashCode()).isEqualTo(sameAsMatcher.hashCode()).isNotEqualTo(different.hashCode());
		assertThat(this.matcher).isEqualTo(sameAsMatcher).isNotEqualTo(different);
	}

	static class Person {

		String firstname;

	}

}
