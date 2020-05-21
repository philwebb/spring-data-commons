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
package org.springframework.data.repository.init;

import java.util.Collection;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.AdditionalAnswers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.Resource;
import org.springframework.data.repository.sample.Product;
import org.springframework.data.repository.sample.ProductRepository;
import org.springframework.data.repository.sample.SampleConfiguration;
import org.springframework.data.repository.sample.User;
import org.springframework.data.repository.support.Repositories;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link UnmarshallingRepositoryInitializer}.
 *
 * @author Oliver Gierke
 */
@SpringJUnitConfig(classes = SampleConfiguration.class)
class ResourceReaderRepositoryInitializerUnitTests {

	@Autowired
	ProductRepository productRepository;

	@Autowired
	Repositories repositories;

	ApplicationEventPublisher publisher;

	ResourceReader reader;

	Resource resource;

	@BeforeEach
	void setUp() {

		this.reader = mock(ResourceReader.class);
		this.publisher = mock(ApplicationEventPublisher.class);
		this.resource = mock(Resource.class);
	}

	@Test
	void storesSingleObjectCorrectly() throws Exception {

		Product reference = new Product();
		setUpReferenceAndInititalize(reference);

		verify(this.productRepository).save(reference);
	}

	@Test
	void storesCollectionOfObjectsCorrectly() throws Exception {

		Product product = new Product();
		Collection<Product> reference = Collections.singletonList(product);

		setUpReferenceAndInititalize(reference);

		verify(this.productRepository, times(1)).save(product);
	}

	@Test // DATACMNS-224
	void emitsRepositoriesPopulatedEventIfPublisherConfigured() throws Exception {

		RepositoryPopulator populator = setUpReferenceAndInititalize(new User(), this.publisher);

		ApplicationEvent event = new RepositoriesPopulatedEvent(populator, this.repositories);
		verify(this.publisher, times(1)).publishEvent(event);
	}

	private RepositoryPopulator setUpReferenceAndInititalize(Object reference, ApplicationEventPublisher publish)
			throws Exception {

		when(this.reader.readFrom(any(), any())).thenReturn(reference);
		when(this.productRepository.save(any())).then(AdditionalAnswers.returnsFirstArg());

		ResourceReaderRepositoryPopulator populator = new ResourceReaderRepositoryPopulator(this.reader);
		populator.setResources(this.resource);
		populator.setApplicationEventPublisher(this.publisher);
		populator.populate(this.repositories);

		return populator;
	}

	private RepositoryPopulator setUpReferenceAndInititalize(Object reference) throws Exception {
		return setUpReferenceAndInititalize(reference, null);
	}

}
