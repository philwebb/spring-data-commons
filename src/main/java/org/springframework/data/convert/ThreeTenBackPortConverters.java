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

package org.springframework.data.convert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.annotation.Nonnull;

import org.threeten.bp.DateTimeUtils;
import org.threeten.bp.Instant;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.LocalTime;
import org.threeten.bp.ZoneId;

import org.springframework.core.convert.converter.Converter;
import org.springframework.util.ClassUtils;

/**
 * Helper class to register {@link Converter} implementations for the ThreeTen Backport
 * project in case it's present on the classpath.
 *
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @author Jens Schauder
 * @author Mark Paluch
 * @see <a href=
 * "https://www.threeten.org/threetenbp">https://www.threeten.org/threetenbp</a>
 * @since 1.10
 * @deprecated since 2.3, use JSR-310 types as replacement for ThreeTenBackport.
 */
@Deprecated
public abstract class ThreeTenBackPortConverters {

	private static final boolean THREE_TEN_BACK_PORT_IS_PRESENT = ClassUtils.isPresent("org.threeten.bp.LocalDateTime",
			ThreeTenBackPortConverters.class.getClassLoader());

	private static final Collection<Class<?>> SUPPORTED_TYPES;

	static {
		SUPPORTED_TYPES = THREE_TEN_BACK_PORT_IS_PRESENT ? Arrays.asList(LocalDateTime.class, LocalDate.class,
				LocalTime.class, Instant.class, java.time.Instant.class) : Collections.emptySet();
	}

	/**
	 * Returns the converters to be registered. Will only return converters in case we're
	 * running on Java 8.
	 * @return
	 */
	public static Collection<Converter<?, ?>> getConvertersToRegister() {
		if (!THREE_TEN_BACK_PORT_IS_PRESENT) {
			return Collections.emptySet();
		}
		List<Converter<?, ?>> converters = new ArrayList<>();
		converters.add(DateToLocalDateTimeConverter.INSTANCE);
		converters.add(LocalDateTimeToDateConverter.INSTANCE);
		converters.add(DateToLocalDateConverter.INSTANCE);
		converters.add(LocalDateToDateConverter.INSTANCE);
		converters.add(DateToLocalTimeConverter.INSTANCE);
		converters.add(LocalTimeToDateConverter.INSTANCE);
		converters.add(DateToInstantConverter.INSTANCE);
		converters.add(InstantToDateConverter.INSTANCE);
		converters.add(ZoneIdToStringConverter.INSTANCE);
		converters.add(StringToZoneIdConverter.INSTANCE);
		converters.add(LocalDateTimeToJsr310LocalDateTimeConverter.INSTANCE);
		converters.add(LocalDateTimeToJavaTimeInstantConverter.INSTANCE);
		converters.add(JavaTimeInstantToLocalDateTimeConverter.INSTANCE);
		return converters;
	}

	public static boolean supports(Class<?> type) {
		return SUPPORTED_TYPES.contains(type);
	}

	@Deprecated
	public enum LocalDateTimeToJsr310LocalDateTimeConverter
			implements Converter<LocalDateTime, java.time.LocalDateTime> {

		INSTANCE;

		@Nonnull
		@Override
		public java.time.LocalDateTime convert(LocalDateTime source) {
			Date date = DateTimeUtils.toDate(source.atZone(ZoneId.systemDefault()).toInstant());
			return Jsr310Converters.DateToLocalDateTimeConverter.INSTANCE.convert(date);
		}

	}

	@Deprecated
	public enum DateToLocalDateTimeConverter implements Converter<Date, LocalDateTime> {

		INSTANCE;

		@Nonnull
		@Override
		public LocalDateTime convert(Date source) {
			return LocalDateTime.ofInstant(DateTimeUtils.toInstant(source), ZoneId.systemDefault());
		}

	}

	@Deprecated
	public enum LocalDateTimeToDateConverter implements Converter<LocalDateTime, Date> {

		INSTANCE;

		@Nonnull
		@Override
		public Date convert(LocalDateTime source) {
			return DateTimeUtils.toDate(source.atZone(ZoneId.systemDefault()).toInstant());
		}

	}

	@Deprecated
	public enum DateToLocalDateConverter implements Converter<Date, LocalDate> {

		INSTANCE;

		@Nonnull
		@Override
		public LocalDate convert(Date source) {
			return LocalDateTime.ofInstant(Instant.ofEpochMilli(source.getTime()), ZoneId.systemDefault())
					.toLocalDate();
		}

	}

	@Deprecated
	public enum LocalDateToDateConverter implements Converter<LocalDate, Date> {

		INSTANCE;

		@Nonnull
		@Override
		public Date convert(LocalDate source) {
			return DateTimeUtils.toDate(source.atStartOfDay(ZoneId.systemDefault()).toInstant());
		}

	}

	@Deprecated
	public enum DateToLocalTimeConverter implements Converter<Date, LocalTime> {

		INSTANCE;

		@Nonnull
		@Override
		public LocalTime convert(Date source) {
			return LocalDateTime.ofInstant(Instant.ofEpochMilli(source.getTime()), ZoneId.systemDefault())
					.toLocalTime();
		}

	}

	@Deprecated
	public enum LocalTimeToDateConverter implements Converter<LocalTime, Date> {

		INSTANCE;

		@Nonnull
		@Override
		public Date convert(LocalTime source) {
			return DateTimeUtils.toDate(source.atDate(LocalDate.now()).atZone(ZoneId.systemDefault()).toInstant());
		}

	}

	@Deprecated
	public enum DateToInstantConverter implements Converter<Date, Instant> {

		INSTANCE;

		@Nonnull
		@Override
		public Instant convert(Date source) {
			return DateTimeUtils.toInstant(source);
		}

	}

	@Deprecated
	public enum InstantToDateConverter implements Converter<Instant, Date> {

		INSTANCE;

		@Nonnull
		@Override
		public Date convert(Instant source) {
			return DateTimeUtils.toDate(source.atZone(ZoneId.systemDefault()).toInstant());
		}

	}

	@Deprecated
	public enum LocalDateTimeToJavaTimeInstantConverter implements Converter<LocalDateTime, java.time.Instant> {

		INSTANCE;

		@Nonnull
		@Override
		public java.time.Instant convert(LocalDateTime source) {
			return java.time.Instant.ofEpochMilli(source.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
		}

	}

	@Deprecated
	public enum JavaTimeInstantToLocalDateTimeConverter implements Converter<java.time.Instant, LocalDateTime> {

		INSTANCE;

		@Nonnull
		@Override
		public LocalDateTime convert(java.time.Instant source) {
			return LocalDateTime.ofInstant(Instant.ofEpochMilli(source.toEpochMilli()), ZoneId.systemDefault());
		}

	}

	@WritingConverter
	@Deprecated
	public enum ZoneIdToStringConverter implements Converter<ZoneId, String> {

		INSTANCE;

		@Nonnull
		@Override
		public String convert(ZoneId source) {
			return source.toString();
		}

	}

	@ReadingConverter
	@Deprecated
	public enum StringToZoneIdConverter implements Converter<String, ZoneId> {

		INSTANCE;

		@Nonnull
		@Override
		public ZoneId convert(String source) {
			return ZoneId.of(source);
		}

	}

}
