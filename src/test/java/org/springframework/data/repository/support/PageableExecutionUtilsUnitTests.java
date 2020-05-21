/*
 * Copyright 2016-2020 the original author or authors.
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

import java.util.Arrays;
import java.util.Collections;
import java.util.function.LongSupplier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Unit tests for {@link PageableExecutionUtils}.
 *
 * @author Mark Paluch
 * @author Oliver Gierke
 */
@ExtendWith(MockitoExtension.class)
class PageableExecutionUtilsUnitTests {

	@Mock
	LongSupplier totalSupplierMock;

	@Test // DATAMCNS-884
	void firstPageRequestIsLessThanOneFullPageDoesNotRequireTotal() {
		Page<Integer> page = PageableExecutionUtils.getPage(Arrays.asList(1, 2, 3), PageRequest.of(0, 10),
				this.totalSupplierMock);
		assertThat(page).contains(1, 2, 3);
		assertThat(page.getTotalElements()).isEqualTo(3L);
		verifyNoInteractions(this.totalSupplierMock);
	}

	@Test // DATAMCNS-884
	void noPageableRequestDoesNotRequireTotal() {
		Page<Integer> page = PageableExecutionUtils.getPage(Arrays.asList(1, 2, 3), Pageable.unpaged(),
				this.totalSupplierMock);
		assertThat(page).contains(1, 2, 3);
		assertThat(page.getTotalElements()).isEqualTo(3L);
		verifyNoInteractions(this.totalSupplierMock);
	}

	@Test // DATAMCNS-884
	void subsequentPageRequestIsLessThanOneFullPageDoesNotRequireTotal() {
		Page<Integer> page = PageableExecutionUtils.getPage(Arrays.asList(1, 2, 3), PageRequest.of(5, 10),
				this.totalSupplierMock);
		assertThat(page).contains(1, 2, 3);
		assertThat(page.getTotalElements()).isEqualTo(53L);
		verifyNoInteractions(this.totalSupplierMock);
	}

	@Test // DATAMCNS-884
	void firstPageRequestHitsUpperBoundRequiresTotal() {
		doReturn(4L).when(this.totalSupplierMock).getAsLong();
		Page<Integer> page = PageableExecutionUtils.getPage(Arrays.asList(1, 2, 3), PageRequest.of(0, 3),
				this.totalSupplierMock);
		assertThat(page).contains(1, 2, 3);
		assertThat(page.getTotalElements()).isEqualTo(4L);
		verify(this.totalSupplierMock).getAsLong();
	}

	@Test // DATAMCNS-884
	void subsequentPageRequestHitsUpperBoundRequiresTotal() {
		doReturn(7L).when(this.totalSupplierMock).getAsLong();
		Page<Integer> page = PageableExecutionUtils.getPage(Arrays.asList(1, 2, 3), PageRequest.of(1, 3),
				this.totalSupplierMock);
		assertThat(page).contains(1, 2, 3);
		assertThat(page.getTotalElements()).isEqualTo(7L);
		verify(this.totalSupplierMock).getAsLong();
	}

	@Test // DATAMCNS-884
	void subsequentPageRequestWithoutResultRequiresRequireTotal() {
		doReturn(7L).when(this.totalSupplierMock).getAsLong();
		Page<Integer> page = PageableExecutionUtils.getPage(Collections.<Integer>emptyList(), PageRequest.of(5, 10),
				this.totalSupplierMock);
		assertThat(page.getTotalElements()).isEqualTo(7L);
		verify(this.totalSupplierMock).getAsLong();
	}

}
