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
package org.springframework.data.history;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RevisionMetadata}.
 *
 * @author Oliver Gierke
 * @author Jens Schauder
 */
@ExtendWith(MockitoExtension.class)
class RevisionUnitTests {

	@Mock
	RevisionMetadata<Integer> firstMetadata, secondMetadata;

	@Test
	void comparesCorrectly() {
		when(this.firstMetadata.getRevisionNumber()).thenReturn(Optional.of(1));
		when(this.secondMetadata.getRevisionNumber()).thenReturn(Optional.of(2));
		Revision<Integer, Object> first = Revision.of(this.firstMetadata, new Object());
		Revision<Integer, Object> second = Revision.of(this.secondMetadata, new Object());
		List<Revision<Integer, Object>> revisions = Stream.of(second, first).sorted().collect(Collectors.toList());
		assertThat(revisions.get(0)).isEqualTo(first);
		assertThat(revisions.get(1)).isEqualTo(second);
	}

	@Test // DATACMNS-187
	void returnsRevisionNumber() {
		Optional<Integer> reference = Optional.of(4711);
		when(this.firstMetadata.getRevisionNumber()).thenReturn(reference);
		assertThat(Revision.of(this.firstMetadata, new Object()).getRevisionNumber()).isEqualTo(reference);
	}

	@Test // DATACMNS-1251
	void returnsRevisionInstant() {
		Optional<Instant> reference = Optional.of(Instant.now());
		when(this.firstMetadata.getRevisionInstant()).thenReturn(reference);
		assertThat(Revision.of(this.firstMetadata, new Object()).getRevisionInstant()).isEqualTo(reference);
	}

	@Test // DATACMNS-218
	void returnsRevisionMetadata() {
		assertThat(Revision.of(this.firstMetadata, new Object()).getMetadata()).isEqualTo(this.firstMetadata);
	}

}
