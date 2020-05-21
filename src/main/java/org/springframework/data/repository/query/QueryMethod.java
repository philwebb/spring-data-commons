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
package org.springframework.data.repository.query;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.stream.Stream;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.EntityMetadata;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.util.QueryExecutionConverters;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.Lazy;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.Assert;

import static org.springframework.data.repository.util.ClassUtils.getNumberOfOccurences;
import static org.springframework.data.repository.util.ClassUtils.hasParameterOfType;

/**
 * Abstraction of a method that is designated to execute a finder query. Enriches the
 * standard {@link Method} interface with specific information that is necessary to
 * construct {@link RepositoryQuery}s for the method.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Christoph Strobl
 * @author Maciek Opała
 * @author Mark Paluch
 */
public class QueryMethod {

	private final RepositoryMetadata metadata;

	private final Method method;

	private final Class<?> unwrappedReturnType;

	private final Parameters<?, ?> parameters;

	private final ResultProcessor resultProcessor;

	private final Lazy<Class<?>> domainClass;

	private final Lazy<Boolean> isCollectionQuery;

	/**
	 * Creates a new {@link QueryMethod} from the given parameters. Looks up the correct
	 * query to use for following invocations of the method given.
	 * @param method must not be {@literal null}.
	 * @param metadata must not be {@literal null}.
	 * @param factory must not be {@literal null}.
	 */
	public QueryMethod(Method method, RepositoryMetadata metadata, ProjectionFactory factory) {
		Assert.notNull(method, "Method must not be null!");
		Assert.notNull(metadata, "Repository metadata must not be null!");
		Assert.notNull(factory, "ProjectionFactory must not be null!");
		Parameters.TYPES.stream()
				.filter(type -> getNumberOfOccurences(method, type) > 1)
				.findFirst().ifPresent(type -> {
					throw new IllegalStateException(
							String.format("Method must only one argument of type %s! Offending method: %s",
									type.getSimpleName(), method.toString()));
				});
		this.method = method;
		this.unwrappedReturnType = potentiallyUnwrapReturnTypeFor(method);
		this.parameters = createParameters(method);
		this.metadata = metadata;
		if (hasParameterOfType(method, Pageable.class)) {
			if (!isStreamQuery()) {
				assertReturnTypeAssignable(method, QueryExecutionConverters.getAllowedPageableTypes());
			}
			if (hasParameterOfType(method, Sort.class)) {
				throw new IllegalStateException(String.format(
						"Method must not have Pageable *and* Sort parameter. "
								+ "Use sorting capabilities on Pageable instead! Offending method: %s",
						method.toString()));
			}
		}
		Assert.notNull(this.parameters,
				() -> String.format("Parameters extracted from method '%s' must not be null!", method.getName()));
		if (isPageQuery()) {
			Assert.isTrue(this.parameters.hasPageableParameter(), String
					.format("Paging query needs to have a Pageable parameter! Offending method %s", method.toString()));
		}
		this.domainClass = Lazy.of(() -> {
			Class<?> repositoryDomainClass = metadata.getDomainType();
			Class<?> methodDomainClass = metadata.getReturnedDomainClass(method);
			return repositoryDomainClass == null || repositoryDomainClass.isAssignableFrom(methodDomainClass)
					? methodDomainClass : repositoryDomainClass;
		});
		this.resultProcessor = new ResultProcessor(this, factory);
		this.isCollectionQuery = Lazy.of(this::calculateIsCollectionQuery);
	}

	/**
	 * Creates a {@link Parameters} instance.
	 * @param method
	 * @return must not return {@literal null}.
	 */
	protected Parameters<?, ?> createParameters(Method method) {
		return new DefaultParameters(method);
	}

	/**
	 * Returns the method's name.
	 * @return
	 */
	public String getName() {
		return this.method.getName();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public EntityMetadata<?> getEntityInformation() {
		return () -> (Class) getDomainClass();
	}

	/**
	 * Returns the name of the named query this method belongs to.
	 * @return
	 */
	public String getNamedQueryName() {
		return String.format("%s.%s", getDomainClass().getSimpleName(), this.method.getName());
	}

	/**
	 * Returns the domain class the query method is targeted at.
	 * @return will never be {@literal null}.
	 */
	protected Class<?> getDomainClass() {
		return this.domainClass.get();
	}

	/**
	 * Returns the type of the object that will be returned.
	 * @return
	 */
	public Class<?> getReturnedObjectType() {
		return this.metadata.getReturnedDomainClass(this.method);
	}

	/**
	 * Returns whether the finder will actually return a collection of entities or a
	 * single one.
	 * @return
	 */
	public boolean isCollectionQuery() {
		return this.isCollectionQuery.get();
	}

	/**
	 * Returns whether the query method will return a {@link Slice}.
	 * @return
	 * @since 1.8
	 */
	public boolean isSliceQuery() {
		return !isPageQuery()
				&& org.springframework.util.ClassUtils.isAssignable(Slice.class, this.unwrappedReturnType);
	}

	/**
	 * Returns whether the finder will return a {@link Page} of results.
	 * @return
	 */
	public final boolean isPageQuery() {
		return org.springframework.util.ClassUtils.isAssignable(Page.class, this.unwrappedReturnType);
	}

	/**
	 * Returns whether the query method is a modifying one.
	 * @return
	 */
	public boolean isModifyingQuery() {
		return false;
	}

	/**
	 * Returns whether the query for this method actually returns entities.
	 * @return
	 */
	public boolean isQueryForEntity() {
		return getDomainClass().isAssignableFrom(getReturnedObjectType());
	}

	/**
	 * Returns whether the method returns a Stream.
	 * @return
	 * @since 1.10
	 */
	public boolean isStreamQuery() {
		return Stream.class.isAssignableFrom(this.unwrappedReturnType);
	}

	/**
	 * Returns the {@link Parameters} wrapper to gain additional information about
	 * {@link Method} parameters.
	 * @return
	 */
	public Parameters<?, ?> getParameters() {
		return this.parameters;
	}

	/**
	 * Returns the {@link ResultProcessor} to be used with the query method.
	 * @return the resultFactory
	 */
	public ResultProcessor getResultProcessor() {
		return this.resultProcessor;
	}

	@Override
	public String toString() {
		return this.method.toString();
	}

	private boolean calculateIsCollectionQuery() {
		if (isPageQuery() || isSliceQuery()) {
			return false;
		}
		Class<?> returnType = this.method.getReturnType();
		if (QueryExecutionConverters.supports(returnType) && !QueryExecutionConverters.isSingleValue(returnType)) {
			return true;
		}
		if (QueryExecutionConverters.supports(this.unwrappedReturnType)) {
			return !QueryExecutionConverters.isSingleValue(this.unwrappedReturnType);
		}
		return ClassTypeInformation.from(this.unwrappedReturnType).isCollectionLike();
	}

	private static Class<? extends Object> potentiallyUnwrapReturnTypeFor(Method method) {
		if (QueryExecutionConverters.supports(method.getReturnType())) {
			// unwrap only one level to handle cases like Future<List<Entity>> correctly.
			TypeInformation<?> componentType = ClassTypeInformation.fromReturnTypeOf(method).getComponentType();
			if (componentType == null) {
				throw new IllegalStateException(
						String.format("Couldn't find component type for return value of method %s!", method));
			}
			return componentType.getType();
		}
		return method.getReturnType();
	}

	private static void assertReturnTypeAssignable(Method method, Set<Class<?>> types) {
		Assert.notNull(method, "Method must not be null!");
		Assert.notEmpty(types, "Types must not be null or empty!");
		TypeInformation<?> returnType = ClassTypeInformation.fromReturnTypeOf(method);
		returnType = QueryExecutionConverters.isSingleValue(returnType.getType()) 
				? returnType.getRequiredComponentType() 
				: returnType;
		for (Class<?> type : types) {
			if (type.isAssignableFrom(returnType.getType())) {
				return;
			}
		}
		throw new IllegalStateException("Method has to have one of the following return types! " + types.toString());
	}

}
