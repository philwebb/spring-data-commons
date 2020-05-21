/*
 * Copyright 2013-2020 the original author or authors.
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

import org.springframework.util.Assert;

/**
 * Abstract Java Bean implementation of {@code Pageable}.
 *
 * @author Thomas Darimont
 * @author Oliver Gierke
 * @author Alex Bondarev
 */
public abstract class AbstractPageRequest implements Pageable, Serializable {

	private static final long serialVersionUID = 1232825578694716871L;

	private final int page;

	private final int size;

	/**
	 * Creates a new {@link AbstractPageRequest}. Pages are zero indexed, thus providing 0
	 * for {@code page} will return the first page.
	 * @param page must not be less than zero.
	 * @param size must not be less than one.
	 */
	public AbstractPageRequest(int page, int size) {
		Assert.isTrue(page >= 0, "Page index must not be less than zero!");
		Assert.isTrue(size >= 1, "Page size must not be less than one!");
		this.page = page;
		this.size = size;
	}

	@Override
	public int getPageSize() {
		return this.size;
	}

	@Override
	public int getPageNumber() {
		return this.page;
	}

	@Override
	public long getOffset() {
		return (long) this.page * (long) this.size;
	}

	@Override
	public boolean hasPrevious() {
		return this.page > 0;
	}

	@Override
	public Pageable previousOrFirst() {
		return hasPrevious() ? previous() : first();
	}

	@Override
	public abstract Pageable next();

	/**
	 * Returns the {@link Pageable} requesting the previous {@link Page}.
	 * @return
	 */
	public abstract Pageable previous();

	@Override
	public abstract Pageable first();

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		AbstractPageRequest other = (AbstractPageRequest) obj;
		return this.page == other.page && this.size == other.size;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + this.page;
		result = prime * result + this.size;
		return result;
	}

}
