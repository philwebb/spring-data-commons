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
package org.springframework.data.repository.support;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.support.RepositoryFactoryInformation;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.util.ProxyUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Wrapper class to access repository instances obtained from a
 * {@link ListableBeanFactory}.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Thomas Eizinger
 * @author Christoph Strobl
 */
public class Repositories implements Iterable<Class<?>> {

	static final Repositories NONE = new Repositories();

	private static final RepositoryFactoryInformation<Object, Object> EMPTY_REPOSITORY_FACTORY_INFO = EmptyRepositoryFactoryInformation.INSTANCE;

	private static final String DOMAIN_TYPE_MUST_NOT_BE_NULL = "Domain type must not be null!";

	private final Optional<BeanFactory> beanFactory;

	private final Map<Class<?>, String> repositoryBeanNames;

	private final Map<Class<?>, RepositoryFactoryInformation<Object, Object>> repositoryFactoryInfos;

	/**
	 * Constructor to create the {@link #NONE} instance.
	 */
	private Repositories() {
		this.beanFactory = Optional.empty();
		this.repositoryBeanNames = Collections.emptyMap();
		this.repositoryFactoryInfos = Collections.emptyMap();
	}

	/**
	 * Creates a new {@link Repositories} instance by looking up the repository instances
	 * and meta information from the given {@link ListableBeanFactory}.
	 * @param factory must not be {@literal null}.
	 */
	public Repositories(ListableBeanFactory factory) {
		Assert.notNull(factory, "ListableBeanFactory must not be null!");
		this.beanFactory = Optional.of(factory);
		this.repositoryFactoryInfos = new HashMap<>();
		this.repositoryBeanNames = new HashMap<>();

		populateRepositoryFactoryInformation(factory);
	}

	private void populateRepositoryFactoryInformation(ListableBeanFactory factory) {
		for (String name : BeanFactoryUtils.beanNamesForTypeIncludingAncestors(factory,
				RepositoryFactoryInformation.class, false, false)) {
			cacheRepositoryFactory(name);
		}
	}

	@SuppressWarnings("rawtypes")
	private synchronized void cacheRepositoryFactory(String name) {
		RepositoryFactoryInformation repositoryFactoryInformation = this.beanFactory.get().getBean(name,
				RepositoryFactoryInformation.class);
		Class<?> domainType = ClassUtils
				.getUserClass(repositoryFactoryInformation.getRepositoryInformation().getDomainType());
		RepositoryInformation information = repositoryFactoryInformation.getRepositoryInformation();
		Set<Class<?>> alternativeDomainTypes = information.getAlternativeDomainTypes();
		Set<Class<?>> typesToRegister = new HashSet<>(alternativeDomainTypes.size() + 1);
		typesToRegister.add(domainType);
		typesToRegister.addAll(alternativeDomainTypes);
		for (Class<?> type : typesToRegister) {
			cacheFirstOrPrimary(type, repositoryFactoryInformation, BeanFactoryUtils.transformedBeanName(name));
		}
	}

	/**
	 * Returns whether we have a repository instance registered to manage instances of the
	 * given domain class.
	 * @param domainClass must not be {@literal null}.
	 * @return
	 */
	public boolean hasRepositoryFor(Class<?> domainClass) {
		Assert.notNull(domainClass, DOMAIN_TYPE_MUST_NOT_BE_NULL);
		Class<?> userClass = ProxyUtils.getUserClass(domainClass);
		return this.repositoryFactoryInfos.containsKey(userClass);
	}

	/**
	 * Returns the repository managing the given domain class.
	 * @param domainClass must not be {@literal null}.
	 * @return
	 */
	public Optional<Object> getRepositoryFor(Class<?> domainClass) {
		Assert.notNull(domainClass, DOMAIN_TYPE_MUST_NOT_BE_NULL);
		Class<?> userClass = ProxyUtils.getUserClass(domainClass);
		Optional<String> repositoryBeanName = Optional.ofNullable(this.repositoryBeanNames.get(userClass));
		return this.beanFactory.flatMap(it -> repositoryBeanName.map(it::getBean));
	}

	/**
	 * Returns the {@link RepositoryFactoryInformation} for the given domain class. The
	 * given <code>code</code> is converted to the actual user class if necessary, @see
	 * ProxyUtils#getUserClass.
	 * @param domainClass must not be {@literal null}.
	 * @return the {@link RepositoryFactoryInformation} for the given domain class or
	 * {@literal null} if no repository registered for this domain class.
	 */
	private RepositoryFactoryInformation<Object, Object> getRepositoryFactoryInfoFor(Class<?> domainClass) {
		Assert.notNull(domainClass, DOMAIN_TYPE_MUST_NOT_BE_NULL);
		Class<?> userType = ProxyUtils.getUserClass(domainClass);
		RepositoryFactoryInformation<Object, Object> repositoryInfo = this.repositoryFactoryInfos.get(userType);
		if (repositoryInfo != null) {
			return repositoryInfo;
		}
		if (!userType.equals(Object.class)) {
			return getRepositoryFactoryInfoFor(userType.getSuperclass());
		}
		return EMPTY_REPOSITORY_FACTORY_INFO;
	}

	/**
	 * Returns the {@link EntityInformation} for the given domain class.
	 * @param domainClass must not be {@literal null}.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T, S> EntityInformation<T, S> getEntityInformationFor(Class<?> domainClass) {
		Assert.notNull(domainClass, DOMAIN_TYPE_MUST_NOT_BE_NULL);
		return (EntityInformation<T, S>) getRepositoryFactoryInfoFor(domainClass).getEntityInformation();
	}

	/**
	 * Returns the {@link RepositoryInformation} for the given domain class.
	 * @param domainClass must not be {@literal null}.
	 * @return the {@link RepositoryInformation} for the given domain class or
	 * {@literal Optional#empty()} if no repository registered for this domain class.
	 */
	public Optional<RepositoryInformation> getRepositoryInformationFor(Class<?> domainClass) {
		Assert.notNull(domainClass, DOMAIN_TYPE_MUST_NOT_BE_NULL);
		RepositoryFactoryInformation<Object, Object> information = getRepositoryFactoryInfoFor(domainClass);
		return information == EMPTY_REPOSITORY_FACTORY_INFO ? Optional.empty()
				: Optional.of(information.getRepositoryInformation());
	}

	/**
	 * Returns the {@link RepositoryInformation} for the given domain type.
	 * @param domainType must not be {@literal null}.
	 * @return the {@link RepositoryInformation} for the given domain type.
	 * @throws IllegalArgumentException in case no {@link RepositoryInformation} could be
	 * found for the given domain type.
	 */
	public RepositoryInformation getRequiredRepositoryInformation(Class<?> domainType) {
		return getRepositoryInformationFor(domainType).orElseThrow(() -> new IllegalArgumentException(
				"No required RepositoryInformation found for domain type " + domainType.getName() + "!"));
	}

	/**
	 * Returns the {@link RepositoryInformation} for the given repository interface.
	 * @param repositoryInterface must not be {@literal null}.
	 * @return the {@link RepositoryInformation} for the given repository interface or
	 * {@literal null} there's no repository instance registered for the given interface.
	 * @since 1.12
	 */
	public Optional<RepositoryInformation> getRepositoryInformation(Class<?> repositoryInterface) {
		return this.repositoryFactoryInfos.values().stream().map(RepositoryFactoryInformation::getRepositoryInformation)
				.filter(information -> information.getRepositoryInterface().equals(repositoryInterface)).findFirst();
	}

	/**
	 * Returns the {@link PersistentEntity} for the given domain class. Might return
	 * {@literal null} in case the module storing the given domain class does not support
	 * the mapping subsystem.
	 * @param domainClass must not be {@literal null}.
	 * @return the {@link PersistentEntity} for the given domain class or {@literal null}
	 * if no repository is registered for the domain class or the repository is not backed
	 * by a {@link MappingContext} implementation.
	 */
	public PersistentEntity<?, ?> getPersistentEntity(Class<?> domainClass) {
		Assert.notNull(domainClass, DOMAIN_TYPE_MUST_NOT_BE_NULL);
		return getRepositoryFactoryInfoFor(domainClass).getPersistentEntity();
	}

	/**
	 * Returns the {@link QueryMethod}s contained in the repository managing the given
	 * domain class.
	 * @param domainClass must not be {@literal null}.
	 * @return
	 */
	public List<QueryMethod> getQueryMethodsFor(Class<?> domainClass) {
		Assert.notNull(domainClass, DOMAIN_TYPE_MUST_NOT_BE_NULL);
		return getRepositoryFactoryInfoFor(domainClass).getQueryMethods();
	}

	@Override
	public Iterator<Class<?>> iterator() {
		return this.repositoryFactoryInfos.keySet().iterator();
	}

	/**
	 * Caches the repository information for the given domain type or overrides existing
	 * information in case the bean name points to a primary bean definition.
	 * @param type must not be {@literal null}.
	 * @param information must not be {@literal null}.
	 * @param name must not be {@literal null}.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void cacheFirstOrPrimary(Class<?> type, RepositoryFactoryInformation information, String name) {
		if (this.repositoryBeanNames.containsKey(type)) {
			Boolean presentAndPrimary = this.beanFactory.filter(ConfigurableListableBeanFactory.class::isInstance)
					.map(ConfigurableListableBeanFactory.class::cast).map(it -> it.getBeanDefinition(name))
					.map(BeanDefinition::isPrimary).orElse(false);
			if (!presentAndPrimary) {
				return;
			}
		}
		this.repositoryFactoryInfos.put(type, information);
		this.repositoryBeanNames.put(type, name);
	}

	/**
	 * Null-object to avoid nasty {@literal null} checks in cache lookups.
	 */
	private static enum EmptyRepositoryFactoryInformation implements RepositoryFactoryInformation<Object, Object> {

		INSTANCE;

		@Override
		public EntityInformation<Object, Object> getEntityInformation() {
			throw new UnsupportedOperationException();
		}

		@Override
		public RepositoryInformation getRepositoryInformation() {
			throw new UnsupportedOperationException();
		}

		@Override
		public PersistentEntity<?, ?> getPersistentEntity() {
			throw new UnsupportedOperationException();
		}

		@Override
		public List<QueryMethod> getQueryMethods() {
			return Collections.emptyList();
		}

	}

}
