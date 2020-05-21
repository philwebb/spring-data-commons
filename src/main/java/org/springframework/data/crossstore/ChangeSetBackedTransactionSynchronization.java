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
package org.springframework.data.crossstore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.transaction.support.TransactionSynchronization;

public class ChangeSetBackedTransactionSynchronization implements TransactionSynchronization {

	protected final Logger log = LoggerFactory.getLogger(getClass());

	private final ChangeSetPersister<Object> changeSetPersister;

	private final ChangeSetBacked entity;

	private int changeSetTxStatus = -1;

	public ChangeSetBackedTransactionSynchronization(ChangeSetPersister<Object> changeSetPersister,
			ChangeSetBacked entity) {
		this.changeSetPersister = changeSetPersister;
		this.entity = entity;
	}

	@Override
	public void afterCommit() {
		this.log.debug("After Commit called for " + this.entity);
		this.changeSetPersister.persistState(this.entity, this.entity.getChangeSet());
		this.changeSetTxStatus = 0;
	}

	@Override
	public void afterCompletion(int status) {
		this.log.debug("After Completion called with status = " + status);
		if (this.changeSetTxStatus == 0) {
			if (status == STATUS_COMMITTED) {
				// this is good
				this.log.debug("ChangedSetBackedTransactionSynchronization completed successfully for " + this.entity);
			}
			else {
				// this could be bad - TODO: compensate
				this.log.error("ChangedSetBackedTransactionSynchronization failed for " + this.entity);
			}
		}
	}

	@Override
	public void beforeCommit(boolean readOnly) {
	}

	@Override
	public void beforeCompletion() {
	}

	@Override
	public void flush() {
	}

	@Override
	public void resume() {
		throw new IllegalStateException(
				"ChangedSetBackedTransactionSynchronization does not support transaction suspension currently.");
	}

	@Override
	public void suspend() {
		throw new IllegalStateException(
				"ChangedSetBackedTransactionSynchronization does not support transaction suspension currently.");
	}

}
