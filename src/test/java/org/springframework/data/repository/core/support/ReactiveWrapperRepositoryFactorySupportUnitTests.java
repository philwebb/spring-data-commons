/*
 * Copyright 2016-2020 the original author or authors.
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

import java.io.Serializable;

import io.reactivex.Completable;
import io.reactivex.Maybe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import rx.Single;

import org.springframework.data.repository.Repository;
import org.springframework.data.repository.reactive.ReactiveSortingRepository;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link RepositoryFactorySupport} using reactive wrapper types.
 *
 * @author Mark Paluch
 * @author Christoph Strobl
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReactiveWrapperRepositoryFactorySupportUnitTests {

	DummyRepositoryFactory factory;

	@Mock
	ReactiveSortingRepository<Object, Serializable> backingRepo;

	@Mock
	ObjectRepositoryCustom customImplementation;

	@BeforeEach
	void setUp() {
		this.factory = new DummyRepositoryFactory(this.backingRepo);
	}

	@Test // DATACMNS-836
	void invokesCustomMethodIfItRedeclaresACRUDOne() {
		ObjectRepository repository = this.factory.getRepository(ObjectRepository.class, this.customImplementation);
		repository.findById(1);
		verify(this.customImplementation, times(1)).findById(1);
		verify(this.backingRepo, times(0)).findById(1);
	}

	@Test // DATACMNS-836, DATACMNS-1154
	void callsRxJava1MethodOnBaseImplementationWithExactArguments() {
		Serializable id = 1L;
		given(this.backingRepo.existsById(id)).willReturn(Mono.just(true));
		RxJava1ConvertingRepository repository = this.factory.getRepository(RxJava1ConvertingRepository.class);
		repository.existsById(id);
		repository.existsById((Long) id);
		verify(this.backingRepo, times(2)).existsById(id);
	}

	@Test // DATACMNS-836, DATACMNS-1063, DATACMNS-1154
	@SuppressWarnings("unchecked")
	void callsRxJava1MethodOnBaseImplementationWithTypeConversion() {
		given(this.backingRepo.existsById(any(Publisher.class))).willReturn(Mono.just(true));
		Single<Long> ids = Single.just(1L);
		RxJava1ConvertingRepository repository = this.factory.getRepository(RxJava1ConvertingRepository.class);
		repository.existsById(ids);
		verify(this.backingRepo, times(1)).existsById(any(Publisher.class));
	}

	@Test // DATACMNS-988, DATACMNS-1154
	void callsRxJava2MethodOnBaseImplementationWithExactArguments() {
		Long id = 1L;
		given(this.backingRepo.findById(id)).willReturn(Mono.just(true));
		RxJava2ConvertingRepository repository = this.factory.getRepository(RxJava2ConvertingRepository.class);
		repository.findById(id);
		verify(this.backingRepo, times(1)).findById(id);
	}

	@Test // DATACMNS-988, DATACMNS-1154
	void callsRxJava2MethodOnBaseImplementationWithTypeConversion() {
		Serializable id = 1L;
		given(this.backingRepo.deleteById(id)).willReturn(Mono.empty());
		RxJava2ConvertingRepository repository = this.factory.getRepository(RxJava2ConvertingRepository.class);
		repository.deleteById(id);
		verify(this.backingRepo, times(1)).deleteById(id);
	}

	interface RxJava1ConvertingRepository extends Repository<Object, Long> {

		Single<Boolean> existsById(Single<Long> id);

		Single<Boolean> existsById(Serializable id);

		Single<Boolean> existsById(Long id);

	}

	interface RxJava2ConvertingRepository extends Repository<Object, Long> {

		Maybe<Boolean> findById(Serializable id);

		Single<Boolean> existsById(Long id);

		Completable deleteById(Serializable id);

	}

	interface ObjectRepository
			extends Repository<Object, Object>, RepositoryFactorySupportUnitTests.ObjectRepositoryCustom {

	}

	interface ObjectRepositoryCustom {

		Object findById(Object id);

	}

}
