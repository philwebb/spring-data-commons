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

package org.springframework.data.projection;

import org.springframework.util.ObjectUtils;

/**
 * Wrapper class to expose an object to the SpEL expression as {@code target}.
 */
public final class TargetWrapper {

	private final Object target;

	private final Object[] args;

	private TargetWrapper(Object target, Object[] args) {
		this.target = target;
		this.args = args;
	}

	static TargetWrapper of(Object target, Object[] args) {
		return new TargetWrapper(target, args);
	}

	public Object getTarget() {
		return this.target;
	}

	public Object[] getArgs() {
		return this.args;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof TargetWrapper)) {
			return false;
		}
		TargetWrapper other = (TargetWrapper) o;
		if (!ObjectUtils.nullSafeEquals(this.target, other.target)) {
			return false;
		}
		return ObjectUtils.nullSafeEquals(this.args, other.args);
	}

	@Override
	public int hashCode() {
		int result = ObjectUtils.nullSafeHashCode(this.target);
		result = 31 * result + ObjectUtils.nullSafeHashCode(this.args);
		return result;
	}

	@Override
	public String toString() {
		return "SpelEvaluatingMethodInterceptor.TargetWrapper(target=" + this.getTarget() + ", args="
				+ java.util.Arrays.deepToString(this.getArgs()) + ")";
	}

}
