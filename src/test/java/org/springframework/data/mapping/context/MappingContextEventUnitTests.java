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
package org.springframework.data.mapping.context;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link MappingContextEvent}.
 *
 * @author Oliver Gierke
 */
@ExtendWith(MockitoExtension.class)
class MappingContextEventUnitTests<E extends PersistentEntity<?, P>, P extends PersistentProperty<P>> {

	@Mock
	E entity;

	@Mock
	MappingContext<?, ?> mappingContext, otherMappingContext;

	@Test
	void returnsPersistentEntityHandedToTheEvent() {
		MappingContextEvent<E, P> event = new MappingContextEvent<>(this.mappingContext, this.entity);
		assertThat(event.getPersistentEntity()).isEqualTo(this.entity);
	}

	@Test
	void usesMappingContextAsEventSource() {
		MappingContextEvent<E, P> event = new MappingContextEvent<>(this.mappingContext, this.entity);
		assertThat(event.getSource()).isEqualTo(this.mappingContext);
	}

	@Test
	void detectsEmittingMappingContextCorrectly() {
		MappingContextEvent<E, P> event = new MappingContextEvent<>(this.mappingContext, this.entity);
		assertThat(event.wasEmittedBy(this.mappingContext)).isTrue();
		assertThat(event.wasEmittedBy(this.otherMappingContext)).isFalse();
	}

}
