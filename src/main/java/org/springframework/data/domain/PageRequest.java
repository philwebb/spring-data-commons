/*
 * Copyright 2008-2020 the original author or authors.
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

import org.springframework.data.domain.Sort.Direction;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Basic Java Bean implementation of {@code Pageable}.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 */
public class PageRequest extends AbstractPageRequest {

	private static final long serialVersionUID = -4541509938956089562L;

	private final Sort sort;

	/**
	 * Creates a new {@link PageRequest} with sort parameters applied.
	 * @param page zero-based page index, must not be negative.
	 * @param size the size of the page to be returned, must be greater than 0.
	 * @param sort must not be {@literal null}, use {@link Sort#unsorted()} instead.
	 */
	protected PageRequest(int page, int size, Sort sort) {
		super(page, size);
		Assert.notNull(sort, "Sort must not be null!");
		this.sort = sort;
	}

	/**
	 * Creates a new unsorted {@link PageRequest}.
	 * @param page zero-based page index, must not be negative.
	 * @param size the size of the page to be returned, must be greater than 0.
	 * @since 2.0
	 */
	public static PageRequest of(int page, int size) {
		return of(page, size, Sort.unsorted());
	}

	/**
	 * Creates a new {@link PageRequest} with sort parameters applied.
	 * @param page zero-based page index.
	 * @param size the size of the page to be returned.
	 * @param sort must not be {@literal null}, use {@link Sort#unsorted()} instead.
	 * @since 2.0
	 */
	public static PageRequest of(int page, int size, Sort sort) {
		return new PageRequest(page, size, sort);
	}

	/**
	 * Creates a new {@link PageRequest} with sort direction and properties applied.
	 * @param page zero-based page index, must not be negative.
	 * @param size the size of the page to be returned, must be greater than 0.
	 * @param direction must not be {@literal null}.
	 * @param properties must not be {@literal null}.
	 * @since 2.0
	 */
	public static PageRequest of(int page, int size, Direction direction, String... properties) {
		return of(page, size, Sort.by(direction, properties));
	}

	@Override
	public Sort getSort() {
		return this.sort;
	}

	@Override
	public Pageable next() {
		return new PageRequest(getPageNumber() + 1, getPageSize(), getSort());
	}

	@Override
	public PageRequest previous() {
		return getPageNumber() == 0 ? this : new PageRequest(getPageNumber() - 1, getPageSize(), getSort());
	}

	@Override
	public Pageable first() {
		return new PageRequest(0, getPageSize(), getSort());
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof PageRequest)) {
			return false;
		}
		PageRequest that = (PageRequest) obj;
		return super.equals(that) && this.sort.equals(that.sort);
	}

	@Override
	public int hashCode() {
		return 31 * super.hashCode() + this.sort.hashCode();
	}

	@Override
	public String toString() {
		return String.format("Page request [number: %d, size %d, sort: %s]", getPageNumber(), getPageSize(), this.sort);
	}

}
