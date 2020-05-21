/*
 * Copyright 2018-2020 the original author or authors.
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
package org.springframework.data.util;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.data.querydsl.EntityPathResolver;
import org.springframework.data.querydsl.SimpleEntityPathResolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.doReturn;

/**
 * Unit tests for {@link BeanLookup}.
 *
 * @author Oliver Gierke
 */
@ExtendWith(MockitoExtension.class)
public class BeanLookupUnitTests {

	@Mock
	ListableBeanFactory beanFactory;

	Map<String, EntityPathResolver> beans;

	@BeforeEach
	public void setUp() {
		this.beans = new HashMap<>();
		doReturn(this.beans).when(this.beanFactory).getBeansOfType(EntityPathResolver.class, false, false);
	}

	@Test // DATACMNS-1235
	public void returnsUniqueBeanByType() {
		this.beans.put("foo", SimpleEntityPathResolver.INSTANCE);
		assertThat(BeanLookup.lazyIfAvailable(EntityPathResolver.class, this.beanFactory).get())
				.isEqualTo(SimpleEntityPathResolver.INSTANCE);
	}

	@Test // DATACMNS-1235
	public void returnsEmptyLazyIfNoBeanAvailable() {
		assertThat(BeanLookup.lazyIfAvailable(EntityPathResolver.class, this.beanFactory).getOptional()).isEmpty();
	}

	@Test // DATACMNS-1235
	public void throwsExceptionIfMultipleBeansAreAvailable() {
		this.beans.put("foo", SimpleEntityPathResolver.INSTANCE);
		this.beans.put("bar", SimpleEntityPathResolver.INSTANCE);
		assertThatExceptionOfType(NoUniqueBeanDefinitionException.class)
				.isThrownBy(() -> BeanLookup.lazyIfAvailable(EntityPathResolver.class, this.beanFactory).get())
				.withMessageContaining("foo").withMessageContaining("bar")
				.withMessageContaining(EntityPathResolver.class.getName());
	}

}
