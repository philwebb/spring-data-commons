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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.function.Supplier;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.AfterDomainEventPublication;
import org.springframework.data.domain.DomainEvents;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.util.AnnotationDetectionMethodCallback;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ReflectionUtils;

/**
 * {@link RepositoryProxyPostProcessor} to register a {@link MethodInterceptor} to
 * intercept the {@link CrudRepository#save(Object)} method and publish events potentially
 * exposed via a method annotated with {@link DomainEvents}. If no such method can be
 * detected on the aggregate root, no interceptor is added. Additionally, the aggregate
 * root can expose a method annotated with {@link AfterDomainEventPublication}. If
 * present, the method will be invoked after all events have been published.
 *
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @author Yuki Yoshida
 * @since 1.13
 */
public class EventPublishingRepositoryProxyPostProcessor implements RepositoryProxyPostProcessor {

	private final ApplicationEventPublisher publisher;

	public EventPublishingRepositoryProxyPostProcessor(ApplicationEventPublisher publisher) {
		this.publisher = publisher;
	}

	@Override
	public void postProcess(ProxyFactory factory, RepositoryInformation repositoryInformation) {
		EventPublishingMethod method = EventPublishingMethod.of(repositoryInformation.getDomainType());
		if (method == null) {
			return;
		}
		factory.addAdvice(new EventPublishingMethodInterceptor(method, this.publisher));
	}

	/**
	 * {@link MethodInterceptor} to publish events exposed an aggregate on calls to a save
	 * method on the repository.
	 *
	 * @since 1.13
	 */
	static final class EventPublishingMethodInterceptor implements MethodInterceptor {

		private final EventPublishingMethod eventMethod;

		private final ApplicationEventPublisher publisher;

		private EventPublishingMethodInterceptor(EventPublishingMethod eventMethod,
				ApplicationEventPublisher publisher) {
			this.eventMethod = eventMethod;
			this.publisher = publisher;
		}

		static EventPublishingMethodInterceptor of(EventPublishingMethod eventMethod,
				ApplicationEventPublisher publisher) {
			return new EventPublishingMethodInterceptor(eventMethod, publisher);
		}

		@Override
		public Object invoke(@SuppressWarnings("null") MethodInvocation invocation) throws Throwable {
			Object[] arguments = invocation.getArguments();
			Object result = invocation.proceed();
			if (!invocation.getMethod().getName().startsWith("save")) {
				return result;
			}
			Object eventSource = (arguments.length != 1) ? result : arguments[0];
			this.eventMethod.publishEventsFrom(eventSource, this.publisher);
			return result;
		}

	}

	/**
	 * Abstraction of a method on the aggregate root that exposes the events to publish.
	 *
	 * @since 1.13
	 */
	static class EventPublishingMethod {

		private static Map<Class<?>, EventPublishingMethod> cache = new ConcurrentReferenceHashMap<>();

		@SuppressWarnings("null")
		private static EventPublishingMethod NONE = new EventPublishingMethod(null, null);

		private final Method publishingMethod;

		@Nullable
		private final Method clearingMethod;

		EventPublishingMethod(Method publishingMethod, Method clearingMethod) {
			this.publishingMethod = publishingMethod;
			this.clearingMethod = clearingMethod;
		}

		/**
		 * Creates an {@link EventPublishingMethod} for the given type.
		 * @param type must not be {@literal null}.
		 * @return an {@link EventPublishingMethod} for the given type or {@literal null}
		 * in case the given type does not expose an event publishing method.
		 */
		@Nullable
		static EventPublishingMethod of(Class<?> type) {
			Assert.notNull(type, "Type must not be null!");
			EventPublishingMethod eventPublishingMethod = cache.get(type);
			if (eventPublishingMethod != null) {
				return eventPublishingMethod.orNull();
			}
			EventPublishingMethod result = from(getDetector(type, DomainEvents.class),
					() -> getDetector(type, AfterDomainEventPublication.class));
			cache.put(type, result);
			return result.orNull();
		}

		/**
		 * Publishes all events in the given aggregate root using the given
		 * {@link ApplicationEventPublisher}.
		 * @param object can be {@literal null}.
		 * @param publisher must not be {@literal null}.
		 */
		void publishEventsFrom(@Nullable Object object, ApplicationEventPublisher publisher) {
			if (object == null) {
				return;
			}
			for (Object aggregateRoot : asCollection(object)) {
				for (Object event : asCollection(ReflectionUtils.invokeMethod(this.publishingMethod, aggregateRoot))) {
					publisher.publishEvent(event);
				}
				if (this.clearingMethod != null) {
					ReflectionUtils.invokeMethod(this.clearingMethod, aggregateRoot);
				}
			}
		}

		/**
		 * Returns the current {@link EventPublishingMethod} or {@literal null} if it's
		 * the default value.
		 * @return
		 */
		@Nullable
		private EventPublishingMethod orNull() {
			return (this != EventPublishingMethod.NONE) ? this : null;
		}

		private static <T extends Annotation> AnnotationDetectionMethodCallback<T> getDetector(Class<?> type,
				Class<T> annotation) {
			AnnotationDetectionMethodCallback<T> callback = new AnnotationDetectionMethodCallback<>(annotation);
			ReflectionUtils.doWithMethods(type, callback);
			return callback;
		}

		/**
		 * Creates a new {@link EventPublishingMethod} using the given pre-populated
		 * {@link AnnotationDetectionMethodCallback} looking up an optional clearing
		 * method from the given callback.
		 * @param publishing must not be {@literal null}.
		 * @param clearing must not be {@literal null}.
		 * @return
		 */
		private static EventPublishingMethod from(AnnotationDetectionMethodCallback<?> publishing,
				Supplier<AnnotationDetectionMethodCallback<?>> clearing) {
			if (!publishing.hasFoundAnnotation()) {
				return EventPublishingMethod.NONE;
			}
			Method eventMethod = publishing.getRequiredMethod();
			ReflectionUtils.makeAccessible(eventMethod);
			return new EventPublishingMethod(eventMethod, getClearingMethod(clearing.get()));
		}

		/**
		 * Returns the {@link Method} supposed to be invoked for event clearing or
		 * {@literal null} if none is found.
		 * @param clearing must not be {@literal null}.
		 * @return
		 */
		@Nullable
		private static Method getClearingMethod(AnnotationDetectionMethodCallback<?> clearing) {
			if (!clearing.hasFoundAnnotation()) {
				return null;
			}
			Method method = clearing.getRequiredMethod();
			ReflectionUtils.makeAccessible(method);
			return method;
		}

		/**
		 * Returns the given source object as collection, i.e. collections are returned as
		 * is, objects are turned into a one-element collection, {@literal null} will
		 * become an empty collection.
		 * @param source can be {@literal null}.
		 * @return
		 */
		@SuppressWarnings("unchecked")
		private static Collection<Object> asCollection(@Nullable Object source) {
			if (source == null) {
				return Collections.emptyList();
			}
			if (Collection.class.isInstance(source)) {
				return (Collection<Object>) source;
			}
			return Collections.singletonList(source);
		}

	}

}
