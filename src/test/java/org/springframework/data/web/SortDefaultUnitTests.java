/*
 * Copyright 2013-2020 the original author or authors.
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

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.web.SortDefault.SortDefaults;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link SortDefault}.
 *
 * @since 1.6
 * @author Oliver Gierke
 * @author Mark Paluch
 */
abstract class SortDefaultUnitTests {

	static final String SORT_0 = "username";
	static final String SORT_1 = "username,asc";
	static final String[] SORT_2 = new String[] { "username,ASC", "lastname,firstname,DESC" };
	static final String SORT_3 = "firstname,lastname";

	static final String[] SORT_FIELDS = new String[] { "firstname", "lastname" };
	static final Direction SORT_DIRECTION = Direction.DESC;

	static final Sort SORT = Sort.by(SORT_DIRECTION, SORT_FIELDS);

	@Test
	void parsesSimpleSortStringCorrectly() {

		assertSortStringParsedInto(Sort.by(Order.asc("username")), SORT_1);
		assertSortStringParsedInto(Sort.by(Order.asc("username"), //
				Order.desc("lastname"), Order.desc("firstname")), SORT_2);
		assertSortStringParsedInto(Sort.by("firstname", "lastname"), SORT_3);
	}

	private static void assertSortStringParsedInto(Sort expected, String... source) {

		SortHandlerMethodArgumentResolver resolver = new SortHandlerMethodArgumentResolver();
		Sort sort = resolver.parseParameterIntoSort(Arrays.asList(source), ",");

		assertThat(sort).isEqualTo(expected);
	}

	@Test
	void supportsSortParameter() {
		assertThat(getResolver().supportsParameter(getParameterOfMethod("supportedMethod"))).isTrue();
	}

	@Test
	void returnsNullForNoDefault() throws Exception {
		assertSupportedAndResolvedTo(getParameterOfMethod("supportedMethod"), Sort.unsorted());
	}

	@Test
	void discoversSimpleDefault() throws Exception {
		assertSupportedAndResolvedTo(getParameterOfMethod("simpleDefault"), Sort.by(SORT_FIELDS).ascending());
	}

	@Test
	void discoversSimpleDefaultWithDirection() throws Exception {
		assertSupportedAndResolvedTo(getParameterOfMethod("simpleDefaultWithDirection"), SORT);
	}

	@Test
	void rejectsNonSortParameter() {

		MethodParameter parameter = TestUtils.getParameterOfMethod(getControllerClass(), "unsupportedMethod",
				String.class);
		assertThat(getResolver().supportsParameter(parameter)).isFalse();
	}

	@Test
	void rejectsDoubleAnnotatedMethod() {

		MethodParameter parameter = getParameterOfMethod("invalid");

		HandlerMethodArgumentResolver resolver = new SortHandlerMethodArgumentResolver();
		assertThat(resolver.supportsParameter(parameter)).isTrue();

		assertThatIllegalArgumentException()
				.isThrownBy(() -> resolver.resolveArgument(parameter, null, TestUtils.getWebRequest(), null)) //
				.withMessageContaining(SortDefault.class.getSimpleName()) //
				.withMessageContaining(SortDefaults.class.getSimpleName()) //
				.withMessageContaining(parameter.toString());
	}

	@Test
	void discoversContaineredDefault() throws Exception {

		MethodParameter parameter = getParameterOfMethod("containeredDefault");
		Sort reference = Sort.by("foo", "bar");

		assertSupportedAndResolvedTo(parameter, reference);
	}

	protected HandlerMethodArgumentResolver getResolver() {
		return new SortHandlerMethodArgumentResolver();
	}

	protected abstract Class<?> getControllerClass();

	private void assertSupportedAndResolvedTo(MethodParameter parameter, Sort sort) throws Exception {

		HandlerMethodArgumentResolver resolver = getResolver();
		assertThat(resolver.supportsParameter(parameter)).isTrue();
		assertThat(resolver.resolveArgument(parameter, null, TestUtils.getWebRequest(), null)).isEqualTo(sort);
	}

	protected MethodParameter getParameterOfMethod(String name) {
		return getParameterOfMethod(getControllerClass(), name);
	}

	private static MethodParameter getParameterOfMethod(Class<?> controller, String name) {
		return TestUtils.getParameterOfMethod(controller, name, Sort.class);
	}

}
