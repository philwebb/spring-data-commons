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
package org.springframework.data.auditing;

import java.time.LocalDateTime;
import java.time.ZoneId;

import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.convert.ConversionService;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.auditing.DefaultAuditableBeanWrapperFactory.ReflectionAuditingBeanWrapper;
import org.springframework.data.convert.Jsr310Converters.LocalDateTimeToDateConverter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ReflectionAuditingBeanWrapper}.
 *
 * @author Oliver Gierke
 * @author Pavel Horal
 * @since 1.5
 */
class ReflectionAuditingBeanWrapperUnitTests {

	ConversionService conversionService;

	AnnotatedUser user;

	AuditableBeanWrapper<?> wrapper;

	LocalDateTime time = LocalDateTime.now();

	@BeforeEach
	void setUp() {
		this.conversionService = new DefaultAuditableBeanWrapperFactory().getConversionService();
		this.user = new AnnotatedUser();
		this.wrapper = new ReflectionAuditingBeanWrapper<>(this.conversionService, this.user);
	}

	@Test
	void setsDateTimeFieldCorrectly() {
		this.wrapper.setCreatedDate(this.time);
		assertThat(this.user.createdDate)
				.isEqualTo(new DateTime(LocalDateTimeToDateConverter.INSTANCE.convert(this.time)));
	}

	@Test
	void setsDateFieldCorrectly() {
		this.wrapper.setLastModifiedDate(this.time);
		assertThat(this.user.lastModifiedDate).isEqualTo(LocalDateTimeToDateConverter.INSTANCE.convert(this.time));
	}

	@Test
	void setsLongFieldCorrectly() {
		Sample sample = new Sample();
		AuditableBeanWrapper<Sample> wrapper = new ReflectionAuditingBeanWrapper<>(this.conversionService, sample);
		wrapper.setCreatedDate(this.time);
		assertThat(sample.createdDate).isEqualTo(this.time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
		wrapper.setLastModifiedDate(this.time);
		assertThat(sample.modifiedDate).isEqualTo(this.time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
	}

	@Test
	void setsAuditorFieldsCorrectly() {
		Object object = new Object();
		this.wrapper.setCreatedBy(object);
		assertThat(this.user.createdBy).isEqualTo(object);
		this.wrapper.setLastModifiedBy(object);
		assertThat(this.user.lastModifiedBy).isEqualTo(object);
	}

	class Sample {

		@CreatedDate
		Long createdDate;

		@LastModifiedDate
		long modifiedDate;

	}

}
