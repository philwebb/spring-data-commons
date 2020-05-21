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

package org.springframework.data.projection;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;

/**
 * Unit tests for {@link MapAccessingMethodInterceptor}.
 *
 * @author Oliver Gierke
 */
@ExtendWith(MockitoExtension.class)
class MapAccessingMethodInterceptorUnitTests {

	@Mock
	MethodInvocation invocation;

	@Test // DATACMNS-630
	void rejectsNullMap() {
		assertThatIllegalArgumentException().isThrownBy(() -> new MapAccessingMethodInterceptor(null));
	}

	@Test // DATACMNS-630
	void forwardsObjectMethodsToBackingMap() throws Throwable {
		Map<String, Object> map = Collections.emptyMap();
		given(this.invocation.proceed()).willReturn(map.toString());
		given(this.invocation.getMethod()).willReturn(Object.class.getMethod("toString"));
		MapAccessingMethodInterceptor interceptor = new MapAccessingMethodInterceptor(map);
		Object result = interceptor.invoke(this.invocation);
		assertThat(result).isEqualTo(map.toString());
	}

	@Test // DATACMNS-630
	void setterInvocationStoresValueInMap() throws Throwable {
		Map<String, Object> map = new HashMap<>();
		given(this.invocation.getMethod()).willReturn(Sample.class.getMethod("setName", String.class));
		given(this.invocation.getArguments()).willReturn(new Object[] { "Foo" });
		Object result = new MapAccessingMethodInterceptor(map).invoke(this.invocation);
		assertThat(result).isNull();
		assertThat(map.get("name")).isEqualTo("Foo");
	}

	@Test // DATACMNS-630
	void getterInvocationReturnsValueFromMap() throws Throwable {
		Map<String, Object> map = new HashMap<>();
		map.put("name", "Foo");
		given(this.invocation.getMethod()).willReturn(Sample.class.getMethod("getName"));
		Object result = new MapAccessingMethodInterceptor(map).invoke(this.invocation);
		assertThat(result).isEqualTo("Foo");
	}

	@Test // DATACMNS-630
	void getterReturnsNullIfMapDoesNotContainValue() throws Throwable {
		Map<String, Object> map = new HashMap<>();
		given(this.invocation.getMethod()).willReturn(Sample.class.getMethod("getName"));
		assertThat(new MapAccessingMethodInterceptor(map).invoke(this.invocation)).isNull();
	}

	@Test // DATACMNS-630
	void rejectsNonAccessorInvocation() throws Throwable {
		given(this.invocation.getMethod()).willReturn(Sample.class.getMethod("someMethod"));
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new MapAccessingMethodInterceptor(Collections.emptyMap()).invoke(this.invocation));
	}

	interface Sample {

		String getName();

		void setName(String name);

		void someMethod();

	}

}
