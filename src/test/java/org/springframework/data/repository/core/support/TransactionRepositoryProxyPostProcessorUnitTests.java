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

package org.springframework.data.repository.core.support;

import java.io.Serializable;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.support.TransactionalRepositoryProxyPostProcessor.CustomAnnotationTransactionAttributeSource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * Unit test for {@link TransactionalRepositoryProxyPostProcessor}.
 *
 * @author Oliver Gierke
 */
@ExtendWith(MockitoExtension.class)
class TransactionRepositoryProxyPostProcessorUnitTests {

	@Mock
	ListableBeanFactory beanFactory;

	@Mock
	ProxyFactory proxyFactory;

	@Mock
	RepositoryInformation repositoryInformation;

	@Test
	void rejectsNullBeanFactory() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new TransactionalRepositoryProxyPostProcessor(null, "transactionManager", true));
	}

	@Test
	void rejectsNullTxManagerName() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new TransactionalRepositoryProxyPostProcessor(this.beanFactory, null, true));
	}

	@Test
	void setsUpBasicInstance() throws Exception {
		RepositoryProxyPostProcessor postProcessor = new TransactionalRepositoryProxyPostProcessor(this.beanFactory,
				"txManager", true);
		postProcessor.postProcess(this.proxyFactory, this.repositoryInformation);
		verify(this.proxyFactory).addAdvice(any(TransactionInterceptor.class));
	}

	@Test // DATACMNS-464
	void fallsBackToTargetMethodTransactionSettings() throws Exception {
		assertTransactionAttributeFor(SampleImplementation.class);
	}

	@Test // DATACMNS-464
	void fallsBackToTargetClassTransactionSettings() throws Exception {
		assertTransactionAttributeFor(SampleImplementationWithClassAnnotation.class);
	}

	@Test // DATACMNS-732
	void considersJtaTransactional() throws Exception {
		Method method = SampleRepository.class.getMethod("methodWithJtaOneDotTwoAtTransactional");
		TransactionAttributeSource attributeSource = new CustomAnnotationTransactionAttributeSource();
		TransactionAttribute attribute = attributeSource.getTransactionAttribute(method, SampleRepository.class);
		assertThat(attribute).isNotNull();
	}

	private void assertTransactionAttributeFor(Class<?> implementationClass) throws Exception {
		Method repositorySaveMethod = SampleRepository.class.getMethod("save", Sample.class);
		Method implementationClassMethod = implementationClass.getMethod("save", Object.class);
		given(this.repositoryInformation.getTargetClassMethod(repositorySaveMethod))
				.willReturn(implementationClassMethod);
		CustomAnnotationTransactionAttributeSource attributeSource = new CustomAnnotationTransactionAttributeSource();
		attributeSource.setRepositoryInformation(this.repositoryInformation);
		TransactionAttribute attribute = attributeSource.getTransactionAttribute(repositorySaveMethod,
				SampleImplementation.class);
		assertThat(attribute).isNotNull();
	}

	static class Sample {

	}

	interface SampleRepository extends Repository<Sample, Serializable> {

		Sample save(Sample object);

		@javax.transaction.Transactional
		void methodWithJtaOneDotTwoAtTransactional();

	}

	static class SampleImplementation<T> {

		@Transactional
		public <S extends T> S save(S object) {
			return null;
		}

	}

	@Transactional
	static class SampleImplementationWithClassAnnotation<T> {

		public <S extends T> S save(S object) {
			return null;
		}

	}

}
