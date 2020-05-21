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

package org.springframework.data.repository.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.stream.Stream;

import com.google.common.base.Optional;
import scala.Function0;
import scala.Option;
import scala.runtime.AbstractFunction0;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Slice;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.util.StreamUtils;
import org.springframework.data.util.Streamable;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ObjectUtils;
import org.springframework.util.concurrent.ListenableFuture;

/**
 * Converters to potentially wrap the execution of a repository method into a variety of
 * wrapper types potentially being available on the classpath. Currently supported:
 * <ul>
 * <li>{@code java.util.Optional}</li>
 * <li>{@code com.google.common.base.Optional}</li>
 * <li>{@code scala.Option} - as of 1.12</li>
 * <li>{@code java.util.concurrent.Future}</li>
 * <li>{@code java.util.concurrent.CompletableFuture}</li>
 * <li>{@code org.springframework.util.concurrent.ListenableFuture<}</li>
 * <li>{@code javaslang.control.Option} - as of 1.13</li>
 * <li>{@code javaslang.collection.Seq}, {@code javaslang.collection.Map},
 * {@code javaslang.collection.Set} - as of 1.13</li>
 * <li>{@code io.vavr.collection.Seq}, {@code io.vavr.collection.Map},
 * {@code io.vavr.collection.Set} - as of 2.0</li>
 * <li>Reactive wrappers supported by {@link ReactiveWrappers} - as of 2.0</li>
 * </ul>
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 * @author Christoph Strobl
 * @author Maciek Opała
 * @author Jens Schauder
 * @since 1.8
 * @see ReactiveWrappers
 */
public abstract class QueryExecutionConverters {

	private static final boolean GUAVA_PRESENT = ClassUtils.isPresent("com.google.common.base.Optional",
			QueryExecutionConverters.class.getClassLoader());

	private static final boolean JDK_8_PRESENT = ClassUtils.isPresent("java.util.Optional",
			QueryExecutionConverters.class.getClassLoader());

	private static final boolean SCALA_PRESENT = ClassUtils.isPresent("scala.Option",
			QueryExecutionConverters.class.getClassLoader());

	private static final boolean VAVR_PRESENT = ClassUtils.isPresent("io.vavr.control.Option",
			QueryExecutionConverters.class.getClassLoader());

	private static final Set<WrapperType> WRAPPER_TYPES = new HashSet<>();

	private static final Set<WrapperType> UNWRAPPER_TYPES = new HashSet<>();

	private static final Set<Converter<Object, Object>> UNWRAPPERS = new HashSet<>();

	private static final Set<Class<?>> ALLOWED_PAGEABLE_TYPES = new HashSet<>();

	private static final Map<Class<?>, ExecutionAdapter> EXECUTION_ADAPTER = new HashMap<>();

	private static final Map<Class<?>, Boolean> supportsCache = new ConcurrentReferenceHashMap<>();

	static {
		WRAPPER_TYPES.add(WrapperType.singleValue(Future.class));
		UNWRAPPER_TYPES.add(WrapperType.singleValue(Future.class));
		WRAPPER_TYPES.add(WrapperType.singleValue(ListenableFuture.class));
		UNWRAPPER_TYPES.add(WrapperType.singleValue(ListenableFuture.class));
		ALLOWED_PAGEABLE_TYPES.add(Slice.class);
		ALLOWED_PAGEABLE_TYPES.add(Page.class);
		ALLOWED_PAGEABLE_TYPES.add(List.class);
		if (GUAVA_PRESENT) {
			WRAPPER_TYPES.add(NullableWrapperToGuavaOptionalConverter.getWrapperType());
			UNWRAPPER_TYPES.add(NullableWrapperToGuavaOptionalConverter.getWrapperType());
			UNWRAPPERS.add(GuavaOptionalUnwrapper.INSTANCE);
		}
		if (JDK_8_PRESENT) {
			WRAPPER_TYPES.add(NullableWrapperToJdk8OptionalConverter.getWrapperType());
			UNWRAPPER_TYPES.add(NullableWrapperToJdk8OptionalConverter.getWrapperType());
			UNWRAPPERS.add(Jdk8OptionalUnwrapper.INSTANCE);
		}
		if (JDK_8_PRESENT) {
			WRAPPER_TYPES.add(NullableWrapperToCompletableFutureConverter.getWrapperType());
			UNWRAPPER_TYPES.add(NullableWrapperToCompletableFutureConverter.getWrapperType());
		}
		if (SCALA_PRESENT) {
			WRAPPER_TYPES.add(NullableWrapperToScalaOptionConverter.getWrapperType());
			UNWRAPPER_TYPES.add(NullableWrapperToScalaOptionConverter.getWrapperType());
			UNWRAPPERS.add(ScalOptionUnwrapper.INSTANCE);
		}
		if (VAVR_PRESENT) {
			WRAPPER_TYPES.add(NullableWrapperToVavrOptionConverter.getWrapperType());
			WRAPPER_TYPES.add(VavrCollections.ToJavaConverter.INSTANCE.getWrapperType());
			UNWRAPPERS.add(VavrOptionUnwrapper.INSTANCE);
			// Try support
			WRAPPER_TYPES.add(WrapperType.singleValue(io.vavr.control.Try.class));
			EXECUTION_ADAPTER.put(io.vavr.control.Try.class, (it) -> io.vavr.control.Try.of(it::get));
			ALLOWED_PAGEABLE_TYPES.add(io.vavr.collection.Seq.class);
		}
	}

	private QueryExecutionConverters() {
	}

	/**
	 * Returns whether the given type is a supported wrapper type.
	 * @param type must not be {@literal null}.
	 * @return
	 */
	public static boolean supports(Class<?> type) {
		Assert.notNull(type, "Type must not be null!");
		return supportsCache.computeIfAbsent(type, (key) -> {
			for (WrapperType candidate : WRAPPER_TYPES) {
				if (candidate.getType().isAssignableFrom(key)) {
					return true;
				}
			}
			if (ReactiveWrappers.supports(type)) {
				return true;
			}
			return false;
		});
	}

	/**
	 * Returns whether the given wrapper type supports unwrapping.
	 * @param type must not be {@literal null}.
	 * @return
	 */
	public static boolean supportsUnwrapping(Class<?> type) {
		Assert.notNull(type, "Type must not be null!");
		for (WrapperType candidate : UNWRAPPER_TYPES) {
			if (candidate.getType().isAssignableFrom(type)) {
				return true;
			}
		}
		return false;
	}

	public static boolean isSingleValue(Class<?> type) {
		for (WrapperType candidate : WRAPPER_TYPES) {
			if (candidate.getType().isAssignableFrom(type)) {
				return candidate.isSingleValue();
			}
		}
		if (ReactiveWrappers.supports(type) && ReactiveWrappers.isSingleValueType(type)) {
			return true;
		}
		return false;
	}

	/**
	 * Returns the types that are supported on paginating query methods. Will include
	 * custom collection types of e.g. Javaslang.
	 * @return
	 */
	public static Set<Class<?>> getAllowedPageableTypes() {
		return Collections.unmodifiableSet(ALLOWED_PAGEABLE_TYPES);
	}

	/**
	 * Registers converters for wrapper types found on the classpath.
	 * @param conversionService must not be {@literal null}.
	 */
	public static void registerConvertersIn(ConfigurableConversionService conversionService) {
		Assert.notNull(conversionService, "ConversionService must not be null!");
		conversionService.removeConvertible(Collection.class, Object.class);
		if (GUAVA_PRESENT) {
			conversionService.addConverter(new NullableWrapperToGuavaOptionalConverter(conversionService));
		}
		if (JDK_8_PRESENT) {
			conversionService.addConverter(new NullableWrapperToJdk8OptionalConverter(conversionService));
			conversionService.addConverter(new NullableWrapperToCompletableFutureConverter(conversionService));
		}
		if (SCALA_PRESENT) {
			conversionService.addConverter(new NullableWrapperToScalaOptionConverter(conversionService));
		}
		if (VAVR_PRESENT) {
			conversionService.addConverter(new NullableWrapperToVavrOptionConverter(conversionService));
			conversionService.addConverter(VavrCollections.FromJavaConverter.INSTANCE);
		}
		conversionService.addConverter(new NullableWrapperToFutureConverter(conversionService));
		conversionService.addConverter(new IterableToStreamableConverter());
	}

	/**
	 * Unwraps the given source value in case it's one of the currently supported wrapper
	 * types detected at runtime.
	 * @param source can be {@literal null}.
	 * @return
	 */
	@Nullable
	public static Object unwrap(@Nullable Object source) {
		if (source == null || !supports(source.getClass())) {
			return source;
		}
		for (Converter<Object, Object> converter : UNWRAPPERS) {
			Object result = converter.convert(source);
			if (result != source) {
				return result;
			}
		}
		return source;
	}

	/**
	 * Recursively unwraps well known wrapper types from the given
	 * {@link TypeInformation}.
	 * @param type must not be {@literal null}.
	 * @return will never be {@literal null}.
	 */
	public static TypeInformation<?> unwrapWrapperTypes(TypeInformation<?> type) {
		Assert.notNull(type, "type must not be null");
		Class<?> rawType = type.getType();
		boolean needToUnwrap = type.isCollectionLike() || Slice.class.isAssignableFrom(rawType)
				|| GeoResults.class.isAssignableFrom(rawType) || rawType.isArray() || supports(rawType)
				|| Stream.class.isAssignableFrom(rawType);
		return needToUnwrap ? unwrapWrapperTypes(type.getRequiredComponentType()) : type;
	}

	/**
	 * Returns the {@link ExecutionAdapter} to be used for the given return type.
	 * @param returnType must not be {@literal null}.
	 * @return
	 */
	@Nullable
	public static ExecutionAdapter getExecutionAdapter(Class<?> returnType) {
		Assert.notNull(returnType, "Return type must not be null!");
		return EXECUTION_ADAPTER.get(returnType);
	}

	public interface ThrowingSupplier {

		Object get() throws Throwable;

	}

	public interface ExecutionAdapter {

		Object apply(ThrowingSupplier supplier) throws Throwable;

	}

	/**
	 * Base class for converters that create instances of wrapper types such as Google
	 * Guava's and JDK 8's {@code Optional} types.
	 */
	private abstract static class AbstractWrapperTypeConverter implements GenericConverter {

		private final ConversionService conversionService;

		private final Object nullValue;

		private final Iterable<Class<?>> wrapperTypes;

		/**
		 * Creates a new {@link AbstractWrapperTypeConverter} using the given
		 * {@link ConversionService} and wrapper type.
		 * @param conversionService must not be {@literal null}.
		 * @param nullValue must not be {@literal null}.
		 */
		protected AbstractWrapperTypeConverter(ConversionService conversionService, Object nullValue) {
			Assert.notNull(conversionService, "ConversionService must not be null!");
			Assert.notNull(nullValue, "Null value must not be null!");
			this.conversionService = conversionService;
			this.nullValue = nullValue;
			this.wrapperTypes = Collections.singleton(nullValue.getClass());
		}

		protected AbstractWrapperTypeConverter(ConversionService conversionService, Object nullValue,
				Iterable<Class<?>> wrapperTypes) {
			this.conversionService = conversionService;
			this.nullValue = nullValue;
			this.wrapperTypes = wrapperTypes;
		}

		@Override
		public Set<ConvertiblePair> getConvertibleTypes() {
			return Streamable.of(this.wrapperTypes).map((it) -> new ConvertiblePair(NullableWrapper.class, it)).stream()
					.collect(StreamUtils.toUnmodifiableSet());
		}

		@Nullable
		@Override
		public final Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
			if (source == null) {
				return null;
			}
			NullableWrapper wrapper = (NullableWrapper) source;
			Object value = wrapper.getValue();
			// TODO: Add Recursive conversion once we move to Spring 4
			return (value != null) ? wrap(value) : this.nullValue;
		}

		/**
		 * Wrap the given, non-{@literal null} value into the wrapper type.
		 * @param source will never be {@literal null}.
		 * @return must not be {@literal null}.
		 */
		protected abstract Object wrap(Object source);

	}

	/**
	 * A Spring {@link Converter} to support Google Guava's {@link Optional}.
	 */
	private static class NullableWrapperToGuavaOptionalConverter extends AbstractWrapperTypeConverter {

		/**
		 * Creates a new {@link NullableWrapperToGuavaOptionalConverter} using the given
		 * {@link ConversionService}.
		 * @param conversionService must not be {@literal null}.
		 */
		NullableWrapperToGuavaOptionalConverter(ConversionService conversionService) {
			super(conversionService, Optional.absent(), Collections.singleton(Optional.class));
		}

		@Override
		protected Object wrap(Object source) {
			return Optional.of(source);
		}

		static WrapperType getWrapperType() {
			return WrapperType.singleValue(Optional.class);
		}

	}

	/**
	 * A Spring {@link Converter} to support JDK 8's {@link java.util.Optional}.
	 */
	private static class NullableWrapperToJdk8OptionalConverter extends AbstractWrapperTypeConverter {

		/**
		 * Creates a new {@link NullableWrapperToJdk8OptionalConverter} using the given
		 * {@link ConversionService}.
		 * @param conversionService must not be {@literal null}.
		 */
		NullableWrapperToJdk8OptionalConverter(ConversionService conversionService) {
			super(conversionService, java.util.Optional.empty());
		}

		@Override
		protected Object wrap(Object source) {
			return java.util.Optional.of(source);
		}

		static WrapperType getWrapperType() {
			return WrapperType.singleValue(java.util.Optional.class);
		}

	}

	/**
	 * A Spring {@link Converter} to support returning {@link Future} instances from
	 * repository methods.
	 */
	private static class NullableWrapperToFutureConverter extends AbstractWrapperTypeConverter {

		/**
		 * Creates a new {@link NullableWrapperToFutureConverter} using the given
		 * {@link ConversionService}.
		 * @param conversionService must not be {@literal null}.
		 */
		NullableWrapperToFutureConverter(ConversionService conversionService) {
			super(conversionService, new AsyncResult<>(null), Arrays.asList(Future.class, ListenableFuture.class));
		}

		@Override
		protected Object wrap(Object source) {
			return new AsyncResult<>(source);
		}

	}

	/**
	 * A Spring {@link Converter} to support returning {@link CompletableFuture} instances
	 * from repository methods.
	 */
	private static class NullableWrapperToCompletableFutureConverter extends AbstractWrapperTypeConverter {

		/**
		 * Creates a new {@link NullableWrapperToCompletableFutureConverter} using the
		 * given {@link ConversionService}.
		 * @param conversionService must not be {@literal null}.
		 */
		NullableWrapperToCompletableFutureConverter(ConversionService conversionService) {
			super(conversionService, CompletableFuture.completedFuture(null));
		}

		@Override
		protected Object wrap(Object source) {
			return (source instanceof CompletableFuture) ? source : CompletableFuture.completedFuture(source);
		}

		static WrapperType getWrapperType() {
			return WrapperType.singleValue(CompletableFuture.class);
		}

	}

	/**
	 * A Spring {@link Converter} to support Scala's {@link Option}.
	 *
	 * @since 1.13
	 */
	private static class NullableWrapperToScalaOptionConverter extends AbstractWrapperTypeConverter {

		NullableWrapperToScalaOptionConverter(ConversionService conversionService) {
			super(conversionService, Option.empty(), Collections.singleton(Option.class));
		}

		@Override
		protected Object wrap(Object source) {
			return Option.apply(source);
		}

		static WrapperType getWrapperType() {
			return WrapperType.singleValue(Option.class);
		}

	}

	/**
	 * Converter to convert from {@link NullableWrapper} into JavaSlang's
	 * {@link io.vavr.control.Option}.
	 *
	 * @since 2.0
	 */
	private static class NullableWrapperToVavrOptionConverter extends AbstractWrapperTypeConverter {

		/**
		 * Creates a new {@link NullableWrapperToJavaslangOptionConverter} using the given
		 * {@link ConversionService}.
		 * @param conversionService must not be {@literal null}.
		 */
		NullableWrapperToVavrOptionConverter(ConversionService conversionService) {
			super(conversionService, io.vavr.control.Option.none(),
					Collections.singleton(io.vavr.control.Option.class));
		}

		static WrapperType getWrapperType() {
			return WrapperType.singleValue(io.vavr.control.Option.class);
		}

		@Override
		protected Object wrap(Object source) {
			return io.vavr.control.Option.of(source);
		}

	}

	/**
	 * A {@link Converter} to unwrap Guava {@link Optional} instances.
	 * @since 1.12
	 */
	private enum GuavaOptionalUnwrapper implements Converter<Object, Object> {

		INSTANCE;

		@Nullable
		@Override
		public Object convert(Object source) {
			return (source instanceof Optional) ? ((Optional<?>) source).orNull() : source;
		}

	}

	/**
	 * A {@link Converter} to unwrap JDK 8 {@link java.util.Optional} instances.
	 * @since 1.12
	 */
	private enum Jdk8OptionalUnwrapper implements Converter<Object, Object> {

		INSTANCE;

		@Nullable
		@Override
		public Object convert(Object source) {
			return (source instanceof java.util.Optional) ? ((java.util.Optional<?>) source).orElse(null) : source;
		}

	}

	/**
	 * A {@link Converter} to unwrap a Scala {@link Option} instance.
	 *
	 * @since 1.12
	 */
	private enum ScalOptionUnwrapper implements Converter<Object, Object> {

		INSTANCE;

		private final Function0<Object> alternative = new AbstractFunction0<Object>() {

			@Nullable
			@Override
			public Option<Object> apply() {
				return null;
			}

		};

		@Nullable
		@Override
		public Object convert(Object source) {
			return (source instanceof Option) ? ((Option<?>) source).getOrElse(this.alternative) : source;
		}

	}

	/**
	 * Converter to unwrap Vavr {@link io.vavr.control.Option} instances.
	 *
	 * @since 2.0
	 */
	private enum VavrOptionUnwrapper implements Converter<Object, Object> {

		INSTANCE;

		@Nullable
		@Override
		@SuppressWarnings("unchecked")
		public Object convert(Object source) {
			if (source instanceof io.vavr.control.Option) {
				return ((io.vavr.control.Option<Object>) source).getOrElse(() -> null);
			}
			if (source instanceof io.vavr.collection.Traversable) {
				return VavrCollections.ToJavaConverter.INSTANCE.convert(source);
			}
			return source;
		}

	}

	private static class IterableToStreamableConverter implements ConditionalGenericConverter {

		private static final TypeDescriptor STREAMABLE = TypeDescriptor.valueOf(Streamable.class);

		private final Map<TypeDescriptor, Boolean> targetTypeCache = new ConcurrentHashMap<>();

		private final ConversionService conversionService = DefaultConversionService.getSharedInstance();

		IterableToStreamableConverter() {
		}

		@NonNull
		@Override
		public Set<ConvertiblePair> getConvertibleTypes() {
			return Collections.singleton(new ConvertiblePair(Iterable.class, Object.class));
		}

		@Override
		public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
			if (sourceType.isAssignableTo(targetType)) {
				return false;
			}
			if (!Iterable.class.isAssignableFrom(sourceType.getType())) {
				return false;
			}
			if (Streamable.class.equals(targetType.getType())) {
				return true;
			}
			return this.targetTypeCache.computeIfAbsent(targetType,
					(it) -> this.conversionService.canConvert(STREAMABLE, targetType));
		}

		@SuppressWarnings("unchecked")
		@Nullable
		@Override
		public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
			Streamable<Object> streamable = (source != null) ? Streamable.of(Iterable.class.cast(source))
					: Streamable.empty();
			return Streamable.class.equals(targetType.getType()) ? streamable
					: this.conversionService.convert(streamable, STREAMABLE, targetType);
		}

	}

	public static final class WrapperType {

		private WrapperType(Class<?> type, Cardinality cardinality) {
			this.type = type;
			this.cardinality = cardinality;
		}

		public Class<?> getType() {
			return this.type;
		}

		public Cardinality getCardinality() {
			return this.cardinality;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (!(o instanceof WrapperType)) {
				return false;
			}
			WrapperType other = (WrapperType) o;
			if (!ObjectUtils.nullSafeEquals(this.type, other.type)) {
				return false;
			}
			return this.cardinality == other.cardinality;
		}

		@Override
		public int hashCode() {
			int result = ObjectUtils.nullSafeHashCode(this.type);
			result = 31 * result + ObjectUtils.nullSafeHashCode(this.cardinality);
			return result;
		}

		@Override
		public String toString() {
			return "QueryExecutionConverters.WrapperType(type=" + this.getType() + ", cardinality="
					+ this.getCardinality() + ")";
		}

		enum Cardinality {

			NONE, SINGLE, MULTI;

		}

		private final Class<?> type;

		private final Cardinality cardinality;

		public static WrapperType singleValue(Class<?> type) {
			return new WrapperType(type, Cardinality.SINGLE);
		}

		public static WrapperType multiValue(Class<?> type) {
			return new WrapperType(type, Cardinality.MULTI);
		}

		public static WrapperType noValue(Class<?> type) {
			return new WrapperType(type, Cardinality.NONE);
		}

		boolean isSingleValue() {
			return this.cardinality.equals(Cardinality.SINGLE);
		}

	}

}
