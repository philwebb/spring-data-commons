/*
 * Copyright 2018-2020 the original author or authors.
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

import org.slf4j.Logger;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.Ordered;
import org.springframework.data.repository.Repository;

/**
 * {@link ApplicationListener} to trigger the initialization of Spring Data repositories right before the application
 * context is started.
 *
 * @author Oliver Gierke
 * @since 2.1
 */
class DeferredRepositoryInitializationListener implements ApplicationListener<ContextRefreshedEvent>, Ordered {

	private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(DeferredRepositoryInitializationListener.class);
	private final ListableBeanFactory beanFactory;

	DeferredRepositoryInitializationListener(ListableBeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}
	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {

		LOG.info("Triggering deferred initialization of Spring Data repositories…");

		this.beanFactory.getBeansOfType(Repository.class);

		LOG.info("Spring Data repositories initialized!");
	}
	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}
}
