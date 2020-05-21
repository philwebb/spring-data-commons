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

package org.springframework.data.repository.core.support;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.RepositoryDefinition;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.DummyRepositoryFactory.MyRepositoryQuery;
import org.springframework.data.repository.core.support.RepositoryComposition.RepositoryFragments;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.sample.User;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncAnnotationBeanPostProcessor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.interceptor.TransactionalProxy;
import org.springframework.util.ClassUtils;
import org.springframework.util.concurrent.ListenableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Unit tests for {@link RepositoryFactorySupport}.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 * @author Ariel Carrera
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RepositoryFactorySupportUnitTests {

	DummyRepositoryFactory factory;

	@Mock
	PagingAndSortingRepository<Object, Object> backingRepo;

	@Mock
	ObjectRepositoryCustom customImplementation;

	@Mock
	MyQueryCreationListener listener;

	@Mock
	PlainQueryCreationListener otherListener;

	@Mock
	RepositoryProxyPostProcessor repositoryPostProcessor;

	@BeforeEach
	void setUp() {
		this.factory = new DummyRepositoryFactory(this.backingRepo);
	}

	@Test
	void invokesCustomQueryCreationListenerForSpecialRepositoryQueryOnly() {
		Mockito.reset(this.factory.strategy);
		given(this.factory.strategy.resolveQuery(any(Method.class), any(RepositoryMetadata.class),
				any(ProjectionFactory.class), any(NamedQueries.class))).willReturn(this.factory.queryOne,
						this.factory.queryTwo);
		this.factory.addQueryCreationListener(this.listener);
		this.factory.addQueryCreationListener(this.otherListener);
		this.factory.getRepository(ObjectRepository.class);
		verify(this.listener, times(1)).onCreation(any(MyRepositoryQuery.class));
		verify(this.otherListener, times(2)).onCreation(any(RepositoryQuery.class));
	}

	@Test // DATACMNS-1538
	void invokesCustomRepositoryProxyPostProcessor() {
		this.factory.addRepositoryProxyPostProcessor(this.repositoryPostProcessor);
		this.factory.getRepository(ObjectRepository.class);
		verify(this.repositoryPostProcessor, times(1)).postProcess(any(ProxyFactory.class),
				any(RepositoryInformation.class));
	}

	@Test
	void routesCallToRedeclaredMethodIntoTarget() {
		ObjectRepository repository = this.factory.getRepository(ObjectRepository.class);
		repository.save(repository);
		verify(this.backingRepo, times(1)).save(any(Object.class));
	}

	@Test
	void invokesCustomMethodIfItRedeclaresACRUDOne() {
		ObjectRepository repository = this.factory.getRepository(ObjectRepository.class, this.customImplementation);
		repository.findById(1);
		verify(this.customImplementation, times(1)).findById(1);
		verify(this.backingRepo, times(0)).findById(1);
	}

	@Test // DATACMNS-102
	void invokesCustomMethodCompositionMethodIfItRedeclaresACRUDOne() {
		ObjectRepository repository = this.factory.getRepository(ObjectRepository.class,
				RepositoryFragments.just(this.customImplementation));
		repository.findById(1);
		verify(this.customImplementation, times(1)).findById(1);
		verify(this.backingRepo, times(0)).findById(1);
	}

	@Test
	void createsRepositoryInstanceWithCustomIntermediateRepository() {
		CustomRepository repository = this.factory.getRepository(CustomRepository.class);
		Pageable pageable = PageRequest.of(0, 10);
		given(this.backingRepo.findAll(pageable)).willReturn(new PageImpl<>(Collections.emptyList()));
		repository.findAll(pageable);
		verify(this.backingRepo, times(1)).findAll(pageable);
	}

	@Test
	@SuppressWarnings("unchecked")
	void createsProxyForAnnotatedRepository() {
		Class<?> repositoryInterface = AnnotatedRepository.class;
		Class<? extends Repository<?, ?>> foo = (Class<? extends Repository<?, ?>>) repositoryInterface;
		assertThat(this.factory.getRepository(foo)).isNotNull();
	}

	@Test // DATACMNS-341
	void usesDefaultClassLoaderIfNullConfigured() {
		this.factory.setBeanClassLoader(null);
		assertThat(ReflectionTestUtils.getField(this.factory, "classLoader"))
				.isEqualTo(ClassUtils.getDefaultClassLoader());
	}

	@Test // DATACMNS-489
	void wrapsExecutionResultIntoFutureIfConfigured() throws Exception {
		final Object reference = new Object();
		given(this.factory.queryOne.execute(any(Object[].class))).will((invocation) -> {
			Thread.sleep(500);
			return reference;
		});
		ConvertingRepository repository = this.factory.getRepository(ConvertingRepository.class);
		AsyncAnnotationBeanPostProcessor processor = new AsyncAnnotationBeanPostProcessor();
		processor.setBeanFactory(new DefaultListableBeanFactory());
		repository = (ConvertingRepository) processor.postProcessAfterInitialization(repository, null);
		Future<Object> future = repository.findByFirstname("Foo");
		assertThat(future.isDone()).isFalse();
		while (!future.isDone()) {
			Thread.sleep(300);
		}
		assertThat(future.get()).isEqualTo(reference);
		verify(this.factory.queryOne, times(1)).execute(any(Object[].class));
	}

	@Test // DATACMNS-509
	void convertsWithSameElementType() {
		List<String> names = Collections.singletonList("Dave");
		given(this.factory.queryOne.execute(any(Object[].class))).willReturn(names);
		ConvertingRepository repository = this.factory.getRepository(ConvertingRepository.class);
		Set<String> result = repository.convertListToStringSet();
		assertThat(result).hasSize(1);
		assertThat(result.iterator().next()).isEqualTo("Dave");
	}

	@Test // DATACMNS-509
	void convertsCollectionToOtherCollectionWithElementSuperType() {
		List<String> names = Collections.singletonList("Dave");
		given(this.factory.queryOne.execute(any(Object[].class))).willReturn(names);
		ConvertingRepository repository = this.factory.getRepository(ConvertingRepository.class);
		Set<Object> result = repository.convertListToObjectSet();
		assertThat(result).containsExactly("Dave");
	}

	@Test // DATACMNS-656
	void rejectsNullRepositoryProxyPostProcessor() {
		assertThatThrownBy(() -> this.factory.addRepositoryProxyPostProcessor(null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining(RepositoryProxyPostProcessor.class.getSimpleName());
	}

	@Test // DATACMNS-715, SPR-13109
	void addsTransactionProxyInterfaceIfAvailable() {
		assertThat(this.factory.getRepository(SimpleRepository.class)).isInstanceOf(TransactionalProxy.class);
	}

	@Test // DATACMNS-714
	void wrapsExecutionResultIntoCompletableFutureIfConfigured() throws Exception {
		User reference = new User();
		expect(prepareConvertingRepository(reference).findOneByFirstname("Foo"), reference);
	}

	@Test // DATACMNS-714
	void wrapsExecutionResultIntoListenableFutureIfConfigured() throws Exception {
		User reference = new User();
		expect(prepareConvertingRepository(reference).findOneByLastname("Foo"), reference);
	}

	@Test // DATACMNS-714
	void wrapsExecutionResultIntoCompletableFutureWithEntityCollectionIfConfigured() throws Exception {
		List<User> reference = Collections.singletonList(new User());
		expect(prepareConvertingRepository(reference).readAllByFirstname("Foo"), reference);
	}

	@Test // DATACMNS-714
	void wrapsExecutionResultIntoListenableFutureWithEntityCollectionIfConfigured() throws Exception {
		List<User> reference = Collections.singletonList(new User());
		expect(prepareConvertingRepository(reference).readAllByLastname("Foo"), reference);
	}

	@Test // DATACMNS-763
	@SuppressWarnings("rawtypes")
	void rejectsRepositoryBaseClassWithInvalidConstructor() {
		RepositoryInformation information = mock(RepositoryInformation.class);
		willReturn(CustomRepositoryBaseClass.class).given(information).getRepositoryBaseClass();
		EntityInformation entityInformation = mock(EntityInformation.class);
		assertThatThrownBy(() -> this.factory.getTargetRepositoryViaReflection(information, entityInformation, "Foo"))
				.isInstanceOf(IllegalStateException.class).hasMessageContaining(entityInformation.getClass().getName())
				.hasMessageContaining(String.class.getName());
	}

	@Test
	void callsStaticMethodOnInterface() {
		ObjectRepository repository = this.factory.getRepository(ObjectRepository.class, this.customImplementation);
		assertThat(repository.staticMethodDelegate()).isEqualTo("OK");
		verifyNoInteractions(this.customImplementation);
		verifyNoInteractions(this.backingRepo);
	}

	@Test // DATACMNS-1154
	void considersRequiredReturnValue() {
		KotlinUserRepository repository = this.factory.getRepository(KotlinUserRepository.class);
		assertThatThrownBy(() -> repository.findById("")).isInstanceOf(EmptyResultDataAccessException.class)
				.hasMessageContaining("Result must not be null!");
		assertThat(repository.findByUsername("")).isNull();
	}

	@Test // DATACMNS-1154
	void considersRequiredParameter() {
		ObjectRepository repository = this.factory.getRepository(ObjectRepository.class);
		assertThatThrownBy(() -> repository.findByClass(null)).isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("must not be null!");
	}

	@Test // DATACMNS-1154
	void shouldAllowVoidMethods() {
		ObjectRepository repository = this.factory.getRepository(ObjectRepository.class, this.backingRepo);
		repository.deleteAll();
		verify(this.backingRepo).deleteAll();
	}

	@Test // DATACMNS-1154
	void considersRequiredKotlinParameter() {
		KotlinUserRepository repository = this.factory.getRepository(KotlinUserRepository.class);
		assertThatThrownBy(() -> repository.findById(null)).isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("must not be null!");
	}

	@Test // DATACMNS-1154
	void considersRequiredKotlinNullableParameter() {
		KotlinUserRepository repository = this.factory.getRepository(KotlinUserRepository.class);
		assertThat(repository.findByOptionalId(null)).isNull();
	}

	@Test // DATACMNS-1197
	void considersNullabilityForKotlinInterfaceProperties() {
		KotlinUserRepository repository = this.factory.getRepository(KotlinUserRepository.class);
		assertThatThrownBy(repository::getFindRouteQuery).isInstanceOf(EmptyResultDataAccessException.class);
	}

	private ConvertingRepository prepareConvertingRepository(final Object expectedValue) {
		given(this.factory.queryOne.execute(any(Object[].class))).will((invocation) -> {
			Thread.sleep(200);
			return expectedValue;
		});
		AsyncAnnotationBeanPostProcessor processor = new AsyncAnnotationBeanPostProcessor();
		processor.setBeanFactory(new DefaultListableBeanFactory());
		return (ConvertingRepository) processor
				.postProcessAfterInitialization(this.factory.getRepository(ConvertingRepository.class), null);
	}

	private void expect(Future<?> future, Object value) throws Exception {
		assertThat(future.isDone()).isFalse();
		while (!future.isDone()) {
			Thread.sleep(50);
		}
		assertThat(future.get()).isEqualTo(value);
		verify(this.factory.queryOne, times(1)).execute(any(Object[].class));
	}

	interface SimpleRepository extends Repository<Object, Serializable> {

	}

	interface ObjectRepository extends Repository<Object, Object>, ObjectRepositoryCustom {

		@Nullable
		Object findByClass(Class<?> clazz);

		@Nullable
		Object findByFoo();

		@Nullable
		Object save(Object entity);

		static String staticMethod() {
			return "OK";
		}

		default String staticMethodDelegate() {
			return staticMethod();
		}

	}

	interface ObjectRepositoryCustom {

		@Nullable
		Object findById(Object id);

		void deleteAll();

	}

	interface PlainQueryCreationListener extends QueryCreationListener<RepositoryQuery> {

	}

	interface MyQueryCreationListener extends QueryCreationListener<MyRepositoryQuery> {

	}

	interface ReadOnlyRepository<T, ID extends Serializable> extends Repository<T, ID> {

		Optional<T> findById(ID id);

		Iterable<T> findAll();

		Page<T> findAll(Pageable pageable);

		List<T> findAll(Sort sort);

		boolean existsById(ID id);

		long count();

	}

	interface CustomRepository extends ReadOnlyRepository<Object, Long> {

	}

	@RepositoryDefinition(domainClass = Object.class, idClass = Long.class)
	interface AnnotatedRepository {

	}

	interface ConvertingRepository extends Repository<Object, Long> {

		Set<String> convertListToStringSet();

		Set<Object> convertListToObjectSet();

		@Async
		Future<Object> findByFirstname(String firstname);

		// DATACMNS-714
		@Async
		CompletableFuture<User> findOneByFirstname(String firstname);

		// DATACMNS-714
		@Async
		CompletableFuture<List<User>> readAllByFirstname(String firstname);

		// DATACMNS-714
		@Async
		ListenableFuture<User> findOneByLastname(String lastname);

		// DATACMNS-714
		@Async
		ListenableFuture<List<User>> readAllByLastname(String lastname);

	}

	static class CustomRepositoryBaseClass {

		CustomRepositoryBaseClass(EntityInformation<?, ?> information) {
		}

	}

}
