/*
 * Copyright 2014-2020 the original author or authors.
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

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import org.springframework.data.classloadersupport.HidingClassLoader;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PreferredConstructor;
import org.springframework.data.mapping.PreferredConstructor.Parameter;
import org.springframework.data.mapping.model.ClassGeneratingEntityInstantiator.ObjectInstantiator;
import org.springframework.data.mapping.model.ClassGeneratingEntityInstantiatorUnitTests.Outer.Inner;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link ClassGeneratingEntityInstantiator}.
 *
 * @author Thomas Darimont
 * @author Oliver Gierke
 * @author Mark Paluch
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ClassGeneratingEntityInstantiatorUnitTests<P extends PersistentProperty<P>> {

	ClassGeneratingEntityInstantiator instance = new ClassGeneratingEntityInstantiator();

	@Mock
	PersistentEntity<?, P> entity;

	@Mock
	ParameterValueProvider<P> provider;

	@Test
	void instantiatesSimpleObjectCorrectly() {
		willReturn(Object.class).given(this.entity).getType();
		this.instance.createInstance(this.entity, this.provider);
	}

	@Test
	void instantiatesArrayCorrectly() {
		willReturn(String[][].class).given(this.entity).getType();
		this.instance.createInstance(this.entity, this.provider);
	}

	@Test // DATACMNS-1126
	void instantiatesTypeWithPreferredConstructorUsingParameterValueProvider() {
		PreferredConstructor<Foo, P> constructor = PreferredConstructorDiscoverer.discover(Foo.class);
		willReturn(Foo.class).given(this.entity).getType();
		willReturn(constructor).given(this.entity).getPersistenceConstructor();
		assertThat(this.instance.createInstance(this.entity, this.provider)).isInstanceOf(Foo.class);
		assertThat(constructor).satisfies(
				it -> verify(this.provider, times(1)).getParameterValue(it.getParameters().iterator().next()));
	}

	@Test // DATACMNS-300, DATACMNS-578
	@SuppressWarnings({ "unchecked", "rawtypes" })
	void throwsExceptionOnBeanInstantiationException() {
		willReturn(PersistentEntity.class).given(this.entity).getType();
		assertThatExceptionOfType(MappingInstantiationException.class)
				.isThrownBy(() -> this.instance.createInstance(this.entity, this.provider));
	}

	@Test // DATACMNS-134, DATACMNS-578
	void createsInnerClassInstanceCorrectly() {
		BasicPersistentEntity<Inner, P> entity = new BasicPersistentEntity<>(ClassTypeInformation.from(Inner.class));
		assertThat(entity.getPersistenceConstructor()).satisfies(constructor -> {
			Parameter<Object, P> parameter = constructor.getParameters().iterator().next();
			Object outer = new Outer();
			willReturn(outer).given(this.provider).getParameterValue(parameter);
			Inner instance = this.instance.createInstance(entity, this.provider);
			assertThat(instance).isNotNull();
			// Hack to check synthetic field as compiles create different field names
			// (e.g. this$0, this$1)
			ReflectionUtils.doWithFields(Inner.class, field -> {
				if (field.isSynthetic() && field.getName().startsWith("this$")) {
					ReflectionUtils.makeAccessible(field);
					assertThat(ReflectionUtils.getField(field, instance)).isEqualTo(outer);
				}
			});
		});
	}

	@Test // DATACMNS-283, DATACMNS-578
	@SuppressWarnings({ "unchecked", "rawtypes" })
	void capturesContextOnInstantiationException() throws Exception {
		PersistentEntity<Sample, P> entity = new BasicPersistentEntity<>(ClassTypeInformation.from(Sample.class));
		willReturn("FOO").given(this.provider).getParameterValue(any(Parameter.class));
		Constructor constructor = Sample.class.getConstructor(Long.class, String.class);
		List<Object> parameters = Arrays.asList("FOO", "FOO");
		try {
			this.instance.createInstance(entity, this.provider);
			fail("Expected MappingInstantiationException!");
		}
		catch (MappingInstantiationException ex) {
			assertThat(ex.getConstructor()).hasValue(constructor);
			assertThat(ex.getConstructorArguments()).isEqualTo(parameters);
			assertThat(ex.getEntityType()).hasValue(Sample.class);
			assertThat(ex.getMessage()).contains(Sample.class.getName());
			assertThat(ex.getMessage()).contains(Long.class.getName());
			assertThat(ex.getMessage()).contains(String.class.getName());
			assertThat(ex.getMessage()).contains("FOO");
		}
	}

	@Test // DATACMNS-1175
	@SuppressWarnings({ "unchecked", "rawtypes" })
	void createsInstancesWithRecursionAndSameCtorArgCountCorrectly() {
		PersistentEntity<SampleWithReference, P> outer = new BasicPersistentEntity<>(
				ClassTypeInformation.from(SampleWithReference.class));
		PersistentEntity<Sample, P> inner = new BasicPersistentEntity<>(ClassTypeInformation.from(Sample.class));
		willReturn(2L, "FOO").given(this.provider).getParameterValue(any(Parameter.class));
		ParameterValueProvider<P> recursive = new ParameterValueProvider<P>() {

			@Override
			public <T> T getParameterValue(Parameter<T, P> parameter) {
				if (parameter.getName().equals("id")) {
					return (T) Long.valueOf(1);
				}
				if (parameter.getName().equals("sample")) {
					return (T) ClassGeneratingEntityInstantiatorUnitTests.this.instance.createInstance(inner,
							ClassGeneratingEntityInstantiatorUnitTests.this.provider);
				}
				throw new UnsupportedOperationException(parameter.getName());
			}

		};
		SampleWithReference reference = this.instance.createInstance(outer, recursive);
		assertThat(reference.id).isEqualTo(1L);
		assertThat(reference.sample).isNotNull();
		assertThat(reference.sample.id).isEqualTo(2L);
		assertThat(reference.sample.name).isEqualTo("FOO");
	}

	@Test // DATACMNS-578, DATACMNS-1126
	void instantiateObjCtorDefault() {
		willReturn(ObjCtorDefault.class).given(this.entity).getType();
		willReturn(PreferredConstructorDiscoverer.discover(ObjCtorDefault.class)).given(this.entity)
				.getPersistenceConstructor();
		IntStream.range(0, 2).forEach(i -> assertThat(this.instance.createInstance(this.entity, this.provider))
				.isInstanceOf(ObjCtorDefault.class));
	}

	@Test // DATACMNS-578, DATACMNS-1126
	void instantiateObjCtorNoArgs() {
		willReturn(ObjCtorNoArgs.class).given(this.entity).getType();
		willReturn(PreferredConstructorDiscoverer.discover(ObjCtorNoArgs.class)).given(this.entity)
				.getPersistenceConstructor();
		IntStream.range(0, 2).forEach(i -> {
			Object instance = this.instance.createInstance(this.entity, this.provider);
			assertThat(instance).isInstanceOf(ObjCtorNoArgs.class);
			assertThat(((ObjCtorNoArgs) instance).ctorInvoked).isTrue();
		});
	}

	@Test // DATACMNS-578, DATACMNS-1126
	void instantiateObjCtor1ParamString() {
		willReturn(ObjCtor1ParamString.class).given(this.entity).getType();
		willReturn(PreferredConstructorDiscoverer.discover(ObjCtor1ParamString.class)).given(this.entity)
				.getPersistenceConstructor();
		willReturn("FOO").given(this.provider).getParameterValue(any());
		IntStream.range(0, 2).forEach(i -> {
			Object instance = this.instance.createInstance(this.entity, this.provider);
			assertThat(instance).isInstanceOf(ObjCtor1ParamString.class);
			assertThat(((ObjCtor1ParamString) instance).ctorInvoked).isTrue();
			assertThat(((ObjCtor1ParamString) instance).param1).isEqualTo("FOO");
		});
	}

	@Test // DATACMNS-578, DATACMNS-1126
	void instantiateObjCtor2ParamStringString() {
		willReturn(ObjCtor2ParamStringString.class).given(this.entity).getType();
		willReturn(PreferredConstructorDiscoverer.discover(ObjCtor2ParamStringString.class)).given(this.entity)
				.getPersistenceConstructor();
		IntStream.range(0, 2).forEach(i -> {
			given(this.provider.getParameterValue(any())).willReturn("FOO", "BAR");
			Object instance = this.instance.createInstance(this.entity, this.provider);
			assertThat(instance).isInstanceOf(ObjCtor2ParamStringString.class);
			assertThat(((ObjCtor2ParamStringString) instance).ctorInvoked).isTrue();
			assertThat(((ObjCtor2ParamStringString) instance).param1).isEqualTo("FOO");
			assertThat(((ObjCtor2ParamStringString) instance).param2).isEqualTo("BAR");
		});
	}

	@Test // DATACMNS-578, DATACMNS-1126
	void instantiateObjectCtor1ParamInt() {
		willReturn(ObjectCtor1ParamInt.class).given(this.entity).getType();
		willReturn(PreferredConstructorDiscoverer.discover(ObjectCtor1ParamInt.class)).given(this.entity)
				.getPersistenceConstructor();
		IntStream.range(0, 2).forEach(i -> {
			willReturn(42).given(this.provider).getParameterValue(any());
			Object instance = this.instance.createInstance(this.entity, this.provider);
			assertThat(instance).isInstanceOf(ObjectCtor1ParamInt.class);
			assertThat(((ObjectCtor1ParamInt) instance).param1).isEqualTo(42);
		});
	}

	@Test // DATACMNS-1200
	void instantiateObjectCtor1ParamIntWithoutValue() {
		willReturn(ObjectCtor1ParamInt.class).given(this.entity).getType();
		willReturn(PreferredConstructorDiscoverer.discover(ObjectCtor1ParamInt.class)).given(this.entity)
				.getPersistenceConstructor();
		assertThatThrownBy(() -> this.instance.createInstance(this.entity, this.provider))
				.hasCauseInstanceOf(IllegalArgumentException.class);
	}

	@Test // DATACMNS-578, DATACMNS-1126
	@SuppressWarnings("unchecked")
	void instantiateObjectCtor7ParamsString5IntsString() {
		willReturn(ObjectCtor7ParamsString5IntsString.class).given(this.entity).getType();
		willReturn(PreferredConstructorDiscoverer.discover(ObjectCtor7ParamsString5IntsString.class)).given(this.entity)
				.getPersistenceConstructor();
		IntStream.range(0, 2).forEach(i -> {
			given(this.provider.getParameterValue(any(Parameter.class))).willReturn("A", 1, 2, 3, 4, 5, "B");
			Object instance = this.instance.createInstance(this.entity, this.provider);
			assertThat(instance).isInstanceOf(ObjectCtor7ParamsString5IntsString.class);
			ObjectCtor7ParamsString5IntsString toTest = (ObjectCtor7ParamsString5IntsString) instance;
			assertThat(toTest.param1).isEqualTo("A");
			assertThat(toTest.param2).isEqualTo(1);
			assertThat(toTest.param3).isEqualTo(2);
			assertThat(toTest.param4).isEqualTo(3);
			assertThat(toTest.param5).isEqualTo(4);
			assertThat(toTest.param6).isEqualTo(5);
			assertThat(toTest.param7).isEqualTo("B");
		});
	}

	@Test // DATACMNS-1373
	void shouldInstantiateProtectedInnerClass() {
		prepareMocks(ProtectedInnerClass.class);
		assertThat(this.instance.shouldUseReflectionEntityInstantiator(this.entity)).isFalse();
		assertThat(this.instance.createInstance(this.entity, this.provider)).isInstanceOf(ProtectedInnerClass.class);
	}

	@Test // DATACMNS-1373
	void shouldInstantiatePackagePrivateInnerClass() {
		prepareMocks(PackagePrivateInnerClass.class);
		assertThat(this.instance.shouldUseReflectionEntityInstantiator(this.entity)).isFalse();
		assertThat(this.instance.createInstance(this.entity, this.provider))
				.isInstanceOf(PackagePrivateInnerClass.class);
	}

	@Test // DATACMNS-1373
	void shouldNotInstantiatePrivateInnerClass() {
		prepareMocks(PrivateInnerClass.class);
		assertThat(this.instance.shouldUseReflectionEntityInstantiator(this.entity)).isTrue();
	}

	@Test // DATACMNS-1373
	void shouldInstantiateClassWithPackagePrivateConstructor() {
		prepareMocks(ClassWithPackagePrivateConstructor.class);
		assertThat(this.instance.shouldUseReflectionEntityInstantiator(this.entity)).isFalse();
		assertThat(this.instance.createInstance(this.entity, this.provider))
				.isInstanceOf(ClassWithPackagePrivateConstructor.class);
	}

	@Test // DATACMNS-1373
	void shouldInstantiateClassInDefaultPackage() throws ClassNotFoundException {
		Class<?> typeInDefaultPackage = Class.forName("TypeInDefaultPackage");
		prepareMocks(typeInDefaultPackage);
		assertThat(this.instance.shouldUseReflectionEntityInstantiator(this.entity)).isFalse();
		assertThat(this.instance.createInstance(this.entity, this.provider)).isInstanceOf(typeInDefaultPackage);
	}

	@Test // DATACMNS-1373
	void shouldNotInstantiateClassWithPrivateConstructor() {
		prepareMocks(ClassWithPrivateConstructor.class);
		assertThat(this.instance.shouldUseReflectionEntityInstantiator(this.entity)).isTrue();
	}

	@Test // DATACMNS-1422
	void shouldUseReflectionIfFrameworkTypesNotVisible() throws Exception {
		HidingClassLoader classLoader = HidingClassLoader.hide(ObjectInstantiator.class);
		classLoader.excludePackage("org.springframework.data.mapping");
		// require type from different package to meet visibility quirks
		Class<?> entityType = classLoader.loadClass("org.springframework.data.mapping.Person");
		prepareMocks(entityType);
		assertThat(this.instance.shouldUseReflectionEntityInstantiator(this.entity)).isTrue();
	}

	private void prepareMocks(Class<?> type) {
		willReturn(type).given(this.entity).getType();
		willReturn(PreferredConstructorDiscoverer.discover(type)).given(this.entity).getPersistenceConstructor();
	}

	static class Foo {

		Foo(String foo) {

		}

	}

	static class Outer {

		class Inner {

		}

	}

	static class Sample {

		final Long id;

		final String name;

		public Sample(Long id, String name) {
			this.id = id;
			this.name = name;
		}

	}

	static class SampleWithReference {

		final Long id;

		final Sample sample;

		public SampleWithReference(Long id, Sample sample) {
			this.id = id;
			this.sample = sample;
		}

	}

	public static class ObjCtorDefault {

	}

	public static class ObjCtorNoArgs {

		public boolean ctorInvoked;

		public ObjCtorNoArgs() {
			this.ctorInvoked = true;
		}

	}

	public static class ObjCtor1ParamString {

		public boolean ctorInvoked;

		public String param1;

		public ObjCtor1ParamString(String param1) {
			this.param1 = param1;
			this.ctorInvoked = true;
		}

	}

	public static class ObjCtor2ParamStringString {

		public boolean ctorInvoked;

		public String param1;

		public String param2;

		public ObjCtor2ParamStringString(String param1, String param2) {
			this.ctorInvoked = true;
			this.param1 = param1;
			this.param2 = param2;
		}

	}

	public static class ObjectCtor1ParamInt {

		public int param1;

		public ObjectCtor1ParamInt(int param1) {
			this.param1 = param1;
		}

	}

	public static class ObjectCtor7ParamsString5IntsString {

		public String param1;

		public int param2;

		public int param3;

		public int param4;

		public int param5;

		public int param6;

		public String param7;

		public ObjectCtor7ParamsString5IntsString(String param1, int param2, int param3, int param4, int param5,
				int param6, String param7) {
			this.param1 = param1;
			this.param2 = param2;
			this.param3 = param3;
			this.param4 = param4;
			this.param5 = param5;
			this.param6 = param6;
			this.param7 = param7;
		}

	}

	protected static class ProtectedInnerClass {

	}

	static class PackagePrivateInnerClass {

	}

	private static class PrivateInnerClass {

	}

	static class ClassWithPrivateConstructor {

		private ClassWithPrivateConstructor() {
		}

	}

	static class ClassWithPackagePrivateConstructor {

		ClassWithPackagePrivateConstructor() {
		}

	}

}
