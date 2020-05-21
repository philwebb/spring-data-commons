/*
 * Copyright 2015-2020 the original author or authors.
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

package org.springframework.data.util;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Spring Data specific Java {@link Stream} utility methods and classes.
 *
 * @author Thomas Darimont
 * @since 1.10
 */
public class StreamUtilsTests {

	@Test // DATACMNS-650
	public void shouldConvertAnIteratorToAStream() {
		List<String> input = Arrays.asList("a", "b", "c");
		Stream<String> stream = StreamUtils.createStreamFromIterator(input.iterator());
		List<String> output = stream.collect(Collectors.toList());
		assertThat(input).isEqualTo(output);
	}

}
