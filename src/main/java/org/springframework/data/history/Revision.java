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

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.util.Optionals;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;

/**
 * Wrapper to contain {@link RevisionMetadata} as well as the revisioned entity.
 *
 * @author Oliver Gierke
 * @author Philipp Huegelmeyer
 * @author Christoph Strobl
 * @author Jens Schauder
 */
public final class Revision<N extends Number & Comparable<N>, T> implements Comparable<Revision<N, ?>> {

	/**
	 * The {@link RevisionMetadata} for the current {@link Revision}.
	 */
	private final RevisionMetadata<N> metadata;

	/**
	 * The underlying entity.
	 */

	private final T entity;

	private Revision(RevisionMetadata<N> metadata, T entity) {
		this.metadata = metadata;
		this.entity = entity;
	}

	/**
	 * Returns the revision number of the revision.
	 * @return the revision number.
	 */
	public Optional<N> getRevisionNumber() {
		return this.metadata.getRevisionNumber();
	}

	/**
	 * Returns the revision number of the revision, immediately failing on absence.
	 * @return the revision number.
	 */
	public N getRequiredRevisionNumber() {
		return this.metadata.getRequiredRevisionNumber();
	}

	/**
	 * Returns the timestamp of the revision.
	 * @return Guaranteed to be not {@literal null}.
	 */
	public Optional<Instant> getRevisionInstant() {
		return this.metadata.getRevisionInstant();
	}

	/**
	 * Returns the timestamp of the revision, immediately failing on absence.
	 * @return the revision {@link Instant}. May be {@literal null}.
	 */
	public Instant getRequiredRevisionInstant() {
		return this.metadata.getRequiredRevisionInstant();
	}

	@Override
	public int compareTo(@Nullable Revision<N, ?> other) {
		if (other == null) {
			return 1;
		}
		return Optionals.mapIfAllPresent(getRevisionNumber(), other.getRevisionNumber(), Comparable::compareTo)
				.orElse(-1);
	}

	public RevisionMetadata<N> getMetadata() {
		return this.metadata;
	}

	public T getEntity() {
		return this.entity;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof Revision)) {
			return false;
		}
		Revision<?, ?> other = (Revision<?, ?>) obj;
		boolean result = true;
		result = result && ObjectUtils.nullSafeEquals(this.metadata, other.metadata);
		result = result && ObjectUtils.nullSafeEquals(this.entity, other.entity);
		return result;
	}

	@Override
	public int hashCode() {
		int result = ObjectUtils.nullSafeHashCode(this.metadata);
		result = 31 * result + ObjectUtils.nullSafeHashCode(this.entity);
		return result;
	}

	@Override
	public String toString() {
		return String.format("Revision %s of entity %s - Revision metadata %s",
				getRevisionNumber().map(Object::toString).orElse("<unknown>"), this.entity, this.metadata);
	}

	/**
	 * Creates a new {@link Revision} for the given {@link RevisionMetadata} and entity.
	 * @param metadata must not be {@literal null}.
	 * @param entity must not be {@literal null}.
	 * @return
	 */
	public static <N extends Number & Comparable<N>, T> Revision<N, T> of(RevisionMetadata<N> metadata, T entity) {
		return new Revision<>(metadata, entity);
	}

}
