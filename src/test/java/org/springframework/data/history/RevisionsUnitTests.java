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

import java.util.Arrays;
import java.util.Iterator;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * Unit tests for {@link Revisions}.
 *
 * @author Oliver Gierke
 */
@ExtendWith(MockitoExtension.class)
class RevisionsUnitTests {

	@Mock
	RevisionMetadata<Integer> first;

	@Mock
	RevisionMetadata<Integer> second;

	Revision<Integer, Object> firstRevision;

	Revision<Integer, Object> secondRevision;

	@BeforeEach
	void setUp() {
		given(this.first.getRevisionNumber()).willReturn(Optional.of(0));
		given(this.second.getRevisionNumber()).willReturn(Optional.of(10));
		this.firstRevision = Revision.of(this.first, new Object());
		this.secondRevision = Revision.of(this.second, new Object());
	}

	@Test
	void returnsCorrectLatestRevision() {
		assertThat(Revisions.of(Arrays.asList(this.firstRevision, this.secondRevision)).getLatestRevision())
				.isEqualTo(this.secondRevision);
	}

	@Test
	void iteratesInCorrectOrder() {
		Revisions<Integer, Object> revisions = Revisions.of(Arrays.asList(this.firstRevision, this.secondRevision));
		Iterator<Revision<Integer, Object>> iterator = revisions.iterator();
		assertThat(iterator.hasNext()).isTrue();
		assertThat(iterator.next()).isEqualTo(this.firstRevision);
		assertThat(iterator.hasNext()).isTrue();
		assertThat(iterator.next()).isEqualTo(this.secondRevision);
		assertThat(iterator.hasNext()).isFalse();
	}

	@Test
	void reversedRevisionsStillReturnsCorrectLatestRevision() {
		assertThat(Revisions.of(Arrays.asList(this.firstRevision, this.secondRevision)).reverse().getLatestRevision())
				.isEqualTo(this.secondRevision);
	}

	@Test
	void iteratesReversedRevisionsInCorrectOrder() {
		Revisions<Integer, Object> revisions = Revisions.of(Arrays.asList(this.firstRevision, this.secondRevision));
		Iterator<Revision<Integer, Object>> iterator = revisions.reverse().iterator();
		assertThat(iterator.hasNext()).isTrue();
		assertThat(iterator.next()).isEqualTo(this.secondRevision);
		assertThat(iterator.hasNext()).isTrue();
		assertThat(iterator.next()).isEqualTo(this.firstRevision);
		assertThat(iterator.hasNext()).isFalse();
	}

	@Test
	void forcesInvalidlyOrderedRevisionsToBeOrdered() {
		Revisions<Integer, Object> revisions = Revisions.of(Arrays.asList(this.secondRevision, this.firstRevision));
		Iterator<Revision<Integer, Object>> iterator = revisions.iterator();
		assertThat(iterator.hasNext()).isTrue();
		assertThat(iterator.next()).isEqualTo(this.firstRevision);
		assertThat(iterator.hasNext()).isTrue();
		assertThat(iterator.next()).isEqualTo(this.secondRevision);
		assertThat(iterator.hasNext()).isFalse();
	}

}
