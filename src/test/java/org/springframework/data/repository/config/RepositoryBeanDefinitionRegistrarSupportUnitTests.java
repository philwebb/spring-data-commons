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
package org.springframework.data.repository.config;

import java.lang.annotation.Annotation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.StandardAnnotationMetadata;
import org.springframework.data.repository.config.basepackage.FragmentImpl;
import org.springframework.data.repository.core.support.DummyRepositoryFactoryBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Integration test for {@link RepositoryBeanDefinitionRegistrarSupport}.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 */
@ExtendWith(MockitoExtension.class)
class RepositoryBeanDefinitionRegistrarSupportUnitTests {

	@Mock
	BeanDefinitionRegistry registry;

	StandardEnvironment environment;

	DummyRegistrar registrar;

	@BeforeEach
	void setUp() {
		this.environment = new StandardEnvironment();
		this.registrar = new DummyRegistrar();
		this.registrar.setEnvironment(this.environment);
	}

	@Test
	void registersBeanDefinitionForFoundBean() {
		AnnotationMetadata metadata = new StandardAnnotationMetadata(SampleConfiguration.class, true);
		this.registrar.registerBeanDefinitions(metadata, this.registry);
		assertBeanDefinitionRegisteredFor("myRepository");
		assertBeanDefinitionRegisteredFor("composedRepository");
		assertBeanDefinitionRegisteredFor("mixinImpl");
		assertBeanDefinitionRegisteredFor("mixinImplFragment");
		assertNoBeanDefinitionRegisteredFor("profileRepository");
	}

	@Test // DATACMNS-1147
	void registersBeanDefinitionWithoutFragmentImplementations() {
		AnnotationMetadata metadata = new StandardAnnotationMetadata(FragmentExclusionConfiguration.class, true);
		this.registrar.registerBeanDefinitions(metadata, this.registry);
		assertBeanDefinitionRegisteredFor("repositoryWithFragmentExclusion");
		assertNoBeanDefinitionRegisteredFor("excludedRepositoryImpl");
	}

	@Test // DATACMNS-1172
	void shouldLimitImplementationBasePackages() {
		AnnotationMetadata metadata = new StandardAnnotationMetadata(LimitsImplementationBasePackages.class, true);
		this.registrar.registerBeanDefinitions(metadata, this.registry);
		assertBeanDefinitionRegisteredFor("personRepository");
		assertNoBeanDefinitionRegisteredFor("fragmentImpl");
	}

	@Test // DATACMNS-360
	void registeredProfileRepositoriesIfProfileActivated() {
		AnnotationMetadata metadata = new StandardAnnotationMetadata(SampleConfiguration.class, true);
		this.environment.setActiveProfiles("profile");
		DummyRegistrar registrar = new DummyRegistrar();
		registrar.setEnvironment(this.environment);
		registrar.registerBeanDefinitions(metadata, this.registry);
		assertBeanDefinitionRegisteredFor("myRepository", "profileRepository");
	}

	@Test // DATACMNS-1497
	void usesBeanNameGeneratorProvided() {
		AnnotationMetadata metadata = new StandardAnnotationMetadata(SampleConfiguration.class, true);
		BeanNameGenerator delegate = new AnnotationBeanNameGenerator();
		DummyRegistrar registrar = new DummyRegistrar();
		registrar.setEnvironment(this.environment);
		registrar.registerBeanDefinitions(metadata, this.registry,
				(definition, registry) -> delegate.generateBeanName(definition, registry).concat("Hello"));
		assertBeanDefinitionRegisteredFor("myRepositoryHello");
	}

	private void assertBeanDefinitionRegisteredFor(String... names) {
		for (String name : names) {
			verify(this.registry, times(1)).registerBeanDefinition(eq(name), any(BeanDefinition.class));
		}
	}

	private void assertNoBeanDefinitionRegisteredFor(String... names) {
		for (String name : names) {
			verify(this.registry, times(0)).registerBeanDefinition(eq(name), any(BeanDefinition.class));
		}
	}

	static class DummyRegistrar extends RepositoryBeanDefinitionRegistrarSupport {

		DummyRegistrar() {
			setResourceLoader(new DefaultResourceLoader());
		}

		@Override
		protected Class<? extends Annotation> getAnnotation() {
			return EnableRepositories.class;
		}

		@Override
		protected RepositoryConfigurationExtension getExtension() {
			return new DummyConfigurationExtension();
		}

	}

	static class DummyConfigurationExtension extends RepositoryConfigurationExtensionSupport {

		@Override
		public String getRepositoryFactoryBeanClassName() {
			return DummyRepositoryFactoryBean.class.getName();
		}

		@Override
		protected String getModulePrefix() {
			return "commons";
		}

	}

	@EnableRepositories(
			includeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, value = RepositoryWithFragmentExclusion.class),
			basePackageClasses = RepositoryWithFragmentExclusion.class)
	static class FragmentExclusionConfiguration {

	}

	@EnableRepositories(basePackageClasses = FragmentImpl.class)
	static class LimitsImplementationBasePackages {

	}

}
