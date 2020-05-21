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
package org.springframework.data.repository.core.support;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.data.domain.ExampleMatcher.GenericPropertyMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.ExampleMatcher.GenericPropertyMatcher;
import org.springframework.data.domain.ExampleMatcher.NoOpPropertyValueTransformer;
import org.springframework.data.domain.ExampleMatcher.NullHandler;
import org.springframework.data.domain.ExampleMatcher.PropertyValueTransformer;
import org.springframework.data.domain.ExampleMatcher.StringMatcher;
import org.springframework.data.support.ExampleMatcherAccessor;

/**
 * Unit tests for {@link ExampleMatcherAccessor}.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 */
class ExampleSpecificationAccessorUnitTests {

	Person person;

	ExampleMatcher specification;

	ExampleMatcherAccessor exampleSpecificationAccessor;

	@BeforeEach
	void setUp() {

		this.person = new Person();
		this.person.firstname = "rand";

		this.specification = ExampleMatcher.matching();
		this.exampleSpecificationAccessor = new ExampleMatcherAccessor(this.specification);
	}

	@Test // DATACMNS-810
	void defaultStringMatcherShouldReturnDefault() {
		assertThat(this.exampleSpecificationAccessor.getDefaultStringMatcher()).isEqualTo(StringMatcher.DEFAULT);
	}

	@Test // DATACMNS-810
	void nullHandlerShouldReturnInclude() {

		this.specification = ExampleMatcher.matching().//
				withIncludeNullValues();
		this.exampleSpecificationAccessor = new ExampleMatcherAccessor(this.specification);

		assertThat(this.exampleSpecificationAccessor.getNullHandler()).isEqualTo(NullHandler.INCLUDE);
	}

	@Test // DATACMNS-810
	void exampleShouldIgnorePaths() {

		this.specification = ExampleMatcher.matching().withIgnorePaths("firstname");
		this.exampleSpecificationAccessor = new ExampleMatcherAccessor(this.specification);

		assertThat(this.exampleSpecificationAccessor.isIgnoredPath("firstname")).isTrue();
		assertThat(this.exampleSpecificationAccessor.isIgnoredPath("lastname")).isFalse();
	}

	@Test // DATACMNS-810
	void exampleShouldUseDefaultStringMatcherForPathThatDoesNotHavePropertySpecifier() {
		assertThat(this.exampleSpecificationAccessor.getStringMatcherForPath("firstname"))
				.isEqualTo(this.specification.getDefaultStringMatcher());
	}

	@Test // DATACMNS-810
	void exampleShouldUseConfiguredStringMatcherAsDefaultForPathThatDoesNotHavePropertySpecifier() {

		this.specification = ExampleMatcher.matching().//
				withStringMatcher(StringMatcher.CONTAINING);

		this.exampleSpecificationAccessor = new ExampleMatcherAccessor(this.specification);

		assertThat(this.exampleSpecificationAccessor.getStringMatcherForPath("firstname"))
				.isEqualTo(StringMatcher.CONTAINING);
	}

	@Test // DATACMNS-810
	void exampleShouldUseDefaultIgnoreCaseForPathThatDoesHavePropertySpecifierWithMatcher() {

		this.specification = ExampleMatcher.matching().//
				withIgnoreCase().//
				withMatcher("firstname", contains());

		this.exampleSpecificationAccessor = new ExampleMatcherAccessor(this.specification);

		assertThat(this.exampleSpecificationAccessor.isIgnoreCaseForPath("firstname")).isTrue();
	}

	@Test // DATACMNS-810
	void exampleShouldUseConfiguredIgnoreCaseForPathThatDoesHavePropertySpecifierWithMatcher() {

		this.specification = ExampleMatcher.matching().//
				withIgnoreCase().//
				withMatcher("firstname", contains().caseSensitive());

		this.exampleSpecificationAccessor = new ExampleMatcherAccessor(this.specification);

		assertThat(this.exampleSpecificationAccessor.isIgnoreCaseForPath("firstname")).isFalse();
	}

	@Test // DATACMNS-810
	void exampleShouldUseDefinedStringMatcherForPathThatDoesHavePropertySpecifierWithStringMatcherStarting() {

		this.specification = ExampleMatcher.matching().//
				withMatcher("firstname", startsWith());

		this.exampleSpecificationAccessor = new ExampleMatcherAccessor(this.specification);

		assertThat(this.exampleSpecificationAccessor.getStringMatcherForPath("firstname"))
				.isEqualTo(StringMatcher.STARTING);
	}

	@Test // DATACMNS-810
	void exampleShouldUseDefinedStringMatcherForPathThatDoesHavePropertySpecifierWithStringMatcherContaining() {

		this.specification = ExampleMatcher.matching().//
				withMatcher("firstname", contains());

		this.exampleSpecificationAccessor = new ExampleMatcherAccessor(this.specification);

		assertThat(this.exampleSpecificationAccessor.getStringMatcherForPath("firstname"))
				.isEqualTo(StringMatcher.CONTAINING);
	}

	@Test // DATACMNS-810
	void exampleShouldUseDefinedStringMatcherForPathThatDoesHavePropertySpecifierWithStringMatcherRegex() {

		this.specification = ExampleMatcher.matching().//
				withMatcher("firstname", regex());

		this.exampleSpecificationAccessor = new ExampleMatcherAccessor(this.specification);

		assertThat(this.exampleSpecificationAccessor.getStringMatcherForPath("firstname"))
				.isEqualTo(StringMatcher.REGEX);
	}

	@Test // DATACMNS-810
	void exampleShouldFavorStringMatcherDefinedForPathOverConfiguredDefaultStringMatcher() {

		this.specification = ExampleMatcher.matching().withStringMatcher(StringMatcher.ENDING)
				.withMatcher("firstname", contains()).withMatcher("address.city", startsWith())
				.withMatcher("lastname", GenericPropertyMatcher::ignoreCase);

		this.exampleSpecificationAccessor = new ExampleMatcherAccessor(this.specification);

		assertThat(this.exampleSpecificationAccessor.getPropertySpecifiers()).hasSize(3);
		assertThat(this.exampleSpecificationAccessor.getStringMatcherForPath("firstname"))
				.isEqualTo(StringMatcher.CONTAINING);
		assertThat(this.exampleSpecificationAccessor.getStringMatcherForPath("lastname"))
				.isEqualTo(StringMatcher.ENDING);
		assertThat(this.exampleSpecificationAccessor.getStringMatcherForPath("unknownProperty"))
				.isEqualTo(StringMatcher.ENDING);
	}

	@Test // DATACMNS-810
	void exampleShouldUseDefaultStringMatcherForPathThatHasPropertySpecifierWithoutStringMatcher() {

		this.specification = ExampleMatcher.matching().//
				withStringMatcher(StringMatcher.STARTING).//
				withMatcher("firstname", GenericPropertyMatcher::ignoreCase);

		this.exampleSpecificationAccessor = new ExampleMatcherAccessor(this.specification);

		assertThat(this.exampleSpecificationAccessor.getStringMatcherForPath("firstname"))
				.isEqualTo(StringMatcher.STARTING);
		assertThat(this.exampleSpecificationAccessor.isIgnoreCaseForPath("firstname")).isTrue();
		assertThat(this.exampleSpecificationAccessor.isIgnoreCaseForPath("unknownProperty")).isFalse();
	}

	@Test // DATACMNS-810
	void ignoreCaseShouldReturnFalseByDefault() {

		assertThat(this.specification.isIgnoreCaseEnabled()).isFalse();
		assertThat(this.exampleSpecificationAccessor.isIgnoreCaseForPath("firstname")).isFalse();
	}

	@Test // DATACMNS-810
	void ignoreCaseShouldReturnTrueWhenIgnoreCaseIsEnabled() {

		this.specification = ExampleMatcher.matching().//
				withIgnoreCase();

		this.exampleSpecificationAccessor = new ExampleMatcherAccessor(this.specification);

		assertThat(this.exampleSpecificationAccessor.isIgnoreCaseEnabled()).isTrue();
		assertThat(this.exampleSpecificationAccessor.isIgnoreCaseForPath("firstname")).isTrue();
	}

	@Test // DATACMNS-810
	void ignoreCaseShouldFavorPathSpecificSettings() {

		this.specification = ExampleMatcher.matching().//
				withIgnoreCase("firstname");

		this.exampleSpecificationAccessor = new ExampleMatcherAccessor(this.specification);

		assertThat(this.specification.isIgnoreCaseEnabled()).isFalse();
		assertThat(this.exampleSpecificationAccessor.isIgnoreCaseForPath("firstname")).isTrue();
	}

	@Test // DATACMNS-810
	void getValueTransformerForPathReturnsNoOpValueTransformerByDefault() {
		assertThat(this.exampleSpecificationAccessor.getValueTransformerForPath("firstname"))
				.isInstanceOf(NoOpPropertyValueTransformer.class);
	}

	@Test // DATACMNS-810
	void getValueTransformerForPathReturnsConfigurtedTransformerForPath() {

		PropertyValueTransformer transformer = source -> source.map(Object::toString);

		this.specification = ExampleMatcher.matching().//
				withTransformer("firstname", transformer);
		this.exampleSpecificationAccessor = new ExampleMatcherAccessor(this.specification);

		assertThat(this.exampleSpecificationAccessor.getValueTransformerForPath("firstname")).isEqualTo(transformer);
	}

	@Test // DATACMNS-810
	void hasPropertySpecifiersReturnsFalseIfNoneDefined() {
		assertThat(this.exampleSpecificationAccessor.hasPropertySpecifiers()).isFalse();
	}

	@Test // DATACMNS-810
	void hasPropertySpecifiersReturnsTrueWhenAtLeastOneIsSet() {

		this.specification = ExampleMatcher.matching().//
				withStringMatcher(StringMatcher.STARTING).//
				withMatcher("firstname", contains());

		this.exampleSpecificationAccessor = new ExampleMatcherAccessor(this.specification);

		assertThat(this.exampleSpecificationAccessor.hasPropertySpecifiers()).isTrue();
	}

	@Test // DATACMNS-953
	void exactMatcherUsesExactMatching() {

		ExampleMatcher matcher = ExampleMatcher.matching()//
				.withMatcher("firstname", exact());

		assertThat(new ExampleMatcherAccessor(matcher).getPropertySpecifier("firstname").getStringMatcher())
				.isEqualTo(StringMatcher.EXACT);
	}

	static class Person {

		String firstname;

	}

}
