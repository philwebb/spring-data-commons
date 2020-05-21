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
package org.springframework.data.auditing;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.mapping.context.SampleMappingContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@code AuditingHandler}.
 *
 * @author Oliver Gierke
 * @since 1.5
 */
@SuppressWarnings("unchecked")
class AuditingHandlerUnitTests {

	AuditingHandler handler;

	AuditorAware<AuditedUser> auditorAware;

	AuditedUser user;

	@BeforeEach
	void setUp() {
		this.handler = getHandler();
		this.user = new AuditedUser();
		this.auditorAware = mock(AuditorAware.class);
		when(this.auditorAware.getCurrentAuditor()).thenReturn(Optional.of(this.user));
	}

	protected AuditingHandler getHandler() {
		return new AuditingHandler(PersistentEntities.of());
	}

	/**
	 * Checks that the advice does not set auditor on the target entity if no
	 * {@code AuditorAware} was configured.
	 */
	@Test
	void doesNotSetAuditorIfNotConfigured() {
		this.handler.markCreated(this.user);
		assertThat(this.user.getCreatedDate()).isPresent();
		assertThat(this.user.getLastModifiedDate()).isPresent();
		assertThat(this.user.getCreatedBy()).isNotPresent();
		assertThat(this.user.getLastModifiedBy()).isNotPresent();
	}

	/**
	 * Checks that the advice sets the auditor on the target entity if an
	 * {@code AuditorAware} was configured.
	 */
	@Test
	void setsAuditorIfConfigured() {
		this.handler.setAuditorAware(this.auditorAware);
		this.handler.markCreated(this.user);
		assertThat(this.user.getCreatedDate()).isPresent();
		assertThat(this.user.getLastModifiedDate()).isPresent();
		assertThat(this.user.getCreatedBy()).isPresent();
		assertThat(this.user.getLastModifiedBy()).isPresent();
		verify(this.auditorAware).getCurrentAuditor();
	}

	/**
	 * Checks that the advice does not set modification information on creation if the
	 * falg is set to {@code false}.
	 */
	@Test
	void honoursModifiedOnCreationFlag() {
		this.handler.setAuditorAware(this.auditorAware);
		this.handler.setModifyOnCreation(false);
		this.handler.markCreated(this.user);
		assertThat(this.user.getCreatedDate()).isPresent();
		assertThat(this.user.getCreatedBy()).isPresent();
		assertThat(this.user.getLastModifiedBy()).isNotPresent();
		assertThat(this.user.getLastModifiedDate()).isNotPresent();
		verify(this.auditorAware).getCurrentAuditor();
	}

	/**
	 * Tests that the advice only sets modification data if a not-new entity is handled.
	 */
	@Test
	void onlySetsModificationDataOnNotNewEntities() {
		AuditedUser audited = new AuditedUser();
		audited.id = 1L;
		this.handler.setAuditorAware(this.auditorAware);
		this.handler.markModified(audited);
		assertThat(audited.getCreatedBy()).isNotPresent();
		assertThat(audited.getCreatedDate()).isNotPresent();
		assertThat(audited.getLastModifiedBy()).isPresent();
		assertThat(audited.getLastModifiedDate()).isPresent();
		verify(this.auditorAware).getCurrentAuditor();
	}

	@Test
	void doesNotSetTimeIfConfigured() {
		this.handler.setDateTimeForNow(false);
		this.handler.setAuditorAware(this.auditorAware);
		this.handler.markCreated(this.user);
		assertThat(this.user.getCreatedBy()).isPresent();
		assertThat(this.user.getCreatedDate()).isNotPresent();
		assertThat(this.user.getLastModifiedBy()).isPresent();
		assertThat(this.user.getLastModifiedDate()).isNotPresent();
	}

	@Test // DATAJPA-9
	void usesDateTimeProviderIfConfigured() {
		DateTimeProvider provider = mock(DateTimeProvider.class);
		doReturn(Optional.empty()).when(provider).getNow();
		this.handler.setDateTimeProvider(provider);
		this.handler.markCreated(this.user);
		verify(provider, times(1)).getNow();
	}

	@Test
	void setsAuditingInfoOnEntityUsingInheritance() {
		AuditingHandler handler = new AuditingHandler(PersistentEntities.of(new SampleMappingContext()));
		handler.setModifyOnCreation(false);
		MyDocument result = handler.markCreated(new MyDocument());
		assertThat(result.created).isNotNull();
		assertThat(result.modified).isNull();
		result = handler.markModified(result);
		assertThat(result.created).isNotNull();
		assertThat(result.modified).isNotNull();
	}

	static abstract class AbstractModel {

		@CreatedDate
		Instant created;

		@CreatedBy
		String creator;

		@LastModifiedDate
		Instant modified;

		@LastModifiedBy
		String modifier;

	}

	static class MyModel extends AbstractModel {

		List<MyModel> models;

	}

	static class MyDocument extends MyModel {

	}

}
