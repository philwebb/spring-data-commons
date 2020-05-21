/*
 * Copyright 2013-2020 the original author or authors.
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
package org.springframework.data.querydsl;

import org.junit.jupiter.api.Test;

import org.springframework.data.domain.AbstractPageRequest;
import org.springframework.data.domain.AbstractPageRequestUnitTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Thomas Darimont
 */
public class QPageRequestUnitTests extends AbstractPageRequestUnitTests {

	@Override
	public AbstractPageRequest newPageRequest(int page, int size) {
		return QPageRequest.of(page, size);
	}

	@Test
	void constructsQPageRequestWithOrderSpecifiers() {
		QUser user = QUser.user;
		QPageRequest pageRequest = QPageRequest.of(0, 10, user.firstname.asc());
		assertThat(pageRequest.getSort()).isEqualTo(QSort.by(user.firstname.asc()));
	}

	@Test
	void constructsQPageRequestWithQSort() {
		QUser user = QUser.user;
		QPageRequest pageRequest = QPageRequest.of(0, 10, QSort.by(user.firstname.asc()));
		assertThat(pageRequest.getSort()).isEqualTo(QSort.by(user.firstname.asc()));
	}

	@Test // DATACMNS-1581
	void rejectsNullSort() {
		assertThatExceptionOfType(IllegalArgumentException.class)
				.isThrownBy(() -> QPageRequest.of(0, 10, (QSort) null));
	}

}
