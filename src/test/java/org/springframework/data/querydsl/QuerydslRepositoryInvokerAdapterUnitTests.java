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
package org.springframework.data.querydsl;

import java.io.Serializable;

import com.querydsl.core.types.Predicate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.support.RepositoryInvoker;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link QuerydslRepositoryInvokerAdapter}.
 *
 * @author Oliver Gierke
 */
@ExtendWith(MockitoExtension.class)
class QuerydslRepositoryInvokerAdapterUnitTests {

	@Mock
	RepositoryInvoker delegate;

	@Mock
	QuerydslPredicateExecutor<Object> executor;

	@Mock
	Predicate predicate;

	QuerydslRepositoryInvokerAdapter adapter;

	@BeforeEach
	void setUp() {
		this.adapter = new QuerydslRepositoryInvokerAdapter(this.delegate, this.executor, this.predicate);
	}

	@Test // DATACMNS-669
	void forwardsFindAllToExecutorWithPredicate() {

		Sort sort = Sort.by("firstname");
		this.adapter.invokeFindAll(sort);

		verify(this.executor, times(1)).findAll(this.predicate, sort);
		verify(this.delegate, times(0)).invokeFindAll(sort);
	}

	@Test // DATACMNS-669
	void forwardsFindAllWithPageableToExecutorWithPredicate() {

		PageRequest pageable = PageRequest.of(0, 10);
		this.adapter.invokeFindAll(pageable);

		verify(this.executor, times(1)).findAll(this.predicate, pageable);
		verify(this.delegate, times(0)).invokeFindAll(pageable);
	}

	@Test // DATACMNS-669
	@SuppressWarnings("unchecked")
	void forwardsMethodsToDelegate() {

		this.adapter.hasDeleteMethod();
		verify(this.delegate, times(1)).hasDeleteMethod();

		this.adapter.hasFindAllMethod();
		verify(this.delegate, times(1)).hasFindAllMethod();

		this.adapter.hasFindOneMethod();
		verify(this.delegate, times(1)).hasFindOneMethod();

		this.adapter.hasSaveMethod();
		verify(this.delegate, times(1)).hasSaveMethod();

		this.adapter.invokeDeleteById(any(Serializable.class));
		verify(this.delegate, times(1)).invokeDeleteById(any());

		this.adapter.invokeFindById(any(Serializable.class));
		verify(this.delegate, times(1)).invokeFindById(any());

		this.adapter.invokeQueryMethod(any(), any(), any(), any());

		verify(this.delegate, times(1)).invokeQueryMethod(any(), any(), any(), any());

		this.adapter.invokeSave(any());
		verify(this.delegate, times(1)).invokeSave(any());
	}

}
