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

package org.springframework.data.web.querydsl;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map.Entry;
import java.util.Optional;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.querydsl.binding.QuerydslBinderCustomizer;
import org.springframework.data.querydsl.binding.QuerydslBindings;
import org.springframework.data.querydsl.binding.QuerydslBindingsFactory;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.data.querydsl.binding.QuerydslPredicateBuilder;
import org.springframework.data.util.CastUtils;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * {@link HandlerMethodArgumentResolver} to allow injection of
 * {@link com.querydsl.core.types.Predicate} into Spring MVC controller methods.
 *
 * @author Christoph Strobl
 * @author Oliver Gierke
 * @since 1.11
 */
public class QuerydslPredicateArgumentResolver implements HandlerMethodArgumentResolver {

	private static final ResolvableType PREDICATE = ResolvableType.forClass(Predicate.class);

	private static final ResolvableType OPTIONAL_OF_PREDICATE = ResolvableType.forClassWithGenerics(Optional.class,
			PREDICATE);

	private final QuerydslBindingsFactory bindingsFactory;

	private final QuerydslPredicateBuilder predicateBuilder;

	/**
	 * Creates a new {@link QuerydslPredicateArgumentResolver} using the given
	 * {@link ConversionService}.
	 * @param factory
	 * @param conversionService defaults to {@link DefaultConversionService} if
	 * {@literal null}.
	 */
	public QuerydslPredicateArgumentResolver(QuerydslBindingsFactory factory,
			Optional<ConversionService> conversionService) {
		this.bindingsFactory = factory;
		this.predicateBuilder = new QuerydslPredicateBuilder(conversionService.orElseGet(DefaultConversionService::new),
				factory.getEntityPathResolver());
	}

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		ResolvableType type = ResolvableType.forMethodParameter(parameter);
		if (PREDICATE.isAssignableFrom(type) || OPTIONAL_OF_PREDICATE.isAssignableFrom(type)) {
			return true;
		}
		if (parameter.hasParameterAnnotation(QuerydslPredicate.class)) {
			throw new IllegalArgumentException(
					String.format("Parameter at position %s must be of type Predicate but was %s.",
							parameter.getParameterIndex(), parameter.getParameterType()));
		}
		return false;
	}

	@Nullable
	@Override
	public Object resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) throws Exception {
		MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
		for (Entry<String, String[]> entry : webRequest.getParameterMap().entrySet()) {
			parameters.put(entry.getKey(), Arrays.asList(entry.getValue()));
		}
		Optional<QuerydslPredicate> annotation = Optional
				.ofNullable(parameter.getParameterAnnotation(QuerydslPredicate.class));
		TypeInformation<?> domainType = extractTypeInfo(parameter).getRequiredActualType();
		Optional<Class<? extends QuerydslBinderCustomizer<?>>> bindingsAnnotation = annotation
				.map(QuerydslPredicate::bindings).map(CastUtils::cast);
		QuerydslBindings bindings = bindingsAnnotation
				.map((it) -> this.bindingsFactory.createBindingsFor(domainType, it))
				.orElseGet(() -> this.bindingsFactory.createBindingsFor(domainType));
		Predicate result = this.predicateBuilder.getPredicate(domainType, parameters, bindings);
		if (!parameter.isOptional() && result == null) {
			return new BooleanBuilder();
		}
		return OPTIONAL_OF_PREDICATE.isAssignableFrom(ResolvableType.forMethodParameter(parameter))
				? Optional.ofNullable(result) : result;
	}

	/**
	 * Obtains the domain type information from the given method parameter. Will favor an
	 * explicitly registered on through {@link QuerydslPredicate#root()} but use the
	 * actual type of the method's return type as fallback.
	 * @param parameter must not be {@literal null}.
	 * @return
	 */
	static TypeInformation<?> extractTypeInfo(MethodParameter parameter) {
		Optional<QuerydslPredicate> annotation = Optional
				.ofNullable(parameter.getParameterAnnotation(QuerydslPredicate.class));
		return annotation.filter((it) -> !Object.class.equals(it.root()))
				.<TypeInformation<?>>map((it) -> ClassTypeInformation.from(it.root()))
				.orElseGet(() -> detectDomainType(parameter));
	}

	private static TypeInformation<?> detectDomainType(MethodParameter parameter) {
		Method method = parameter.getMethod();
		if (method == null) {
			throw new IllegalArgumentException("Method parameter is not backed by a method!");
		}
		return detectDomainType(ClassTypeInformation.fromReturnTypeOf(method));
	}

	private static TypeInformation<?> detectDomainType(TypeInformation<?> source) {
		if (source.getTypeArguments().isEmpty()) {
			return source;
		}
		TypeInformation<?> actualType = source.getActualType();
		if (actualType == null) {
			throw new IllegalArgumentException(String.format("Could not determine domain type from %s!", source));
		}
		if (source != actualType) {
			return detectDomainType(actualType);
		}
		if (source instanceof Iterable) {
			return source;
		}
		return detectDomainType(source.getRequiredComponentType());
	}

}
