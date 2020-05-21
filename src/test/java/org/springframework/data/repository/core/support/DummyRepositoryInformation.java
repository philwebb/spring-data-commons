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
package org.springframework.data.repository.core.support;

import java.lang.reflect.Method;
import java.util.Set;

import org.springframework.data.repository.core.CrudMethods;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.util.Streamable;

public final class DummyRepositoryInformation implements RepositoryInformation {

	private final RepositoryMetadata metadata;

	public DummyRepositoryInformation(Class<?> repositoryInterface) {
		this(new DefaultRepositoryMetadata(repositoryInterface));
	}

	public DummyRepositoryInformation(RepositoryMetadata metadata) {
		this.metadata = metadata;
	}

	@Override
	public Class<?> getIdType() {
		return this.metadata.getIdType();
	}

	@Override
	public Class<?> getDomainType() {
		return this.metadata.getDomainType();
	}

	@Override
	public Class<?> getRepositoryInterface() {
		return this.metadata.getRepositoryInterface();
	}

	@Override
	public Class<?> getReturnedDomainClass(Method method) {
		return getDomainType();
	}

	@Override
	public Class<?> getRepositoryBaseClass() {
		return getRepositoryInterface();
	}

	@Override
	public boolean hasCustomMethod() {
		return false;
	}

	@Override
	public boolean isCustomMethod(Method method) {
		return false;
	}

	@Override
	public boolean isQueryMethod(Method method) {
		return false;
	}

	@Override
	public Streamable<Method> getQueryMethods() {
		return Streamable.empty();
	}

	@Override
	public Method getTargetClassMethod(Method method) {
		return method;
	}

	@Override
	public boolean isBaseClassMethod(Method method) {
		return true;
	}

	@Override
	public CrudMethods getCrudMethods() {
		return new DefaultCrudMethods(this);
	}

	@Override
	public boolean isPagingRepository() {
		return false;
	}

	@Override
	public Set<Class<?>> getAlternativeDomainTypes() {
		return this.metadata.getAlternativeDomainTypes();
	}

	@Override
	public boolean isReactiveRepository() {
		return this.metadata.isReactiveRepository();
	}

}
