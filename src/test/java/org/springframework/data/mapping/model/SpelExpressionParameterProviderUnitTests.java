/*
 * Copyright 2012-2020 the original author or authors.
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
package org.springframework.data.mapping.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import org.springframework.core.convert.ConversionService;
import org.springframework.data.mapping.PreferredConstructor.Parameter;
import org.springframework.data.mapping.model.AbstractPersistentPropertyUnitTests.SamplePersistentProperty;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link SpELExpressionParameterValueProvider}.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SpelExpressionParameterProviderUnitTests {

	@Mock
	SpELExpressionEvaluator evaluator;

	@Mock
	ParameterValueProvider<SamplePersistentProperty> delegate;

	@Mock
	ConversionService conversionService;

	private SpELExpressionParameterValueProvider<SamplePersistentProperty> provider;

	private Parameter<Object, SamplePersistentProperty> parameter;

	@BeforeEach
	@SuppressWarnings("unchecked")
	void setUp() {
		this.provider = new SpELExpressionParameterValueProvider<>(this.evaluator, this.conversionService,
				this.delegate);
		this.parameter = mock(Parameter.class);
		given(this.parameter.hasSpelExpression()).willReturn(true);
		given(this.parameter.getRawType()).willReturn(Object.class);
	}

	@Test
	@SuppressWarnings("unchecked")
	void delegatesIfParameterDoesNotHaveASpELExpression() {
		Parameter<Object, SamplePersistentProperty> parameter = mock(Parameter.class);
		given(parameter.hasSpelExpression()).willReturn(false);
		this.provider.getParameterValue(parameter);
		verify(this.delegate, times(1)).getParameterValue(parameter);
		verify(this.evaluator, times(0)).evaluate("expression");
	}

	@Test
	void evaluatesSpELExpression() {
		given(this.parameter.getSpelExpression()).willReturn("expression");
		this.provider.getParameterValue(this.parameter);
		verify(this.delegate, times(0)).getParameterValue(this.parameter);
		verify(this.evaluator, times(1)).evaluate("expression");
	}

	@Test
	void handsSpELValueToConversionService() {
		willReturn("source").given(this.parameter).getSpelExpression();
		willReturn("value").given(this.evaluator).evaluate(any());
		this.provider.getParameterValue(this.parameter);
		verify(this.delegate, times(0)).getParameterValue(this.parameter);
		verify(this.conversionService, times(1)).convert("value", Object.class);
	}

	@Test
	void doesNotConvertNullValue() {
		willReturn("source").given(this.parameter).getSpelExpression();
		willReturn(null).given(this.evaluator).evaluate(any());
		this.provider.getParameterValue(this.parameter);
		verify(this.delegate, times(0)).getParameterValue(this.parameter);
		verify(this.conversionService, times(0)).convert("value", Object.class);
	}

	@Test
	void returnsMassagedObjectOnOverride() {
		this.provider = new SpELExpressionParameterValueProvider<SamplePersistentProperty>(this.evaluator,
				this.conversionService, this.delegate) {

			@Override
			@SuppressWarnings("unchecked")
			protected <T> T potentiallyConvertSpelValue(Object object,
					Parameter<T, SamplePersistentProperty> parameter) {
				return (T) "FOO";
			}

		};
		willReturn("source").given(this.parameter).getSpelExpression();
		willReturn("value").given(this.evaluator).evaluate(any());
		assertThat(this.provider.getParameterValue(this.parameter)).isEqualTo("FOO");
		verify(this.delegate, times(0)).getParameterValue(this.parameter);
	}

}
