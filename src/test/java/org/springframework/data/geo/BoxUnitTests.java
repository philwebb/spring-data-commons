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
package org.springframework.data.geo;

import org.junit.jupiter.api.Test;

import org.springframework.util.SerializationUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link Box}.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 */
class BoxUnitTests {

	Box first = new Box(new Point(1d, 1d), new Point(2d, 2d));

	Box second = new Box(new Point(1d, 1d), new Point(2d, 2d));

	Box third = new Box(new Point(3d, 3d), new Point(1d, 1d));

	@Test // DATACMNS-437
	void equalsWorksCorrectly() {
		assertThat(this.first.equals(this.second)).isTrue();
		assertThat(this.second.equals(this.first)).isTrue();
		assertThat(this.first.equals(this.third)).isFalse();
	}

	@Test // DATACMNS-437
	void hashCodeWorksCorrectly() {
		assertThat(this.first.hashCode()).isEqualTo(this.second.hashCode());
		assertThat(this.first.hashCode()).isNotEqualTo(this.third.hashCode());
	}

	@Test // DATACMNS-437
	void testToString() {
		assertThat(this.first.toString())
				.isEqualTo("Box [Point [x=1.000000, y=1.000000], Point [x=2.000000, y=2.000000]]");
	}

	@Test // DATACMNS-482
	void testSerialization() {

		Box serialized = (Box) SerializationUtils.deserialize(SerializationUtils.serialize(this.first));
		assertThat(serialized).isEqualTo(this.first);
	}

}
