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

package org.springframework.data.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Interface to access property types and resolving generics on the way. Starting with a
 * {@link ClassTypeInformation} you can traverse properties using
 * {@link #getProperty(String)} to access type information.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 */
public interface TypeInformation<S> {

	/**
	 * Returns the {@link TypeInformation}s for the parameters of the given
	 * {@link Constructor}.
	 * @param constructor must not be {@literal null}.
	 * @return
	 */
	List<TypeInformation<?>> getParameterTypes(Constructor<?> constructor);

	/**
	 * Returns the property information for the property with the given name. Supports
	 * property traversal through dot notation.
	 * @param property
	 * @return
	 */
	@Nullable
	TypeInformation<?> getProperty(String property);

	/**
	 * Returns the property information for the property with the given name or throw
	 * {@link IllegalArgumentException} if the type information cannot be resolved.
	 * Supports property traversal through dot notation.
	 * @param property
	 * @return the property information
	 * @throws IllegalArgumentException if the type information cannot be resolved.
	 * @since 2.0
	 */
	default TypeInformation<?> getRequiredProperty(String property) {
		TypeInformation<?> typeInformation = getProperty(property);
		Assert.notNull(typeInformation,
				() -> String.format("Could not find required property %s on %s!", property, getType()));
		return typeInformation;
	}

	/**
	 * Returns whether the type can be considered a collection, which means it's a
	 * container of elements, e.g. a {@link java.util.Collection} and
	 * {@link java.lang.reflect.Array} or anything implementing {@link Iterable}. If this
	 * returns {@literal true} you can expect {@link #getComponentType()} to return a
	 * non-{@literal null} value.
	 * @return
	 */
	boolean isCollectionLike();

	/**
	 * Returns the component type for {@link java.util.Collection}s or the key type for
	 * {@link java.util.Map}s.
	 * @return
	 */
	@Nullable
	TypeInformation<?> getComponentType();

	/**
	 * Returns the component type for {@link java.util.Collection}s, the key type for
	 * {@link java.util.Map}s or the single generic type if available. Throws
	 * {@link IllegalStateException} if the component value type cannot be resolved.
	 * @return the component type
	 * @throws IllegalStateException if the component type cannot be resolved, e.g. if a
	 * raw type is used or the type is not generic in the first place.
	 * @since 2.0
	 */
	default TypeInformation<?> getRequiredComponentType() {
		TypeInformation<?> componentType = getComponentType();
		Assert.state(componentType != null,
				() -> String.format("Can't resolve required component type for %s!", getType()));
		return componentType;
	}

	/**
	 * Returns whether the property is a {@link java.util.Map}. If this returns
	 * {@literal true} you can expect {@link #getComponentType()} as well as
	 * {@link #getMapValueType()} to return something not {@literal null}.
	 * @return
	 */
	boolean isMap();

	/**
	 * Will return the type of the value in case the underlying type is a
	 * {@link java.util.Map}.
	 * @return
	 */
	@Nullable
	TypeInformation<?> getMapValueType();

	/**
	 * Will return the type of the value in case the underlying type is a
	 * {@link java.util.Map} or throw {@link IllegalStateException} if the map value type
	 * cannot be resolved.
	 * @return the map value type
	 * @throws IllegalStateException if the map value type cannot be resolved, usually due
	 * to the current {@link java.util.Map} type being a raw one.
	 * @since 2.0
	 */
	default TypeInformation<?> getRequiredMapValueType() {
		TypeInformation<?> mapValueType = getMapValueType();
		Assert.state(mapValueType != null,
				() -> String.format("Can't resolve required map value type for %s!", getType()));
		return mapValueType;
	}

	/**
	 * Returns the type of the property. Will resolve generics and the generic context of
	 * @return
	 */
	Class<S> getType();

	/**
	 * Returns a {@link ClassTypeInformation} to represent the {@link TypeInformation} of
	 * the raw type of the current instance.
	 * @return
	 */
	ClassTypeInformation<?> getRawTypeInformation();

	/**
	 * Transparently returns the {@link java.util.Map} value type if the type is a
	 * {@link java.util.Map}, returns the component type if the type
	 * {@link #isCollectionLike()} or the simple type if none of this applies.
	 * @return the map value, collection component type or the current type,
	 * {@literal null} it the current type is a raw {@link java.util.Map} or
	 * {@link java.util.Collection}.
	 */
	@Nullable
	TypeInformation<?> getActualType();

	/**
	 * Transparently returns the {@link java.util.Map} value type if the type is a
	 * {@link java.util.Map}, returns the component type if the type
	 * {@link #isCollectionLike()} or the simple type if none of this applies.
	 * @return the actual type
	 * @throws IllegalArgumentException if the current type is a raw {@link java.util.Map}
	 * or {@link java.util.Collection} and no value or component type is available.
	 * @since 2.0
	 */
	default TypeInformation<?> getRequiredActualType() {
		TypeInformation<?> result = getActualType();
		Assert.state(result != null,
				"Expected to be able to resolve a type but got null! This usually stems from types implementing raw Map or Collection interfaces!");
		return result;
	}

	/**
	 * Returns a {@link TypeInformation} for the return type of the given {@link Method}.
	 * Will potentially resolve generics information against the current types type
	 * parameter bindings.
	 * @param method must not be {@literal null}.
	 * @return
	 */
	TypeInformation<?> getReturnType(Method method);

	/**
	 * Returns the {@link TypeInformation}s for the parameters of the given
	 * {@link Method}.
	 * @param method must not be {@literal null}.
	 * @return
	 */
	List<TypeInformation<?>> getParameterTypes(Method method);

	/**
	 * Returns the {@link TypeInformation} for the given raw super type.
	 * @param superType must not be {@literal null}.
	 * @return the {@link TypeInformation} for the given raw super type or {@literal null}
	 * in case the current {@link TypeInformation} does not implement the given type.
	 */
	@Nullable
	TypeInformation<?> getSuperTypeInformation(Class<?> superType);

	/**
	 * Returns the {@link TypeInformation} for the given raw super type.
	 * @param superType must not be {@literal null}.
	 * @return the {@link TypeInformation} for the given raw super type.
	 * @throws IllegalArgumentException in case the current {@link TypeInformation} does
	 * not implement the given type.
	 * @since 2.0
	 */
	default TypeInformation<?> getRequiredSuperTypeInformation(Class<?> superType) {
		TypeInformation<?> result = getSuperTypeInformation(superType);
		Assert.notNull(result, () -> String.format(
				"Can't retrieve super type information for %s! Does current type really implement the given one?",
				superType));
		return result;
	}

	/**
	 * Returns if the current {@link TypeInformation} can be safely assigned to the given
	 * one. Mimics semantics of {@link Class#isAssignableFrom(Class)} but takes generics
	 * into account. Thus it will allow to detect that a {@code List<Long>} is assignable
	 * to {@code List<? extends Number>}.
	 * @param target
	 * @return
	 */
	boolean isAssignableFrom(TypeInformation<?> target);

	/**
	 * Returns the {@link TypeInformation} for the type arguments of the current
	 * {@link TypeInformation}.
	 * @return
	 */
	List<TypeInformation<?>> getTypeArguments();

	/**
	 * Specializes the given (raw) {@link ClassTypeInformation} using the context of the
	 * current potentially parameterized type, basically turning the given raw type into a
	 * parameterized one. Will return the given type as is if no generics are involved.
	 * @param type must not be {@literal null}.
	 * @return will never be {@literal null}.
	 */
	TypeInformation<? extends S> specialize(ClassTypeInformation<?> type);

	/**
	 * Returns whether the current type is a sub type of the given one, i.e. whether it's
	 * assignable but not the same one.
	 * @param type must not be {@literal null}.
	 * @return if the current type is a subtype of the given class
	 * @since 2.2
	 */
	default boolean isSubTypeOf(Class<?> type) {
		return !type.equals(getType()) && type.isAssignableFrom(getType());
	}

}
