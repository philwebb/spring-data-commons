package org.springframework.data.repository.sample;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.DummyRepositoryFactoryBean;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;
import org.springframework.data.repository.support.Repositories;

import static org.mockito.Mockito.mock;

@Configuration
public class SampleConfiguration {

	@Autowired
	ApplicationContext context;

	@Bean
	Repositories repositories() {
		return new Repositories(this.context);
	}

	@Bean
	RepositoryFactoryBeanSupport<Repository<User, Long>, User, Long> userRepositoryFactory() {

		return new DummyRepositoryFactoryBean<>(UserRepository.class);
	}

	@Bean
	RepositoryFactoryBeanSupport<Repository<Product, Long>, Product, Long> productRepositoryFactory(
			ProductRepository productRepository) {

		DummyRepositoryFactoryBean<Repository<Product, Long>, Product, Long> factory = new DummyRepositoryFactoryBean<>(
				ProductRepository.class);
		factory.setCustomImplementation(productRepository);

		return factory;
	}

	@Bean
	ProductRepository productRepository() {
		return mock(ProductRepository.class);
	}

}
