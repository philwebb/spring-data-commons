/*
 * Copyright 2017-2020 the original author or authors.
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
package org.springframework.data.history;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Reference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Unit tests for {@link AnnotationRevisionMetadata}.
 *
 * @author Oliver Gierke
 * @author Jens Schauder
 */
class AnnotationRevisionMetadataUnitTests {

	SoftAssertions softly = new SoftAssertions();

	@Test // DATACMNS-1173
	void exposesNoInformationOnEmptyProbe() {

		Sample sample = new Sample();
		RevisionMetadata<Long> metadata = getMetadata(sample);

		assertThat(metadata.getRevisionNumber()).isEmpty();

		assertThatExceptionOfType(IllegalStateException.class).isThrownBy(metadata::getRequiredRevisionNumber);

		assertThatExceptionOfType(IllegalStateException.class).isThrownBy(metadata::getRequiredRevisionInstant);

	}

	@Test // DATACMNS-1173
	void exposesRevisionNumber() {

		Sample sample = new Sample();
		sample.revisionNumber = 1L;

		RevisionMetadata<Long> metadata = getMetadata(sample);

		this.softly.assertThat(metadata.getRevisionNumber()).hasValue(1L);
		this.softly.assertThat(metadata.getRequiredRevisionNumber()).isEqualTo(1L);

		this.softly.assertAll();
	}

	@Test // DATACMNS-1173
	void exposesRevisionDateAndInstantForLocalDateTime() {

		Sample sample = new Sample();
		sample.revisionDate = LocalDateTime.now();
		Instant expectedInstant = sample.revisionDate.atZone(ZoneId.systemDefault()).toInstant();

		RevisionMetadata<Long> metadata = getMetadata(sample);

		this.softly.assertThat(metadata.getRevisionInstant()).hasValue(expectedInstant);
		this.softly.assertThat(metadata.getRequiredRevisionInstant()).isEqualTo(expectedInstant);

		this.softly.assertAll();
	}

	@Test // DATACMNS-1251
	void exposesRevisionDateAndInstantForInstant() {

		SampleWithInstant sample = new SampleWithInstant();
		sample.revisionInstant = Instant.now();
		LocalDateTime expectedLocalDateTime = LocalDateTime.ofInstant(sample.revisionInstant, ZoneId.systemDefault());

		RevisionMetadata<Long> metadata = getMetadata(sample);

		this.softly.assertThat(metadata.getRevisionInstant()).hasValue(sample.revisionInstant);
		this.softly.assertThat(metadata.getRequiredRevisionInstant()).isEqualTo(sample.revisionInstant);

		this.softly.assertAll();
	}

	@Test // DATACMNS-1290
	void exposesRevisionDateAndInstantForLong() {

		SampleWithLong sample = new SampleWithLong();
		sample.revisionLong = 4711L;

		Instant expectedInstant = Instant.ofEpochMilli(sample.revisionLong);
		LocalDateTime expectedLocalDateTime = LocalDateTime.ofInstant(expectedInstant, ZoneId.systemDefault());

		RevisionMetadata<Long> metadata = getMetadata(sample);

		this.softly.assertThat(metadata.getRevisionInstant()).hasValue(expectedInstant);
		this.softly.assertThat(metadata.getRequiredRevisionInstant()).isEqualTo(expectedInstant);

		this.softly.assertAll();
	}

	@Test // DATACMNS-1384
	void supportsTimestampRevisionInstant() {

		SampleWithTimestamp sample = new SampleWithTimestamp();
		Instant now = Instant.now();
		sample.revision = Timestamp.from(now);

		RevisionMetadata<Long> metadata = getMetadata(sample);

		assertThat(metadata.getRequiredRevisionInstant()).isEqualTo(now);
	}

	@Test // DATACMNS-1384
	void supportsDateRevisionInstant() {

		SampleWithDate sample = new SampleWithDate();
		Date date = new Date();
		sample.revision = date;

		RevisionMetadata<Long> metadata = getMetadata(sample);

		assertThat(metadata.getRequiredRevisionInstant()).isEqualTo(date.toInstant());
	}

	private static RevisionMetadata<Long> getMetadata(Object sample) {
		return new AnnotationRevisionMetadata<>(sample, Autowired.class, Reference.class);
	}

	static class Sample {

		@Autowired
		Long revisionNumber;

		@Reference
		LocalDateTime revisionDate;

	}

	static class SampleWithInstant {

		@Autowired
		Long revisionNumber;

		@Reference
		Instant revisionInstant;

	}

	static class SampleWithLong {

		@Autowired
		Long revisionNumber;

		@Reference
		long revisionLong;

	}

	// DATACMNS-1384

	static class SampleWithTimestamp {

		@Reference
		Timestamp revision;

	}

	static class SampleWithDate {

		@Reference
		Date revision;

	}

}
