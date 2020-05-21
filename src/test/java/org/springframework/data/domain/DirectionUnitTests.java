package org.springframework.data.domain;

import org.junit.jupiter.api.Test;

import org.springframework.data.domain.Sort.Direction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit test for {@link Direction}.
 *
 * @author Oliver Gierke
 */
class DirectionUnitTests {

	@Test
	void jpaValueMapping() throws Exception {

		assertThat(Direction.fromString("asc")).isEqualTo(Direction.ASC);
		assertThat(Direction.fromString("desc")).isEqualTo(Direction.DESC);
	}

	@Test
	void rejectsInvalidString() {
		assertThatIllegalArgumentException().isThrownBy(() -> Direction.fromString("foo"));
	}

}
