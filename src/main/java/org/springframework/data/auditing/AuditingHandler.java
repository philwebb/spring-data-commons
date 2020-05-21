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
package org.springframework.data.auditing;

import java.time.temporal.TemporalAccessor;
import java.util.Optional;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.log.LogMessage;
import org.springframework.data.domain.Auditable;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.util.Assert;

/**
 * Auditing handler to mark entity objects created and modified.
 *
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @since 1.5
 */
public class AuditingHandler implements InitializingBean {

	private static final Log logger = LogFactory.getLog(AuditingHandler.class);

	private final DefaultAuditableBeanWrapperFactory factory;

	private DateTimeProvider dateTimeProvider = CurrentDateTimeProvider.INSTANCE;

	private Optional<AuditorAware<?>> auditorAware;

	private boolean dateTimeForNow = true;

	private boolean modifyOnCreation = true;

	/**
	 * Creates a new {@link AuditableBeanWrapper} using the given {@link MappingContext}
	 * when looking up auditing metadata via reflection.
	 * @param mappingContext must not be {@literal null}.
	 * @since 1.8
	 * @deprecated use {@link AuditingHandler(PersistentEntities)} instead.
	 */
	@Deprecated
	public AuditingHandler(
			MappingContext<? extends PersistentEntity<?, ?>, ? extends PersistentProperty<?>> mappingContext) {
		this(PersistentEntities.of(mappingContext));
	}

	/**
	 * Creates a new {@link AuditableBeanWrapper} using the given
	 * {@link PersistentEntities} when looking up auditing metadata via reflection.
	 * @param entities must not be {@literal null}.
	 * @since 1.10
	 */
	public AuditingHandler(PersistentEntities entities) {
		Assert.notNull(entities, "PersistentEntities must not be null!");
		this.factory = new MappingAuditableBeanWrapperFactory(entities);
		this.auditorAware = Optional.empty();
	}

	/**
	 * Setter to inject a {@code AuditorAware} component to retrieve the current auditor.
	 * @param auditorAware must not be {@literal null}.
	 */
	public void setAuditorAware(AuditorAware<?> auditorAware) {
		Assert.notNull(auditorAware, "AuditorAware must not be null!");
		this.auditorAware = Optional.of(auditorAware);
	}

	/**
	 * Setter do determine if {@link Auditable#setCreatedDate(DateTime)} and
	 * {@link Auditable#setLastModifiedDate(DateTime)} shall be filled with the current
	 * Java time. Defaults to {@code true}. One might set this to {@code false} to use
	 * database features to set entity time.
	 * @param dateTimeForNow the dateTimeForNow to set
	 */
	public void setDateTimeForNow(boolean dateTimeForNow) {
		this.dateTimeForNow = dateTimeForNow;
	}

	/**
	 * Set this to true if you want to treat entity creation as modification and thus
	 * setting the current date as modification date during creation, too. Defaults to
	 * {@code true}.
	 * @param modifyOnCreation if modification information shall be set on creation, too
	 */
	public void setModifyOnCreation(boolean modifyOnCreation) {
		this.modifyOnCreation = modifyOnCreation;
	}

	/**
	 * Sets the {@link DateTimeProvider} to be used to determine the dates to be set.
	 * @param dateTimeProvider
	 */
	public void setDateTimeProvider(DateTimeProvider dateTimeProvider) {
		this.dateTimeProvider = dateTimeProvider == null ? CurrentDateTimeProvider.INSTANCE : dateTimeProvider;
	}

	/**
	 * Marks the given object as created.
	 * @param source
	 */
	public <T> T markCreated(T source) {
		Assert.notNull(source, "Entity must not be null!");
		return touch(source, true);
	}

	/**
	 * Marks the given object as modified.
	 * @param source
	 */
	public <T> T markModified(T source) {
		Assert.notNull(source, "Entity must not be null!");
		return touch(source, false);
	}

	/**
	 * Returns whether the given source is considered to be auditable in the first place
	 * @param source must not be {@literal null}.
	 * @return
	 */
	protected final boolean isAuditable(Object source) {
		Assert.notNull(source, "Source must not be null!");
		return this.factory.getBeanWrapperFor(source).isPresent();
	}

	private <T> T touch(T target, boolean isNew) {
		Optional<AuditableBeanWrapper<T>> wrapper = this.factory.getBeanWrapperFor(target);
		return wrapper.map(it -> {
			Optional<Object> auditor = touchAuditor(it, isNew);
			Optional<TemporalAccessor> now = this.dateTimeForNow ? touchDate(it, isNew) : Optional.empty();
			if (logger.isDebugEnabled()) {
				Object defaultedNow = now.map(Object::toString).orElse("not set");
				Object defaultedAuditor = auditor.map(Object::toString).orElse("unknown");
				logger.debug(LogMessage.format("Touched %s - Last modification at %s by %s", target, defaultedNow,
						defaultedAuditor));
			}
			return it.getBean();
		}).orElse(target);
	}

	/**
	 * Sets modifying and creating auditor. Creating auditor is only set on new
	 * auditables.
	 * @param auditable
	 * @return
	 */
	private Optional<Object> touchAuditor(AuditableBeanWrapper<?> wrapper, boolean isNew) {
		Assert.notNull(wrapper, "AuditableBeanWrapper must not be null!");
		return this.auditorAware.map(it -> {
			Optional<?> auditor = it.getCurrentAuditor();
			Assert.notNull(auditor,
					() -> String.format("Auditor must not be null! Returned by: %s!", AopUtils.getTargetClass(it)));
			auditor.filter(temporalAccessor -> isNew).ifPresent(wrapper::setCreatedBy);
			auditor.filter(temporalAccessor -> !isNew || this.modifyOnCreation).ifPresent(wrapper::setLastModifiedBy);
			return auditor;
		});
	}

	/**
	 * Touches the auditable regarding modification and creation date. Creation date is
	 * only set on new auditables.
	 * @param wrapper
	 * @return
	 */
	private Optional<TemporalAccessor> touchDate(AuditableBeanWrapper<?> wrapper, boolean isNew) {
		Assert.notNull(wrapper, "AuditableBeanWrapper must not be null!");
		Optional<TemporalAccessor> now = this.dateTimeProvider.getNow();
		Assert.notNull(now,
				() -> String.format("Now must not be null! Returned by: %s!", this.dateTimeProvider.getClass()));
		now.filter(temporalAccessor -> isNew).ifPresent(wrapper::setCreatedDate);
		now.filter(temporalAccessor -> !isNew || this.modifyOnCreation).ifPresent(wrapper::setLastModifiedDate);
		return now;
	}

	@Override
	public void afterPropertiesSet() {
		if (!this.auditorAware.isPresent()) {
			logger.debug("No AuditorAware set! Auditing will not be applied!");
		}
	}

}
