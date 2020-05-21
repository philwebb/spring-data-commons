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

import java.util.Collection;
import java.util.Locale;
import java.util.Optional;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.support.DefaultBeanNameGenerator;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.core.env.Environment;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.data.config.ConfigurationUtils;
import org.springframework.data.config.TypeFilterParser;
import org.springframework.data.config.TypeFilterParser.Type;
import org.springframework.data.repository.query.QueryLookupStrategy.Key;
import org.springframework.data.util.ParsingUtils;
import org.springframework.data.util.Streamable;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * XML based {@link RepositoryConfigurationSource}. Uses configuration defined on
 * {@link Element} attributes.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Christoph Strobl
 * @author Peter Rietzler
 * @author Jens Schauder
 */
public class XmlRepositoryConfigurationSource extends RepositoryConfigurationSourceSupport {

	private static final String QUERY_LOOKUP_STRATEGY = "query-lookup-strategy";

	private static final String BASE_PACKAGE = "base-package";

	private static final String NAMED_QUERIES_LOCATION = "named-queries-location";

	private static final String REPOSITORY_IMPL_POSTFIX = "repository-impl-postfix";

	private static final String REPOSITORY_FACTORY_BEAN_CLASS_NAME = "factory-class";

	private static final String REPOSITORY_BASE_CLASS_NAME = "base-class";

	private static final String CONSIDER_NESTED_REPOSITORIES = "consider-nested-repositories";

	private static final String BOOTSTRAP_MODE = "bootstrap-mode";

	private final Element element;

	private final ParserContext context;

	private final Collection<TypeFilter> includeFilters;

	private final Collection<TypeFilter> excludeFilters;

	/**
	 * Creates a new {@link XmlRepositoryConfigurationSource} using the given
	 * {@link Element} and {@link ParserContext}.
	 * @param element must not be {@literal null}.
	 * @param context must not be {@literal null}.
	 * @param environment must not be {@literal null}.
	 */
	public XmlRepositoryConfigurationSource(Element element, ParserContext context, Environment environment) {
		super(environment, ConfigurationUtils.getRequiredClassLoader(context.getReaderContext()), context.getRegistry(),
				defaultBeanNameGenerator(context.getReaderContext().getReader().getBeanNameGenerator()));
		Assert.notNull(element, "Element must not be null!");
		this.element = element;
		this.context = context;
		TypeFilterParser parser = new TypeFilterParser(context.getReaderContext());
		this.includeFilters = parser.parseTypeFilters(element, Type.INCLUDE);
		this.excludeFilters = parser.parseTypeFilters(element, Type.EXCLUDE);
	}

	@Override
	@Nullable
	public Object getSource() {
		return this.context.extractSource(this.element);
	}

	@Override
	public Streamable<String> getBasePackages() {
		String attribute = this.element.getAttribute(BASE_PACKAGE);
		return Streamable.of(StringUtils.delimitedListToStringArray(attribute, ",", " "));
	}

	@Override
	public Optional<Object> getQueryLookupStrategyKey() {
		return getNullDefaultedAttribute(this.element, QUERY_LOOKUP_STRATEGY).map(Key::create);
	}

	@Override
	public Optional<String> getNamedQueryLocation() {
		return getNullDefaultedAttribute(this.element, NAMED_QUERIES_LOCATION);
	}

	/**
	 * Returns the XML element backing the configuration.
	 * @return the element
	 */
	public Element getElement() {
		return this.element;
	}

	@Override
	public Streamable<TypeFilter> getExcludeFilters() {
		return Streamable.of(this.excludeFilters);
	}

	@Override
	protected Iterable<TypeFilter> getIncludeFilters() {
		return this.includeFilters;
	}

	@Override
	public Optional<String> getRepositoryImplementationPostfix() {
		return getNullDefaultedAttribute(this.element, REPOSITORY_IMPL_POSTFIX);
	}

	public Optional<String> getRepositoryFactoryBeanName() {
		return getNullDefaultedAttribute(this.element, REPOSITORY_FACTORY_BEAN_CLASS_NAME);
	}

	@Override
	public Optional<String> getRepositoryBaseClassName() {
		return getNullDefaultedAttribute(this.element, REPOSITORY_BASE_CLASS_NAME);
	}

	@Override
	public Optional<String> getRepositoryFactoryBeanClassName() {
		return getNullDefaultedAttribute(this.element, REPOSITORY_FACTORY_BEAN_CLASS_NAME);
	}

	private Optional<String> getNullDefaultedAttribute(Element element, String attributeName) {
		String attribute = element.getAttribute(attributeName);
		return StringUtils.hasText(attribute) ? Optional.of(attribute) : Optional.empty();
	}

	@Override
	public boolean shouldConsiderNestedRepositories() {
		return getNullDefaultedAttribute(this.element, CONSIDER_NESTED_REPOSITORIES).map(Boolean::parseBoolean)
				.orElse(false);
	}

	@Override
	public Optional<String> getAttribute(String name) {
		String xmlAttributeName = ParsingUtils.reconcatenateCamelCase(name, "-");
		String attribute = this.element.getAttribute(xmlAttributeName);
		return StringUtils.hasText(attribute) ? Optional.of(attribute) : Optional.empty();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> Optional<T> getAttribute(String name, Class<T> type) {
		Assert.isAssignable(String.class, type, "Only String attribute lookups are allowed for XML namespaces!");
		return (Optional<T>) getAttribute(name);
	}

	@Override
	public boolean usesExplicitFilters() {
		return !(this.includeFilters.isEmpty() && this.excludeFilters.isEmpty());
	}

	@Override
	public BootstrapMode getBootstrapMode() {
		String attribute = this.element.getAttribute(BOOTSTRAP_MODE);
		return StringUtils.hasText(attribute) 
				? BootstrapMode.valueOf(attribute.toUpperCase(Locale.US)) 
				: BootstrapMode.DEFAULT;
	}

	@Override
	@NonNull
	public String getResourceDescription() {
		Object source = getSource();
		return source == null ? "" : source.toString();
	}

	/**
	 * Returns the {@link BeanNameGenerator} to use falling back to an
	 * {@link AnnotationBeanNameGenerator} if either the given generator is
	 * {@literal null} or it's {@link DefaultBeanNameGenerator} in particular. This is to
	 * make sure we only use the given {@link BeanNameGenerator} if it was customized.
	 * @param generator can be {@literal null}.
	 * @return
	 * @since 2.2
	 */
	private static BeanNameGenerator defaultBeanNameGenerator(@Nullable BeanNameGenerator generator) {
		return generator == null || DefaultBeanNameGenerator.class.equals(generator.getClass()) 
				? new AnnotationBeanNameGenerator() 
				: generator;
	}

}
