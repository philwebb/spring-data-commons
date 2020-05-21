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
package org.springframework.data.repository.query;

import java.util.Iterator;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.util.QueryExecutionConverters;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@link ParameterAccessor} implementation using a {@link Parameters} instance to find
 * special parameters.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 */
public class ParametersParameterAccessor implements ParameterAccessor {

	private final Parameters<?, ?> parameters;

	private final Object[] values;

	/**
	 * Creates a new {@link ParametersParameterAccessor}.
	 * @param parameters must not be {@literal null}.
	 * @param values must not be {@literal null}.
	 */
	public ParametersParameterAccessor(Parameters<?, ?> parameters, Object[] values) {
		Assert.notNull(parameters, "Parameters must not be null!");
		Assert.notNull(values, "Values must not be null!");
		Assert.isTrue(parameters.getNumberOfParameters() == values.length, "Invalid number of parameters given!");
		this.parameters = parameters;
		if (requiresUnwrapping(values)) {
			this.values = new Object[values.length];
			for (int i = 0; i < values.length; i++) {
				this.values[i] = QueryExecutionConverters.unwrap(values[i]);
			}
		}
		else {
			this.values = values;
		}
	}

	private static boolean requiresUnwrapping(Object[] values) {
		for (Object value : values) {
			if (value != null && QueryExecutionConverters.supports(value.getClass())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns the {@link Parameters} instance backing the accessor.
	 * @return the parameters will never be {@literal null}.
	 */
	public Parameters<?, ?> getParameters() {
		return this.parameters;
	}

	/**
	 * Returns the potentially unwrapped values.
	 * @return
	 */
	protected Object[] getValues() {
		return this.values;
	}

	@Override
	public Pageable getPageable() {
		if (!this.parameters.hasPageableParameter()) {
			return Pageable.unpaged();
		}
		Pageable pageable = (Pageable) this.values[this.parameters.getPageableIndex()];
		return pageable == null ? Pageable.unpaged() : pageable;
	}

	@Override
	public Sort getSort() {
		if (this.parameters.hasSortParameter()) {
			Sort sort = (Sort) this.values[this.parameters.getSortIndex()];
			return sort == null ? Sort.unsorted() : sort;
		}
		if (this.parameters.hasPageableParameter()) {
			return getPageable().getSort();
		}
		return Sort.unsorted();
	}

	/**
	 * Returns the dynamic projection type if available, {@literal null} otherwise.
	 * @return
	 */
	@Override
	public Optional<Class<?>> getDynamicProjection() {
		return Optional.ofNullable(this.parameters.hasDynamicProjection()
				? (Class<?>) this.values[this.parameters.getDynamicProjectionIndex()] : null);
	}

	/**
	 * Returns the dynamic projection type if available, {@literal null} otherwise.
	 * @return
	 */
	@Override
	@Nullable
	public Class<?> findDynamicProjection() {
		return this.parameters.hasDynamicProjection()
				? (Class<?>) this.values[this.parameters.getDynamicProjectionIndex()] : null;
	}

	/**
	 * Returns the value with the given index.
	 * @param index
	 * @return
	 */
	@SuppressWarnings("unchecked")
	protected <T> T getValue(int index) {
		return (T) this.values[index];
	}

	@Override
	public Object getBindableValue(int index) {
		return this.values[this.parameters.getBindableParameter(index).getIndex()];
	}

	@Override
	public boolean hasBindableNullValue() {
		for (Parameter parameter : this.parameters.getBindableParameters()) {
			if (this.values[parameter.getIndex()] == null) {
				return true;
			}
		}
		return false;
	}

	@Override
	public BindableParameterIterator iterator() {
		return new BindableParameterIterator(this);
	}

	/**
	 * Iterator class to allow traversing all bindable parameters inside the accessor.
	 */
	private static class BindableParameterIterator implements Iterator<Object> {

		private final int bindableParameterCount;

		private final ParameterAccessor accessor;

		private int currentIndex = 0;

		/**
		 * Creates a new {@link BindableParameterIterator}.
		 * @param accessor must not be {@literal null}.
		 */
		BindableParameterIterator(ParametersParameterAccessor accessor) {
			Assert.notNull(accessor, "ParametersParameterAccessor must not be null!");
			this.accessor = accessor;
			this.bindableParameterCount = accessor.getParameters().getBindableParameters().getNumberOfParameters();
		}

		/**
		 * Returns the next bindable parameter.
		 * @return
		 */
		@Override
		public Object next() {
			return this.accessor.getBindableValue(this.currentIndex++);
		}

		@Override
		public boolean hasNext() {
			return this.bindableParameterCount > this.currentIndex;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

	}

}
