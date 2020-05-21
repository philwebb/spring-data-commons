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

import java.lang.reflect.Method;
import java.lang.reflect.Type;

import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ProjectingJackson2HttpMessageConverter}.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 * @since 1.13
 */
class ProjectingJackson2HttpMessageConverterUnitTests {

	ProjectingJackson2HttpMessageConverter converter = new ProjectingJackson2HttpMessageConverter();

	MediaType ANYTHING_JSON = MediaType.parseMediaType("application/*+json");

	@Test // DATCMNS-885
	void canReadJsonIntoAnnotatedInterface() {
		assertThat(this.converter.canRead(SampleInterface.class, this.ANYTHING_JSON)).isTrue();
	}

	@Test // DATCMNS-885
	void cannotReadUnannotatedInterface() {
		assertThat(this.converter.canRead(UnannotatedInterface.class, this.ANYTHING_JSON)).isFalse();
	}

	@Test // DATCMNS-885
	void cannotReadClass() {
		assertThat(this.converter.canRead(SampleClass.class, this.ANYTHING_JSON)).isFalse();
	}

	@Test // DATACMNS-972
	void doesNotConsiderTypeVariableBoundTo() throws Throwable {
		Method method = BaseController.class.getDeclaredMethod("createEntity", AbstractDto.class);
		Type type = method.getGenericParameterTypes()[0];
		assertThat(this.converter.canRead(type, BaseController.class, this.ANYTHING_JSON)).isFalse();
	}

	@Test // DATACMNS-972
	void genericTypeOnConcreteOne() throws Throwable {
		Method method = ConcreteController.class.getMethod("createEntity", AbstractDto.class);
		Type type = method.getGenericParameterTypes()[0];
		assertThat(this.converter.canRead(type, ConcreteController.class, this.ANYTHING_JSON)).isFalse();
	}

	@ProjectedPayload
	interface SampleInterface {

	}

	interface UnannotatedInterface {

	}

	class SampleClass {

	}

	class AbstractDto {

	}

	abstract class BaseController<D extends AbstractDto> {

		public void createEntity(D dto) {
		}

	}

	class ConcreteController extends BaseController<AbstractDto> {

	}

}
