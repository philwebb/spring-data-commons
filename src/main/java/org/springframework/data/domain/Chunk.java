/*
 * Copyright 2014-2020 the original author or authors.
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

package org.springframework.data.domain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.util.Assert;

/**
 * A chunk of data restricted by the configured {@link Pageable}.
 *
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @since 1.8
 */
abstract class Chunk<T> implements Slice<T>, Serializable {

	private static final long serialVersionUID = 867755909294344406L;

	private final List<T> content = new ArrayList<>();

	private final Pageable pageable;

	/**
	 * Creates a new {@link Chunk} with the given content and the given governing
	 * {@link Pageable}.
	 * @param content must not be {@literal null}.
	 * @param pageable must not be {@literal null}.
	 */
	Chunk(List<T> content, Pageable pageable) {
		Assert.notNull(content, "Content must not be null!");
		Assert.notNull(pageable, "Pageable must not be null!");
		this.content.addAll(content);
		this.pageable = pageable;
	}

	@Override
	public int getNumber() {
		return this.pageable.isPaged() ? this.pageable.getPageNumber() : 0;
	}

	@Override
	public int getSize() {
		return this.pageable.isPaged() ? this.pageable.getPageSize() : this.content.size();
	}

	@Override
	public int getNumberOfElements() {
		return this.content.size();
	}

	@Override
	public boolean hasPrevious() {
		return getNumber() > 0;
	}

	@Override
	public boolean isFirst() {
		return !hasPrevious();
	}

	@Override
	public boolean isLast() {
		return !hasNext();
	}

	@Override
	public Pageable nextPageable() {
		return hasNext() ? this.pageable.next() : Pageable.unpaged();
	}

	@Override
	public Pageable previousPageable() {
		return hasPrevious() ? this.pageable.previousOrFirst() : Pageable.unpaged();
	}

	@Override
	public boolean hasContent() {
		return !this.content.isEmpty();
	}

	@Override
	public List<T> getContent() {
		return Collections.unmodifiableList(this.content);
	}

	@Override
	public Pageable getPageable() {
		return this.pageable;
	}

	@Override
	public Sort getSort() {
		return this.pageable.getSort();
	}

	@Override
	public Iterator<T> iterator() {
		return this.content.iterator();
	}

	/**
	 * Applies the given {@link Function} to the content of the {@link Chunk}.
	 * @param converter must not be {@literal null}.
	 * @return
	 */
	protected <U> List<U> getConvertedContent(Function<? super T, ? extends U> converter) {
		Assert.notNull(converter, "Function must not be null!");
		return this.stream().map(converter::apply).collect(Collectors.toList());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof Chunk<?>)) {
			return false;
		}
		Chunk<?> other = (Chunk<?>) obj;
		boolean contentEqual = this.content.equals(other.content);
		boolean pageableEqual = this.pageable.equals(other.pageable);
		return contentEqual && pageableEqual;
	}

	@Override
	public int hashCode() {
		int result = 17;
		result += 31 * this.pageable.hashCode();
		result += 31 * this.content.hashCode();
		return result;
	}

}
