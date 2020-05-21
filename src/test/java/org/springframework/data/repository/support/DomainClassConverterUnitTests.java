/*
 * Copyright 2008-2020 the original author or authors.
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
package org.springframework.data.repository.support;

import java.lang.reflect.Method;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import org.springframework.aop.framework.Advised;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.core.support.DummyRepositoryFactoryBean;
import org.springframework.data.repository.support.DomainClassConverter.ToIdConverter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.bind.annotation.ModelAttribute;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link DomainClassConverter}.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DomainClassConverterUnitTests {

	static final User USER = new User();

	static final TypeDescriptor STRING_TYPE = TypeDescriptor.valueOf(String.class);

	static final TypeDescriptor USER_TYPE = TypeDescriptor.valueOf(User.class);

	static final TypeDescriptor SUB_USER_TYPE = TypeDescriptor.valueOf(SubUser.class);

	static final TypeDescriptor LONG_TYPE = TypeDescriptor.valueOf(Long.class);

	@SuppressWarnings("rawtypes")
	DomainClassConverter converter;

	@Mock
	DefaultConversionService service;

	@BeforeEach
	@SuppressWarnings({ "unchecked", "rawtypes" })
	void setUp() {
		this.converter = new DomainClassConverter(this.service);
	}

	@Test
	void matchFailsIfNoDaoAvailable() {
		GenericApplicationContext ctx = new GenericApplicationContext();
		ctx.refresh();
		this.converter.setApplicationContext(ctx);
		assertMatches(false);
	}

	@Test
	void matchesIfConversionInBetweenIsPossible() {
		this.converter.setApplicationContext(initContextWithRepo());
		when(this.service.canConvert(String.class, Long.class)).thenReturn(true);
		assertMatches(true);
	}

	@Test
	void matchFailsIfNoIntermediateConversionIsPossible() {
		this.converter.setApplicationContext(initContextWithRepo());
		when(this.service.canConvert(String.class, Long.class)).thenReturn(false);
		assertMatches(false);
	}

	// DATACMNS-233
	void returnsNullForNullSource() {
		assertThat(this.converter.convert(null, STRING_TYPE, USER_TYPE)).isNull();
	}

	// DATACMNS-233
	void returnsNullForEmptyStringSource() {
		assertThat(this.converter.convert("", STRING_TYPE, USER_TYPE)).isNull();
	}

	private void assertMatches(boolean matchExpected) {
		assertThat(this.converter.matches(STRING_TYPE, USER_TYPE)).isEqualTo(matchExpected);
	}

	@Test
	void convertsStringToUserCorrectly() throws Exception {
		ApplicationContext context = initContextWithRepo();
		this.converter.setApplicationContext(context);
		doReturn(1L).when(this.service).convert(any(), eq(Long.class));
		this.converter.convert("1", STRING_TYPE, USER_TYPE);
		UserRepository bean = context.getBean(UserRepository.class);
		UserRepository repo = (UserRepository) ((Advised) bean).getTargetSource().getTarget();
		verify(repo, times(1)).findById(1L);
	}

	@Test // DATACMNS-133
	void discoversFactoryAndRepoFromParentApplicationContext() {
		ApplicationContext parent = initContextWithRepo();
		GenericApplicationContext context = new GenericApplicationContext(parent);
		context.refresh();
		when(this.service.canConvert(String.class, Long.class)).thenReturn(true);
		this.converter.setApplicationContext(context);
		assertThat(this.converter.matches(STRING_TYPE, USER_TYPE)).isTrue();
	}

	@Test // DATACMNS-583
	void converterDoesntMatchIfTargetTypeIsAssignableFromSource() {
		this.converter.setApplicationContext(initContextWithRepo());
		assertThat(this.converter.matches(SUB_USER_TYPE, USER_TYPE)).isFalse();
		assertThat((User) this.converter.convert(USER, USER_TYPE, USER_TYPE)).isEqualTo(USER);
	}

	@Test // DATACMNS-627
	void supportsConversionFromIdType() {
		this.converter.setApplicationContext(initContextWithRepo());
		assertThat(this.converter.matches(LONG_TYPE, USER_TYPE)).isTrue();
	}

	@Test // DATACMNS-627
	void supportsConversionFromEntityToIdType() {
		this.converter.setApplicationContext(initContextWithRepo());
		assertThat(this.converter.matches(USER_TYPE, LONG_TYPE)).isTrue();
	}

	@Test // DATACMNS-627
	void supportsConversionFromEntityToString() {
		this.converter.setApplicationContext(initContextWithRepo());
		when(this.service.canConvert(Long.class, String.class)).thenReturn(true);
		assertThat(this.converter.matches(USER_TYPE, STRING_TYPE)).isTrue();
	}

	@Test // DATACMNS-683
	void toIdConverterDoesNotMatchIfTargetTypeIsAssignableFromSource() throws NoSuchMethodException {
		this.converter.setApplicationContext(initContextWithRepo());
		@SuppressWarnings("rawtypes")
		Optional<ToIdConverter> toIdConverter = (Optional<ToIdConverter>) ReflectionTestUtils.getField(this.converter,
				"toIdConverter");
		Method method = Wrapper.class.getMethod("foo", User.class);
		TypeDescriptor target = TypeDescriptor.nested(new MethodParameter(method, 0), 0);
		assertThat(toIdConverter).map(it -> it.matches(SUB_USER_TYPE, target)).hasValue(false);
	}

	private ApplicationContext initContextWithRepo() {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(DummyRepositoryFactoryBean.class);
		builder.addConstructorArgValue(UserRepository.class);
		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		factory.registerBeanDefinition("provider", builder.getBeanDefinition());
		GenericApplicationContext ctx = new GenericApplicationContext(factory);
		ctx.refresh();
		return ctx;
	}

	static interface Wrapper {

		void foo(@ModelAttribute User user);

	}

	private static class User {

	}

	private static class SubUser extends User {

	}

	private static interface UserRepository extends CrudRepository<User, Long> {

	}

}
