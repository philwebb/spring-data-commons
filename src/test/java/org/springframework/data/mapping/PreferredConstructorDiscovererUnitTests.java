/*
 * Copyright 2011-2020 the original author or authors.
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

package org.springframework.data.mapping;

import java.util.Iterator;

import org.junit.jupiter.api.Test;

import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.mapping.PreferredConstructor.Parameter;
import org.springframework.data.mapping.PreferredConstructorDiscovererUnitTests.Outer.Inner;
import org.springframework.data.mapping.model.BasicPersistentEntity;
import org.springframework.data.mapping.model.PreferredConstructorDiscoverer;
import org.springframework.data.util.ClassTypeInformation;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link PreferredConstructorDiscoverer}.
 *
 * @author Oliver Gierke
 * @author Roman Rodov
 * @author Mark Paluch
 */
public class PreferredConstructorDiscovererUnitTests<P extends PersistentProperty<P>> {

	@Test // DATACMNS-1126
	void findsNoArgConstructorForClassWithoutExplicitConstructor() {
		assertThat(PreferredConstructorDiscoverer.discover(EntityWithoutConstructor.class)).satisfies((constructor) -> {
			assertThat(constructor).isNotNull();
			assertThat(constructor.isNoArgConstructor()).isTrue();
			assertThat(constructor.isExplicitlyAnnotated()).isFalse();
		});
	}

	@Test // DATACMNS-1126
	void findsNoArgConstructorForClassWithMultipleConstructorsAndNoArgOne() {
		assertThat(PreferredConstructorDiscoverer.discover(ClassWithEmptyConstructor.class))
				.satisfies((constructor) -> {
					assertThat(constructor).isNotNull();
					assertThat(constructor.isNoArgConstructor()).isTrue();
					assertThat(constructor.isExplicitlyAnnotated()).isFalse();
				});
	}

	@Test // DATACMNS-1126
	void doesNotThrowExceptionForMultipleConstructorsAndNoNoArgConstructorWithoutAnnotation() {
		assertThat(PreferredConstructorDiscoverer.discover(ClassWithMultipleConstructorsWithoutEmptyOne.class))
				.isNull();
	}

	@Test // DATACMNS-1126
	@SuppressWarnings({ "unchecked", "rawtypes" })
	void usesConstructorWithAnnotationOverEveryOther() {
		assertThat(PreferredConstructorDiscoverer.discover(ClassWithMultipleConstructorsAndAnnotation.class))
				.satisfies((constructor) -> {
					assertThat(constructor).isNotNull();
					assertThat(constructor.isNoArgConstructor()).isFalse();
					assertThat(constructor.isExplicitlyAnnotated()).isTrue();
					assertThat(constructor.hasParameters()).isTrue();
					Iterator<Parameter<Object, P>> parameters = (Iterator) constructor.getParameters().iterator();
					Parameter<?, P> parameter = parameters.next();
					assertThat(parameter.getType().getType()).isEqualTo(Long.class);
					assertThat(parameters.hasNext()).isFalse();
				});
	}

	@Test // DATACMNS-134, DATACMNS-1126
	void discoversInnerClassConstructorCorrectly() {
		PersistentEntity<Inner, P> entity = new BasicPersistentEntity<>(ClassTypeInformation.from(Inner.class));
		assertThat(PreferredConstructorDiscoverer.discover(entity)).satisfies((constructor) -> {
			Parameter<?, P> parameter = constructor.getParameters().iterator().next();
			assertThat(constructor.isEnclosingClassParameter(parameter)).isTrue();
		});
	}

	@Test // DATACMNS-1082, DATACMNS-1126
	void skipsSyntheticConstructor() {
		PersistentEntity<SyntheticConstructor, P> entity = new BasicPersistentEntity<>(
				ClassTypeInformation.from(SyntheticConstructor.class));
		assertThat(PreferredConstructorDiscoverer.discover(entity)).satisfies((constructor) -> {
			PersistenceConstructor annotation = constructor.getConstructor()
					.getAnnotation(PersistenceConstructor.class);
			assertThat(annotation).isNotNull();
			assertThat(constructor.getConstructor().isSynthetic()).isFalse();
		});
	}

	static final class SyntheticConstructor {

		@PersistenceConstructor
		private SyntheticConstructor(String x) {
		}

		class InnerSynthetic {

			// Compiler will generate a synthetic constructor since
			// SyntheticConstructor() is private.
			InnerSynthetic() {
				new SyntheticConstructor("");
			}

		}

	}

	static class EntityWithoutConstructor {

	}

	static class ClassWithEmptyConstructor {

		ClassWithEmptyConstructor() {
		}

	}

	static class ClassWithMultipleConstructorsAndEmptyOne {

		ClassWithMultipleConstructorsAndEmptyOne(String value) {
		}

		ClassWithMultipleConstructorsAndEmptyOne() {
		}

	}

	static class ClassWithMultipleConstructorsWithoutEmptyOne {

		ClassWithMultipleConstructorsWithoutEmptyOne(String value) {
		}

		ClassWithMultipleConstructorsWithoutEmptyOne(Long value) {
		}

	}

	static class ClassWithMultipleConstructorsAndAnnotation {

		ClassWithMultipleConstructorsAndAnnotation() {
		}

		ClassWithMultipleConstructorsAndAnnotation(String value) {
		}

		@PersistenceConstructor
		ClassWithMultipleConstructorsAndAnnotation(Long value) {
		}

	}

	static class Outer {

		class Inner {

		}

	}

}
