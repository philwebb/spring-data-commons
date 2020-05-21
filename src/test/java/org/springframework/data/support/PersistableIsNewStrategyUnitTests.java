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
package org.springframework.data.support;

import org.junit.jupiter.api.Test;

import org.springframework.data.domain.Persistable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link PersistableIsNewStrategy}.
 *
 * @author Oliver Gierke
 */
class PersistableIsNewStrategyUnitTests {

	IsNewStrategy strategy = PersistableIsNewStrategy.INSTANCE;

	@Test
	void invokesPersistableIsNewForTest() {

		PersistableEntity entity = new PersistableEntity();
		assertThat(this.strategy.isNew(entity)).isTrue();

		entity.isNew = false;
		assertThat(this.strategy.isNew(entity)).isFalse();
	}

	@Test
	void rejectsNonPersistableEntity() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.strategy.isNew(new Object()));
	}

	@SuppressWarnings("serial")
	static class PersistableEntity implements Persistable<Long> {

		boolean isNew = true;

		@Override
		public Long getId() {
			return null;
		}

		@Override
		public boolean isNew() {
			return this.isNew;
		}

	}

}
