/*
 * Copyright 2014-2020 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.expression.spel.standard.SpelExpressionParser;

/**
 * Unit tests for {@link SpelEvaluatingMethodInterceptor}.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 */
@ExtendWith(MockitoExtension.class)
class SpelEvaluatingMethodInterceptorUnitTests {

	@Mock MethodInterceptor delegate;
	@Mock MethodInvocation invocation;

	SpelExpressionParser parser = new SpelExpressionParser();

	@Test // DATAREST-221, DATACMNS-630
	void invokesMethodOnTarget() throws Throwable {

		when(this.invocation.getMethod()).thenReturn(Projection.class.getMethod("propertyFromTarget"));

		MethodInterceptor interceptor = new SpelEvaluatingMethodInterceptor(this.delegate, new Target(), null, this.parser,
				Projection.class);

		assertThat(interceptor.invoke(this.invocation)).isEqualTo("property");
	}

	@Test // DATAREST-221, DATACMNS-630
	void invokesMethodOnBean() throws Throwable {

		when(this.invocation.getMethod()).thenReturn(Projection.class.getMethod("invokeBean"));

		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		factory.registerSingleton("someBean", new SomeBean());

		SpelEvaluatingMethodInterceptor interceptor = new SpelEvaluatingMethodInterceptor(this.delegate, new Target(), factory,
				this.parser, Projection.class);

		assertThat(interceptor.invoke(this.invocation)).isEqualTo("value");
	}

	@Test // DATACMNS-630
	void forwardNonAtValueAnnotatedMethodToDelegate() throws Throwable {

		when(this.invocation.getMethod()).thenReturn(Projection.class.getMethod("getName"));

		SpelEvaluatingMethodInterceptor interceptor = new SpelEvaluatingMethodInterceptor(this.delegate, new Target(),
				new DefaultListableBeanFactory(), this.parser, Projection.class);

		interceptor.invoke(this.invocation);

		verify(this.delegate).invoke(this.invocation);
	}

	@Test // DATACMNS-630
	void rejectsEmptySpelExpression() {
		assertThatIllegalStateException().isThrownBy(() -> new SpelEvaluatingMethodInterceptor(this.delegate, new Target(),
				new DefaultListableBeanFactory(), this.parser, InvalidProjection.class));
	}

	@Test // DATACMNS-630
	void allowsMapAccessViaPropertyExpression() throws Throwable {

		Map<String, Object> map = new HashMap<>();
		map.put("name", "Dave");

		when(this.invocation.getMethod()).thenReturn(Projection.class.getMethod("propertyFromTarget"));

		SpelEvaluatingMethodInterceptor interceptor = new SpelEvaluatingMethodInterceptor(this.delegate, map,
				new DefaultListableBeanFactory(), this.parser, Projection.class);

		assertThat(interceptor.invoke(this.invocation)).isEqualTo("Dave");
	}

	@Test // DATACMNS-1150
	void forwardsParameterIntoSpElExpressionEvaluation() throws Throwable {

		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		factory.registerSingleton("someBean", new SomeBean());

		when(this.invocation.getMethod()).thenReturn(Projection.class.getMethod("invokeBeanWithParameter", Integer.class));
		when(this.invocation.getArguments()).thenReturn(new Object[] { 1 });

		MethodInterceptor interceptor = new SpelEvaluatingMethodInterceptor(this.delegate, new Target(), factory, this.parser,
				Projection.class);

		assertThat(interceptor.invoke(this.invocation)).isEqualTo("property1");
	}

	interface Projection {

		@Value("#{target.name}")
		String propertyFromTarget();

		@Value("#{@someBean.value}")
		String invokeBean();

		String getName();

		String getAddress();

		@Value("#{@someBean.someMethod(target, args[0])}")
		String invokeBeanWithParameter(Integer parameter);
	}

	interface InvalidProjection {

		@Value("")
		String getAddress();
	}

	static class Target {

		public String getName() {
			return "property";
		}
	}

	static class SomeBean {

		public String getValue() {
			return "value";
		}

		public String someMethod(Target target, Integer parameter) {
			return target.getName() + parameter.toString();
		}
	}
}
