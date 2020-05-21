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
package org.springframework.data.repository.core.support;

import java.util.List;
import java.util.Optional;

import javax.annotation.Nonnull;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryComposition.RepositoryFragments;
import org.springframework.data.repository.query.ExtensionAwareQueryMethodEvaluationContextProvider;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryLookupStrategy.Key;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.data.util.Lazy;
import org.springframework.util.Assert;

/**
 * Adapter for Springs {@link FactoryBean} interface to allow easy setup of repository
 * factories via Spring configuration.
 *
 * @param <T> the type of the repository
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Mark Paluch
 */
public abstract class RepositoryFactoryBeanSupport<T extends Repository<S, ID>, S, ID>
		implements InitializingBean, RepositoryFactoryInformation<S, ID>, FactoryBean<T>, BeanClassLoaderAware,
		BeanFactoryAware, ApplicationEventPublisherAware {

	private final Class<? extends T> repositoryInterface;

	private RepositoryFactorySupport factory;

	private Key queryLookupStrategyKey;

	private Optional<Class<?>> repositoryBaseClass = Optional.empty();

	private Optional<Object> customImplementation = Optional.empty();

	private Optional<RepositoryFragments> repositoryFragments = Optional.empty();

	private NamedQueries namedQueries;

	private Optional<MappingContext<?, ?>> mappingContext = Optional.empty();

	private ClassLoader classLoader;

	private BeanFactory beanFactory;

	private boolean lazyInit = false;

	private Optional<QueryMethodEvaluationContextProvider> evaluationContextProvider = Optional.empty();

	private ApplicationEventPublisher publisher;

	private Lazy<T> repository;

	private RepositoryMetadata repositoryMetadata;

	/**
	 * Creates a new {@link RepositoryFactoryBeanSupport} for the given repository
	 * interface.
	 * @param repositoryInterface must not be {@literal null}.
	 */
	protected RepositoryFactoryBeanSupport(Class<? extends T> repositoryInterface) {

		Assert.notNull(repositoryInterface, "Repository interface must not be null!");
		this.repositoryInterface = repositoryInterface;
	}

	/**
	 * Configures the repository base class to be used.
	 * @param repositoryBaseClass the repositoryBaseClass to set, can be {@literal null}.
	 * @since 1.11
	 */
	public void setRepositoryBaseClass(Class<?> repositoryBaseClass) {
		this.repositoryBaseClass = Optional.ofNullable(repositoryBaseClass);
	}

	/**
	 * Set the {@link QueryLookupStrategy.Key} to be used.
	 * @param queryLookupStrategyKey
	 */
	public void setQueryLookupStrategyKey(Key queryLookupStrategyKey) {
		this.queryLookupStrategyKey = queryLookupStrategyKey;
	}

	/**
	 * Setter to inject a custom repository implementation.
	 * @param customImplementation
	 */
	public void setCustomImplementation(Object customImplementation) {
		this.customImplementation = Optional.ofNullable(customImplementation);
	}

	/**
	 * Setter to inject repository fragments.
	 * @param repositoryFragments
	 */
	public void setRepositoryFragments(RepositoryFragments repositoryFragments) {
		this.repositoryFragments = Optional.ofNullable(repositoryFragments);
	}

	/**
	 * Setter to inject a {@link NamedQueries} instance.
	 * @param namedQueries the namedQueries to set
	 */
	public void setNamedQueries(NamedQueries namedQueries) {
		this.namedQueries = namedQueries;
	}

	/**
	 * Configures the {@link MappingContext} to be used to lookup {@link PersistentEntity}
	 * instances for {@link #getPersistentEntity()}.
	 * @param mappingContext
	 */
	protected void setMappingContext(MappingContext<?, ?> mappingContext) {
		this.mappingContext = Optional.ofNullable(mappingContext);
	}

	/**
	 * Sets the {@link QueryMethodEvaluationContextProvider} to be used to evaluate SpEL
	 * expressions in manually defined queries.
	 * @param evaluationContextProvider must not be {@literal null}.
	 */
	public void setEvaluationContextProvider(QueryMethodEvaluationContextProvider evaluationContextProvider) {
		this.evaluationContextProvider = Optional.of(evaluationContextProvider);
	}

	/**
	 * Configures whether to initialize the repository proxy lazily. This defaults to
	 * {@literal false}.
	 * @param lazy whether to initialize the repository proxy lazily. This defaults to
	 * {@literal false}.
	 */
	public void setLazyInit(boolean lazy) {
		this.lazyInit = lazy;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {

		this.beanFactory = beanFactory;

		if (!this.evaluationContextProvider.isPresent() && ListableBeanFactory.class.isInstance(beanFactory)) {
			this.evaluationContextProvider = Optional
					.of(new ExtensionAwareQueryMethodEvaluationContextProvider((ListableBeanFactory) beanFactory));
		}
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher publisher) {
		this.publisher = publisher;
	}

	@SuppressWarnings("unchecked")
	public EntityInformation<S, ID> getEntityInformation() {
		return (EntityInformation<S, ID>) this.factory.getEntityInformation(this.repositoryMetadata.getDomainType());
	}

	public RepositoryInformation getRepositoryInformation() {

		RepositoryFragments fragments = this.customImplementation.map(RepositoryFragments::just)//
				.orElse(RepositoryFragments.empty());

		return this.factory.getRepositoryInformation(this.repositoryMetadata, fragments);
	}

	public PersistentEntity<?, ?> getPersistentEntity() {

		return this.mappingContext.orElseThrow(() -> new IllegalStateException("No MappingContext available!"))
				.getRequiredPersistentEntity(this.repositoryMetadata.getDomainType());
	}

	public List<QueryMethod> getQueryMethods() {
		return this.factory.getQueryMethods();
	}

	@Nonnull
	public T getObject() {
		return this.repository.get();
	}

	@Nonnull
	public Class<? extends T> getObjectType() {
		return this.repositoryInterface;
	}

	public boolean isSingleton() {
		return true;
	}

	public void afterPropertiesSet() {

		this.factory = createRepositoryFactory();
		this.factory.setQueryLookupStrategyKey(this.queryLookupStrategyKey);
		this.factory.setNamedQueries(this.namedQueries);
		this.factory.setEvaluationContextProvider(
				this.evaluationContextProvider.orElseGet(() -> QueryMethodEvaluationContextProvider.DEFAULT));
		this.factory.setBeanClassLoader(this.classLoader);
		this.factory.setBeanFactory(this.beanFactory);

		if (this.publisher != null) {
			this.factory
					.addRepositoryProxyPostProcessor(new EventPublishingRepositoryProxyPostProcessor(this.publisher));
		}

		this.repositoryBaseClass.ifPresent(this.factory::setRepositoryBaseClass);

		RepositoryFragments customImplementationFragment = this.customImplementation //
				.map(RepositoryFragments::just) //
				.orElseGet(RepositoryFragments::empty);

		RepositoryFragments repositoryFragmentsToUse = this.repositoryFragments //
				.orElseGet(RepositoryFragments::empty) //
				.append(customImplementationFragment);

		this.repositoryMetadata = this.factory.getRepositoryMetadata(this.repositoryInterface);

		// Make sure the aggregate root type is present in the MappingContext (e.g. for
		// auditing)
		this.mappingContext.ifPresent(it -> it.getPersistentEntity(this.repositoryMetadata.getDomainType()));

		this.repository = Lazy.of(() -> this.factory.getRepository(this.repositoryInterface, repositoryFragmentsToUse));

		if (!this.lazyInit) {
			this.repository.get();
		}
	}

	/**
	 * Create the actual {@link RepositoryFactorySupport} instance.
	 * @return
	 */
	protected abstract RepositoryFactorySupport createRepositoryFactory();

}
