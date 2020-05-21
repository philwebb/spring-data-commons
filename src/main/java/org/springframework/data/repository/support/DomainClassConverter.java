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

package org.springframework.data.repository.support;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link org.springframework.core.convert.converter.Converter} to convert arbitrary input
 * into domain classes managed by Spring Data {@link CrudRepository}s. The implementation
 * uses a {@link ConversionService} in turn to convert the source type into the domain
 * class' id type which is then converted into a domain class object by using a
 * {@link CrudRepository}.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 */
public class DomainClassConverter<T extends ConversionService & ConverterRegistry>
		implements ConditionalGenericConverter, ApplicationContextAware {

	private final T conversionService;

	private Repositories repositories = Repositories.NONE;

	private Optional<ToEntityConverter> toEntityConverter = Optional.empty();

	private Optional<ToIdConverter> toIdConverter = Optional.empty();

	/**
	 * Creates a new {@link DomainClassConverter} for the given {@link ConversionService}.
	 * @param conversionService must not be {@literal null}.
	 */
	public DomainClassConverter(T conversionService) {

		Assert.notNull(conversionService, "ConversionService must not be null!");

		this.conversionService = conversionService;
	}

	@Nonnull
	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		return Collections.singleton(new ConvertiblePair(Object.class, Object.class));
	}

	@Nullable
	@Override
	public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		return getConverter(targetType).map((it) -> it.convert(source, sourceType, targetType)).orElse(source);
	}

	@Override
	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
		return getConverter(targetType).map((it) -> it.matches(sourceType, targetType)).orElse(false);
	}

	/**
	 * @param targetType
	 * @return
	 */
	private Optional<? extends ConditionalGenericConverter> getConverter(TypeDescriptor targetType) {
		return this.repositories.hasRepositoryFor(targetType.getType()) ? this.toEntityConverter : this.toIdConverter;
	}

	@Override
	public void setApplicationContext(ApplicationContext context) {
		this.repositories = new Repositories(context);
		this.toEntityConverter = Optional.of(new ToEntityConverter(this.repositories, this.conversionService));
		this.toEntityConverter.ifPresent((it) -> this.conversionService.addConverter(it));
		this.toIdConverter = Optional.of(new ToIdConverter());
		this.toIdConverter.ifPresent((it) -> this.conversionService.addConverter(it));
	}

	/**
	 * Converter to create domain types from any source that can be converted into the
	 * domain types identifier type.
	 *
	 * @since 1.10
	 */
	private class ToEntityConverter implements ConditionalGenericConverter {

		private final RepositoryInvokerFactory repositoryInvokerFactory;

		/**
		 * Creates a new {@link ToEntityConverter} for the given {@link Repositories} and
		 * {@link ConversionService}.
		 * @param repositories must not be {@literal null}.
		 * @param conversionService must not be {@literal null}.
		 */
		ToEntityConverter(Repositories repositories, ConversionService conversionService) {
			this.repositoryInvokerFactory = new DefaultRepositoryInvokerFactory(repositories, conversionService);
		}

		@Nonnull
		@Override
		public Set<ConvertiblePair> getConvertibleTypes() {
			return Collections.singleton(new ConvertiblePair(Object.class, Object.class));
		}

		@Nullable
		@Override
		public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
			if (source == null || !StringUtils.hasText(source.toString())) {
				return null;
			}
			if (sourceType.equals(targetType)) {
				return source;
			}
			Class<?> domainType = targetType.getType();
			RepositoryInvoker invoker = this.repositoryInvokerFactory.getInvokerFor(domainType);
			RepositoryInformation information = DomainClassConverter.this.repositories
					.getRequiredRepositoryInformation(domainType);
			Object id = DomainClassConverter.this.conversionService.convert(source, information.getIdType());
			return (id != null) ? invoker.invokeFindById(id).orElse(null) : null;
		}

		@Override
		public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
			if (sourceType.isAssignableTo(targetType)) {
				return false;
			}
			Class<?> domainType = targetType.getType();
			if (!DomainClassConverter.this.repositories.hasRepositoryFor(domainType)) {
				return false;
			}
			Optional<RepositoryInformation> repositoryInformation = DomainClassConverter.this.repositories
					.getRepositoryInformationFor(domainType);
			return repositoryInformation.map((it) -> {
				Class<?> rawIdType = it.getIdType();
				return sourceType.equals(TypeDescriptor.valueOf(rawIdType))
						|| DomainClassConverter.this.conversionService.canConvert(sourceType.getType(), rawIdType);
			}).orElseThrow(() -> new IllegalStateException(
					String.format("Couldn't find RepositoryInformation for %s!", domainType)));
		}

	}

	/**
	 * Converter to turn domain types into their identifiers or any transitively
	 * convertible type.
	 *
	 * @since 1.10
	 */
	class ToIdConverter implements ConditionalGenericConverter {

		@Nonnull
		@Override
		public Set<ConvertiblePair> getConvertibleTypes() {
			return Collections.singleton(new ConvertiblePair(Object.class, Object.class));
		}

		@Nullable
		@Override
		public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
			if (source == null || !StringUtils.hasText(source.toString())) {
				return null;
			}
			if (sourceType.equals(targetType)) {
				return source;
			}
			Class<?> domainType = sourceType.getType();
			EntityInformation<Object, ?> entityInformation = DomainClassConverter.this.repositories
					.getEntityInformationFor(domainType);
			return DomainClassConverter.this.conversionService.convert(entityInformation.getId(source),
					targetType.getType());
		}

		@Override
		public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
			if (sourceType.isAssignableTo(targetType)) {
				return false;
			}
			Class<?> domainType = sourceType.getType();
			if (!DomainClassConverter.this.repositories.hasRepositoryFor(domainType)) {
				return false;
			}
			Optional<RepositoryInformation> information = DomainClassConverter.this.repositories
					.getRepositoryInformationFor(domainType);
			return information.map((it) -> {
				Class<?> rawIdType = it.getIdType();
				return targetType.equals(TypeDescriptor.valueOf(rawIdType))
						|| DomainClassConverter.this.conversionService.canConvert(rawIdType, targetType.getType());

			}).orElseThrow(() -> new IllegalStateException(
					String.format("Couldn't find RepositoryInformation for %s!", domainType)));
		}

	}

}
