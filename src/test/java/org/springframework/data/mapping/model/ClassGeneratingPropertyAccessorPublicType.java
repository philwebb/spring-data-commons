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

package org.springframework.data.mapping.model;

import org.springframework.data.annotation.AccessType;
import org.springframework.data.annotation.AccessType.Type;

/**
 * @author Mark Paluch
 * @author Oliver Gierke
 */
@SuppressWarnings("unused")
public class ClassGeneratingPropertyAccessorPublicType {

	private String privateField;

	String packageDefaultField;

	protected String protectedField;

	public String publicField;

	private String backing;

	private final String immutable = "";

	private final String wither;

	private Integer aa;

	private int bb;

	@AccessType(Type.PROPERTY)
	private String privateProperty;

	@AccessType(Type.PROPERTY)
	private String packageDefaultProperty;

	@AccessType(Type.PROPERTY)
	private String protectedProperty;

	@AccessType(Type.PROPERTY)
	private String publicProperty;

	public ClassGeneratingPropertyAccessorPublicType() {
		this.wither = "";
	}

	private ClassGeneratingPropertyAccessorPublicType(String wither) {
		this.wither = wither;
	}

	private String getPrivateProperty() {
		return this.privateProperty;
	}

	private void setPrivateProperty(String privateProperty) {
		this.privateProperty = privateProperty;
	}

	String getPackageDefaultProperty() {
		return this.packageDefaultProperty;
	}

	void setPackageDefaultProperty(String packageDefaultProperty) {
		this.packageDefaultProperty = packageDefaultProperty;
	}

	protected String getProtectedProperty() {
		return this.protectedProperty;
	}

	protected void setProtectedProperty(String protectedProperty) {
		this.protectedProperty = protectedProperty;
	}

	public String getPublicProperty() {
		return this.publicProperty;
	}

	public void setPublicProperty(String publicProperty) {
		this.publicProperty = publicProperty;
	}

	@AccessType(Type.PROPERTY)
	public String getSyntheticProperty() {
		return this.backing;
	}

	public void setSyntheticProperty(String syntheticProperty) {
		this.backing = syntheticProperty;
	}

	public String getWither() {
		return this.wither;
	}

	public ClassGeneratingPropertyAccessorPublicType withWither(String wither) {
		return new ClassGeneratingPropertyAccessorPublicType(wither);
	}

	public Object set(Object e) {
		this.aa = (Integer) e;
		this.bb = (Integer) e;
		return this.bb;
	}

	public static void main(String[] args) {
	}

}
