/*
 * Copyright 2015-2020 the original author or authors.
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
package org.springframework.data.querydsl.binding;

import java.util.Optional;

import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.StringPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.querydsl.QSpecialUser;
import org.springframework.data.querydsl.QUser;
import org.springframework.data.querydsl.SimpleEntityPathResolver;
import org.springframework.data.querydsl.User;
import org.springframework.data.util.ClassTypeInformation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link QuerydslBindings}.
 *
 * @author Oliver Gierke
 * @author Christoph Strobl
 */
class QuerydslBindingsUnitTests {

	QuerydslPredicateBuilder builder;

	QuerydslBindings bindings;

	static final SingleValueBinding<StringPath, String> CONTAINS_BINDING = (path, value) -> path.contains(value);

	@BeforeEach
	void setUp() {

		this.builder = new QuerydslPredicateBuilder(new DefaultConversionService(), SimpleEntityPathResolver.INSTANCE);
		this.bindings = new QuerydslBindings();
	}

	@Test // DATACMNS-669
	void rejectsNullPath() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.bindings.getBindingForPath(null));
	}

	@Test // DATACMNS-669
	void returnsEmptyOptionalIfNoBindingRegisteredForPath() {

		PathInformation path = PropertyPathInformation.of("lastname", User.class);

		assertThat(this.bindings.getBindingForPath(path)).isEmpty();
	}

	@Test // DATACMNS-669
	void returnsRegisteredBindingForSimplePath() {

		this.bindings.bind(QUser.user.firstname).first(CONTAINS_BINDING);

		PathInformation path = PropertyPathInformation.of("firstname", User.class);

		assertAdapterWithTargetBinding(this.bindings.getBindingForPath(path), CONTAINS_BINDING);
	}

	@Test // DATACMNS-669
	void getBindingForPathShouldReturnSpeficicBindingForNestedElementsWhenAvailable() {

		this.bindings.bind(QUser.user.address.street).first(CONTAINS_BINDING);

		PathInformation path = PropertyPathInformation.of("address.street", User.class);

		assertAdapterWithTargetBinding(this.bindings.getBindingForPath(path), CONTAINS_BINDING);
	}

	@Test // DATACMNS-669
	void getBindingForPathShouldReturnSpeficicBindingForTypes() {

		this.bindings.bind(String.class).first(CONTAINS_BINDING);

		PathInformation path = PropertyPathInformation.of("address.street", User.class);
		assertAdapterWithTargetBinding(this.bindings.getBindingForPath(path), CONTAINS_BINDING);
	}

	@Test // DATACMNS-669
	void propertyNotExplicitlyIncludedAndWithoutTypeBindingIsNotAvailable() {

		this.bindings.bind(String.class).first(CONTAINS_BINDING);

		PathInformation path = PropertyPathInformation.of("inceptionYear", User.class);

		assertThat(this.bindings.getBindingForPath(path)).isEmpty();
	}

	@Test // DATACMNS-669
	void pathIsAvailableIfTypeBasedBindingWasRegistered() {

		this.bindings.bind(String.class).first(CONTAINS_BINDING);

		assertThat(this.bindings.isPathAvailable("inceptionYear", User.class)).isTrue();
	}

	@Test // DATACMNS-669
	void explicitlyIncludedPathIsAvailable() {

		this.bindings.including(QUser.user.inceptionYear);

		assertThat(this.bindings.isPathAvailable("inceptionYear", User.class)).isTrue();
	}

	@Test // DATACMNS-669
	void notExplicitlyIncludedPathIsNotAvailable() {

		this.bindings.including(QUser.user.inceptionYear);

		assertThat(this.bindings.isPathAvailable("firstname", User.class)).isFalse();
	}

	@Test // DATACMNS-669
	void excludedPathIsNotAvailable() {

		this.bindings.excluding(QUser.user.inceptionYear);

		assertThat(this.bindings.isPathAvailable("inceptionYear", User.class)).isFalse();
	}

	@Test // DATACMNS-669
	void pathIsAvailableIfNotExplicitlyExcluded() {

		this.bindings.excluding(QUser.user.inceptionYear);

		assertThat(this.bindings.isPathAvailable("firstname", User.class)).isTrue();
	}

	@Test // DATACMNS-669
	void pathIsAvailableIfItsBothBlackAndWhitelisted() {

		this.bindings.excluding(QUser.user.firstname);
		this.bindings.including(QUser.user.firstname);

		assertThat(this.bindings.isPathAvailable("firstname", User.class)).isTrue();
	}

	@Test // DATACMNS-669
	void nestedPathIsNotAvailableIfAParanetPathWasExcluded() {

		this.bindings.excluding(QUser.user.address);

		assertThat(this.bindings.isPathAvailable("address.city", User.class)).isFalse();
	}

	@Test // DATACMNS-669
	void pathIsAvailableIfConcretePathIsAvailableButParentExcluded() {

		this.bindings.excluding(QUser.user.address);
		this.bindings.including(QUser.user.address.city);

		assertThat(this.bindings.isPathAvailable("address.city", User.class)).isTrue();
	}

	@Test // DATACMNS-669
	void isPathAvailableShouldReturnFalseWhenPartialPathContainedInExcludingAndConcretePathToDifferentPropertyIsIncluded() {

		this.bindings.excluding(QUser.user.address);
		this.bindings.including(QUser.user.address.city);

		assertThat(this.bindings.isPathAvailable("address.street", User.class)).isFalse();
	}

	@Test // DATACMNS-669
	void whitelistsPropertiesCorrectly() {

		this.bindings.including(QUser.user.firstname, QUser.user.address.street);

		assertThat(this.bindings.isPathAvailable("firstname", User.class)).isTrue();
		assertThat(this.bindings.isPathAvailable("address.street", User.class)).isTrue();
		assertThat(this.bindings.isPathAvailable("lastname", User.class)).isFalse();
		assertThat(this.bindings.isPathAvailable("address.city", User.class)).isFalse();
	}

	@Test // DATACMNS-787
	void rejectsNullAlias() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.bindings.bind(QUser.user.address).as(null));
	}

	@Test // DATACMNS-787
	void rejectsEmptyAlias() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.bindings.bind(QUser.user.address).as(""));
	}

	@Test // DATACMNS-787
	void aliasesBinding() {

		this.bindings.bind(QUser.user.address.city).as("city").first(CONTAINS_BINDING);

		PathInformation path = this.bindings.getPropertyPath("city", ClassTypeInformation.from(User.class));

		assertThat(path).isNotNull();
		assertThat(this.bindings.isPathAvailable("city", User.class)).isTrue();

		// Aliasing implicitly blacklists original path
		assertThat(this.bindings.isPathAvailable("address.city", User.class)).isFalse();
	}

	@Test // DATACMNS-787
	void explicitlyIncludesOriginalBindingDespiteAlias() {

		this.bindings.including(QUser.user.address.city);
		this.bindings.bind(QUser.user.address.city).as("city").first(CONTAINS_BINDING);

		PathInformation path = this.bindings.getPropertyPath("city", ClassTypeInformation.from(User.class));

		assertThat(path).isNotNull();
		assertThat(this.bindings.isPathAvailable("city", User.class)).isTrue();

		assertThat(this.bindings.isPathAvailable("address.city", User.class)).isTrue();

		PathInformation propertyPath = this.bindings.getPropertyPath("address.city",
				ClassTypeInformation.from(User.class));
		assertThat(propertyPath).isNotNull();

		assertAdapterWithTargetBinding(this.bindings.getBindingForPath(propertyPath), CONTAINS_BINDING);
	}

	@Test // DATACMNS-787
	void registedAliasWithNullBinding() {

		this.bindings.bind(QUser.user.address.city).as("city").withDefaultBinding();

		PathInformation path = this.bindings.getPropertyPath("city", ClassTypeInformation.from(User.class));
		assertThat(path).isNotNull();

		assertThat(this.bindings.getBindingForPath(path)).isNotPresent();
	}

	@Test // DATACMNS-941
	void registersBindingForPropertyOfSubtype() {

		this.bindings.bind(QUser.user.as(QSpecialUser.class).specialProperty).first(ContainsBinding.INSTANCE);

		assertThat(this.bindings.isPathAvailable("specialProperty", User.class)).isTrue();
	}

	private static <P extends Path<? extends S>, S> void assertAdapterWithTargetBinding(
			Optional<MultiValueBinding<P, S>> binding, SingleValueBinding<? extends Path<?>, ?> expected) {

		assertThat(binding).hasValueSatisfying(it -> {
			// assertThat(binding,
			// is(instanceOf(QuerydslBindings.MultiValueBindingAdapter.class)));
			// assertThat(ReflectionTestUtils.getField(binding, "delegate"), is((Object)
			// expected));
		});
	}

	enum ContainsBinding implements SingleValueBinding<StringPath, String> {

		INSTANCE;

		@Override
		public Predicate bind(StringPath path, String value) {
			return path.contains(value);
		}

	}

}
