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

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

import org.springframework.data.domain.Range;
import org.springframework.data.domain.Range.Bound;
import org.springframework.util.SerializationUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link Distance}.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Mark Paluch
 */
class DistanceUnitTests {

	private static final Offset<Double> EPS = Offset.offset(0.000000001);

	private static final double TEN_MILES_NORMALIZED = 0.002523219294755161;

	private static final double TEN_KM_NORMALIZED = 0.001567855942887398;

	@Test // DATACMNS-437
	void defaultsMetricToNeutralOne() {
		assertThat(new Distance(2.5).getMetric()).isEqualTo(Metrics.NEUTRAL);
	}

	@Test // DATACMNS-437
	void addsDistancesWithoutExplicitMetric() {
		Distance left = new Distance(2.5, Metrics.KILOMETERS);
		Distance right = new Distance(2.5, Metrics.KILOMETERS);
		assertThat(left.add(right)).isEqualTo(new Distance(5.0, Metrics.KILOMETERS));
	}

	@Test // DATACMNS-437
	void addsDistancesWithExplicitMetric() {
		Distance left = new Distance(2.5, Metrics.KILOMETERS);
		Distance right = new Distance(2.5, Metrics.KILOMETERS);
		assertThat(left.add(right, Metrics.MILES)).isEqualTo(new Distance(3.106856281073925, Metrics.MILES));
	}

	@Test // DATACMNS-474
	void distanceWithSameMetricShoudEqualAfterConversion() {
		assertThat(new Distance(1).in(Metrics.NEUTRAL)).isEqualTo(new Distance(1));
		assertThat(new Distance(TEN_KM_NORMALIZED).in(Metrics.KILOMETERS))
				.isEqualTo(new Distance(10, Metrics.KILOMETERS));
		assertThat(new Distance(TEN_MILES_NORMALIZED).in(Metrics.MILES)).isEqualTo(new Distance(10, Metrics.MILES));
	}

	@Test // DATACMNS-474
	void distanceWithDifferentMetricShoudEqualAfterConversion() {
		assertThat(new Distance(10, Metrics.MILES)).isEqualTo(new Distance(TEN_MILES_NORMALIZED).in(Metrics.MILES));
		assertThat(new Distance(10, Metrics.KILOMETERS))
				.isEqualTo(new Distance(TEN_KM_NORMALIZED).in(Metrics.KILOMETERS));
	}

	@Test // DATACMNS-474
	void conversionShouldProduceCorrectNormalizedValue() {
		assertThat(new Distance(TEN_KM_NORMALIZED, Metrics.NEUTRAL).in(Metrics.KILOMETERS).getNormalizedValue())
				.isCloseTo(new Distance(10, Metrics.KILOMETERS).getNormalizedValue(), EPS);
		assertThat(new Distance(TEN_KM_NORMALIZED).in(Metrics.KILOMETERS).getNormalizedValue())
				.isCloseTo(new Distance(10, Metrics.KILOMETERS).getNormalizedValue(), EPS);
		assertThat(new Distance(TEN_MILES_NORMALIZED).in(Metrics.MILES).getNormalizedValue())
				.isCloseTo(new Distance(10, Metrics.MILES).getNormalizedValue(), EPS);
		assertThat(new Distance(TEN_MILES_NORMALIZED).in(Metrics.MILES).getNormalizedValue())
				.isCloseTo(new Distance(16.09344, Metrics.KILOMETERS).getNormalizedValue(), EPS);
		assertThat(new Distance(TEN_MILES_NORMALIZED).in(Metrics.KILOMETERS).getNormalizedValue())
				.isCloseTo(new Distance(10, Metrics.MILES).getNormalizedValue(), EPS);
		assertThat(new Distance(10, Metrics.KILOMETERS).in(Metrics.MILES).getNormalizedValue())
				.isCloseTo(new Distance(6.21371192, Metrics.MILES).getNormalizedValue(), EPS);
	}

	@Test // DATACMNS-474
	void toStringAfterConversion() {
		assertThat(new Distance(10, Metrics.KILOMETERS).in(Metrics.MILES).toString())
				.isEqualTo(new Distance(6.21371256214785, Metrics.MILES).toString());
		assertThat(new Distance(6.21371256214785, Metrics.MILES).in(Metrics.KILOMETERS).toString())
				.isEqualTo(new Distance(10, Metrics.KILOMETERS).toString());
	}

	@Test // DATACMNS-482
	void testSerialization() {
		Distance dist = new Distance(10, Metrics.KILOMETERS);
		Distance serialized = (Distance) SerializationUtils.deserialize(SerializationUtils.serialize(dist));
		assertThat(serialized).isEqualTo(dist);
	}

	@Test // DATACMNS-626
	void returnsMetricsAbbreviationAsUnit() {
		assertThat(new Distance(10, Metrics.KILOMETERS).getUnit()).isEqualTo("km");
	}

	@Test // DATACMNS-651
	void createsARangeCorrectly() {
		Distance twoKilometers = new Distance(2, Metrics.KILOMETERS);
		Distance tenKilometers = new Distance(10, Metrics.KILOMETERS);
		Range<Distance> range = Distance.between(twoKilometers, tenKilometers);
		assertThat(range).isNotNull();
		assertThat(range.getLowerBound()).isEqualTo(Bound.inclusive(twoKilometers));
		assertThat(range.getUpperBound()).isEqualTo(Bound.inclusive(tenKilometers));
	}

	@Test // DATACMNS-651
	void createsARangeFromPiecesCorrectly() {
		Distance twoKilometers = new Distance(2, Metrics.KILOMETERS);
		Distance tenKilometers = new Distance(10, Metrics.KILOMETERS);
		Range<Distance> range = Distance.between(2, Metrics.KILOMETERS, 10, Metrics.KILOMETERS);
		assertThat(range).isNotNull();
		assertThat(range.getLowerBound()).isEqualTo(Bound.inclusive(twoKilometers));
		assertThat(range.getUpperBound()).isEqualTo(Bound.inclusive(tenKilometers));
	}

	@Test // DATACMNS-651
	void implementsComparableCorrectly() {
		Distance twoKilometers = new Distance(2, Metrics.KILOMETERS);
		Distance tenKilometers = new Distance(10, Metrics.KILOMETERS);
		Distance tenKilometersInMiles = new Distance(6.21371256214785, Metrics.MILES);
		assertThat(tenKilometers.compareTo(tenKilometers)).isEqualTo(0);
		assertThat(tenKilometers.compareTo(tenKilometersInMiles)).isEqualTo(0);
		assertThat(tenKilometersInMiles.compareTo(tenKilometers)).isEqualTo(0);
		assertThat(twoKilometers.compareTo(tenKilometers)).isLessThan(0);
		assertThat(tenKilometers.compareTo(twoKilometers)).isGreaterThan(0);
	}

}
