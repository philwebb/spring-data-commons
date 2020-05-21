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
package org.springframework.data.repository.config;

import java.util.Optional;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.data.config.ConfigurationUtils;
import org.springframework.data.repository.query.QueryLookupStrategy.Key;
import org.springframework.data.util.Streamable;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Default implementation of {@link RepositoryConfiguration}.
 *
 * @author Oliver Gierke
 * @author Jens Schauder
 * @author Mark Paluch
 */
public class DefaultRepositoryConfiguration<T extends RepositoryConfigurationSource>
		implements RepositoryConfiguration<T> {

	public static final String DEFAULT_REPOSITORY_IMPLEMENTATION_POSTFIX = "Impl";
	public static final Key DEFAULT_QUERY_LOOKUP_STRATEGY = Key.CREATE_IF_NOT_FOUND;

	private final T configurationSource;
	private final BeanDefinition definition;
	private final RepositoryConfigurationExtension extension;

	public DefaultRepositoryConfiguration(T configurationSource, BeanDefinition definition,
			RepositoryConfigurationExtension extension) {

		this.configurationSource = configurationSource;
		this.definition = definition;
		this.extension = extension;
	}
	public String getBeanId() {
		return StringUtils.uncapitalize(ClassUtils.getShortName(getRepositoryBaseClassName().orElseThrow(
				() -> new IllegalStateException("Can't create bean identifier without a repository base class defined!"))));
	}
	public Object getQueryLookupStrategyKey() {
		return this.configurationSource.getQueryLookupStrategyKey().orElse(DEFAULT_QUERY_LOOKUP_STRATEGY);
	}
	public Streamable<String> getBasePackages() {
		return this.configurationSource.getBasePackages();
	}
	@Override
	public Streamable<String> getImplementationBasePackages() {
		return Streamable.of(ClassUtils.getPackageName(getRepositoryInterface()));
	}
	public String getRepositoryInterface() {
		return ConfigurationUtils.getRequiredBeanClassName(this.definition);
	}
	public RepositoryConfigurationSource getConfigSource() {
		return this.configurationSource;
	}
	public Optional<String> getNamedQueriesLocation() {
		return this.configurationSource.getNamedQueryLocation();
	}
	public String getImplementationClassName() {
		return ClassUtils.getShortName(getRepositoryInterface()).concat(
				this.configurationSource.getRepositoryImplementationPostfix().orElse(DEFAULT_REPOSITORY_IMPLEMENTATION_POSTFIX));
	}
	public String getImplementationBeanName() {
		return this.configurationSource.generateBeanName(this.definition)
				+ this.configurationSource.getRepositoryImplementationPostfix().orElse("Impl");
	}
	@Nullable
	@Override
	public Object getSource() {
		return this.configurationSource.getSource();
	}
	@Override
	public T getConfigurationSource() {
		return this.configurationSource;
	}
	@Override
	public Optional<String> getRepositoryBaseClassName() {
		return this.configurationSource.getRepositoryBaseClassName();
	}
	@Override
	public String getRepositoryFactoryBeanClassName() {

		return this.configurationSource.getRepositoryFactoryBeanClassName()
				.orElseGet(this.extension::getRepositoryFactoryBeanClassName);
	}
	@Override
	public boolean isLazyInit() {
		return this.definition.isLazyInit() || !this.configurationSource.getBootstrapMode().equals(BootstrapMode.DEFAULT);
	}
	@Override
	public boolean isPrimary() {
		return this.definition.isPrimary();
	}
	@Override
	public Streamable<TypeFilter> getExcludeFilters() {
		return this.configurationSource.getExcludeFilters();
	}
	@Override
	public ImplementationDetectionConfiguration toImplementationDetectionConfiguration(MetadataReaderFactory factory) {

		Assert.notNull(factory, "MetadataReaderFactory must not be null!");

		return this.configurationSource.toImplementationDetectionConfiguration(factory);
	}
	@Override
	public ImplementationLookupConfiguration toLookupConfiguration(MetadataReaderFactory factory) {

		Assert.notNull(factory, "MetadataReaderFactory must not be null!");

		return toImplementationDetectionConfiguration(factory).forRepositoryConfiguration(this);
	}
	@Override
	@org.springframework.lang.NonNull
	public String getResourceDescription() {
		return String.format("%s defined in %s", getRepositoryInterface(), this.configurationSource.getResourceDescription());
	}
}
