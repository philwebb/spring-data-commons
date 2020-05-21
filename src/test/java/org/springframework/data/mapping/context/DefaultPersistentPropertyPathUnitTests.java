/*
 * Copyright 2011-2020 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PersistentPropertyPath;

/**
 * Unit tests for {@link DefaultPersistentPropertyPath}.
 *
 * @author Oliver Gierke
 */
@ExtendWith(MockitoExtension.class)
class DefaultPersistentPropertyPathUnitTests<P extends PersistentProperty<P>> {

	@Mock
	P first, second;

	@Mock
	Converter<P, String> converter;

	PersistentPropertyPath<P> oneLeg;

	PersistentPropertyPath<P> twoLegs;

	@BeforeEach
	void setUp() {
		this.oneLeg = new DefaultPersistentPropertyPath<>(Collections.singletonList(this.first));
		this.twoLegs = new DefaultPersistentPropertyPath<>(Arrays.asList(this.first, this.second));
	}

	@Test
	void rejectsNullProperties() {
		assertThatIllegalArgumentException().isThrownBy(() -> new DefaultPersistentPropertyPath<>(null));
	}

	@Test
	void usesPropertyNameForSimpleDotPath() {

		when(this.first.getName()).thenReturn("foo");
		when(this.second.getName()).thenReturn("bar");

		assertThat(this.twoLegs.toDotPath()).isEqualTo("foo.bar");
	}

	@Test
	void usesConverterToCreatePropertyPath() {

		when(this.converter.convert(any())).thenReturn("foo");

		assertThat(this.twoLegs.toDotPath(this.converter)).isEqualTo("foo.foo");
	}

	@Test
	void returnsCorrectLeafProperty() {

		assertThat(this.twoLegs.getLeafProperty()).isEqualTo(this.second);
		assertThat(this.oneLeg.getLeafProperty()).isEqualTo(this.first);
	}

	@Test
	void returnsCorrectBaseProperty() {

		assertThat(this.twoLegs.getBaseProperty()).isEqualTo(this.first);
		assertThat(this.oneLeg.getBaseProperty()).isEqualTo(this.first);
	}

	@Test
	void detectsBasePathCorrectly() {

		assertThat(this.oneLeg.isBasePathOf(this.twoLegs)).isTrue();
		assertThat(this.twoLegs.isBasePathOf(this.oneLeg)).isFalse();
	}

	@Test
	void calculatesExtensionCorrectly() {

		PersistentPropertyPath<P> extension = this.twoLegs.getExtensionForBaseOf(this.oneLeg);

		assertThat(extension).isEqualTo(new DefaultPersistentPropertyPath<>(Collections.singletonList(this.second)));
	}

	@Test
	void returnsTheCorrectParentPath() {
		assertThat(this.twoLegs.getParentPath()).isEqualTo(this.oneLeg);
	}

	@Test
	void returnsEmptyPathForRootLevelProperty() {
		assertThat(this.oneLeg.getParentPath()).isEmpty();
	}

	@Test
	void returnItselfForEmptyPath() {

		PersistentPropertyPath<P> parent = this.oneLeg.getParentPath();
		PersistentPropertyPath<P> parentsParent = parent.getParentPath();

		assertThat(parentsParent).isEmpty();
		assertThat(parentsParent).isSameAs(parent);
	}

	@Test
	void pathReturnsCorrectSize() {
		assertThat(this.oneLeg.getLength()).isEqualTo(1);
		assertThat(this.twoLegs.getLength()).isEqualTo(2);
	}

	@Test // DATACMNS-444
	void skipsMappedPropertyNameIfConverterReturnsNull() {
		assertThat(this.twoLegs.toDotPath(source -> null)).isNull();
	}

	@Test // DATACMNS-444
	void skipsMappedPropertyNameIfConverterReturnsEmptyStrings() {
		assertThat(this.twoLegs.toDotPath(source -> "")).isNull();
	}

	@Test // DATACMNS-1466
	void returnsNullForLeafPropertyOnEmptyPath() {

		PersistentPropertyPath<P> path = new DefaultPersistentPropertyPath<P>(Collections.emptyList());

		assertThat(path.getLeafProperty()).isNull();
	}

	@Test // DATACMNS-1466
	void returnsNullForBasePropertyOnEmptyPath() {

		PersistentPropertyPath<P> path = new DefaultPersistentPropertyPath<P>(Collections.emptyList());

		assertThat(path.getBaseProperty()).isNull();
	}

}
