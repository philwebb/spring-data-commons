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

package org.springframework.data.mapping;

import java.beans.Introspector;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.Streamable;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Abstraction of a {@link PropertyPath} of a domain class.
 *
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @author Mark Paluch
 * @author Mariusz Mączkowski
 */
public class PropertyPath implements Streamable<PropertyPath> {

	private static final String PARSE_DEPTH_EXCEEDED = "Trying to parse a path with depth greater than 1000! This has been disabled for security reasons to prevent parsing overflows.";

	private static final String DELIMITERS = "_\\.";

	private static final String ALL_UPPERCASE = "[A-Z0-9._$]+";

	private static final Pattern SPLITTER = Pattern.compile("(?:[%s]?([%s]*?[^%s]+))".replaceAll("%s", DELIMITERS));

	private static final Pattern SPLITTER_FOR_QUOTED = Pattern
			.compile("(?:[%s]?([%s]*?[^%s]+))".replaceAll("%s", "\\."));

	private static final Map<Key, PropertyPath> cache = new ConcurrentReferenceHashMap<>();

	private final TypeInformation<?> owningType;

	private final String name;

	private final TypeInformation<?> typeInformation;

	private final TypeInformation<?> actualTypeInformation;

	private final boolean isCollection;

	@Nullable
	private PropertyPath next;

	/**
	 * Creates a leaf {@link PropertyPath} (no nested ones) with the given name inside the
	 * given owning type.
	 * @param name must not be {@literal null} or empty.
	 * @param owningType must not be {@literal null}.
	 */
	PropertyPath(String name, Class<?> owningType) {
		this(name, ClassTypeInformation.from(owningType), Collections.emptyList());
	}

	/**
	 * Creates a leaf {@link PropertyPath} (no nested ones with the given name and owning
	 * type.
	 * @param name must not be {@literal null} or empty.
	 * @param owningType must not be {@literal null}.
	 * @param base the {@link PropertyPath} previously found.
	 */
	PropertyPath(String name, TypeInformation<?> owningType, List<PropertyPath> base) {
		Assert.hasText(name, "Name must not be null or empty!");
		Assert.notNull(owningType, "Owning type must not be null!");
		Assert.notNull(base, "Perviously found properties must not be null!");
		String propertyName = Introspector.decapitalize(name);
		TypeInformation<?> propertyType = owningType.getProperty(propertyName);
		if (propertyType == null) {
			throw new PropertyReferenceException(propertyName, owningType, base);
		}
		this.owningType = owningType;
		this.typeInformation = propertyType;
		this.isCollection = propertyType.isCollectionLike();
		this.name = propertyName;
		this.actualTypeInformation = (propertyType.getActualType() != null) ? propertyType.getRequiredActualType()
				: propertyType;
	}

	/**
	 * Returns the owning type of the {@link PropertyPath}.
	 * @return the owningType will never be {@literal null}.
	 */
	public TypeInformation<?> getOwningType() {
		return this.owningType;
	}

	/**
	 * Returns the name of the {@link PropertyPath}.
	 * @return the name will never be {@literal null}.
	 */
	public String getSegment() {
		return this.name;
	}

	/**
	 * Returns the leaf property of the {@link PropertyPath}.
	 * @return will never be {@literal null}.
	 */
	public PropertyPath getLeafProperty() {
		PropertyPath result = this;
		while (result.hasNext()) {
			result = result.requiredNext();
		}
		return result;
	}

	/**
	 * Returns the type of the leaf property of the current {@link PropertyPath}.
	 * @return will never be {@literal null}.
	 */
	public Class<?> getLeafType() {
		return getLeafProperty().getType();
	}

	/**
	 * Returns the type of the property will return the plain resolved type for simple
	 * properties, the component type for any {@link Iterable} or the value type of a
	 * {@link java.util.Map} if the property is one.
	 * @return
	 */
	public Class<?> getType() {
		return this.actualTypeInformation.getType();
	}

	public TypeInformation<?> getTypeInformation() {
		return this.typeInformation;
	}

	/**
	 * Returns the next nested {@link PropertyPath}.
	 * @return the next nested {@link PropertyPath} or {@literal null} if no nested
	 * {@link PropertyPath} available.
	 * @see #hasNext()
	 */
	@Nullable
	public PropertyPath next() {
		return this.next;
	}

	/**
	 * Returns whether there is a nested {@link PropertyPath}. If this returns
	 * {@literal true} you can expect {@link #next()} to return a non- {@literal null}
	 * value.
	 * @return
	 */
	public boolean hasNext() {
		return this.next != null;
	}

	/**
	 * Returns the {@link PropertyPath} in dot notation.
	 * @return
	 */
	public String toDotPath() {
		if (hasNext()) {
			return getSegment() + "." + requiredNext().toDotPath();
		}
		return getSegment();
	}

	/**
	 * Returns whether the {@link PropertyPath} is actually a collection.
	 * @return
	 */
	public boolean isCollection() {
		return this.isCollection;
	}

	/**
	 * Returns the {@link PropertyPath} for the path nested under the current property.
	 * @param path must not be {@literal null} or empty.
	 * @return will never be {@literal null}.
	 */
	public PropertyPath nested(String path) {
		Assert.hasText(path, "Path must not be null or empty!");
		String lookup = toDotPath().concat(".").concat(path);
		return PropertyPath.from(lookup, this.owningType);
	}

	@Override
	public Iterator<PropertyPath> iterator() {
		return new Iterator<PropertyPath>() {

			@Nullable
			private PropertyPath current = PropertyPath.this;

			@Override
			public boolean hasNext() {
				return this.current != null;
			}

			@Override
			@Nullable
			public PropertyPath next() {
				PropertyPath result = this.current;
				if (result == null) {
					return null;
				}
				this.current = result.next();
				return result;
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}

		};
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof PropertyPath)) {
			return false;
		}
		PropertyPath other = (PropertyPath) obj;
		boolean result = true;
		result = result && this.isCollection == other.isCollection;
		result = result && ObjectUtils.nullSafeEquals(this.owningType, other.owningType);
		result = result && ObjectUtils.nullSafeEquals(this.name, other.name);
		result = result && ObjectUtils.nullSafeEquals(this.typeInformation, other.typeInformation);
		result = result && ObjectUtils.nullSafeEquals(this.actualTypeInformation, other.actualTypeInformation);
		result = result && ObjectUtils.nullSafeEquals(this.next, other.next);
		return result;
	}

	@Override
	public int hashCode() {
		int result = ObjectUtils.nullSafeHashCode(this.owningType);
		result = 31 * result + ObjectUtils.nullSafeHashCode(this.name);
		result = 31 * result + ObjectUtils.nullSafeHashCode(this.typeInformation);
		result = 31 * result + ObjectUtils.nullSafeHashCode(this.actualTypeInformation);
		result = 31 * result + (this.isCollection ? 1 : 0);
		result = 31 * result + ObjectUtils.nullSafeHashCode(this.next);
		return result;
	}

	/**
	 * Returns the next {@link PropertyPath}.
	 * @return the next path
	 * @throws IllegalStateException it there's no next one.
	 */
	private PropertyPath requiredNext() {
		PropertyPath result = this.next;
		Assert.state(result != null,
				"No next path available! Clients should call hasNext() before invoking this method!");
		return result;
	}

	@Override
	public String toString() {
		return String.format("%s.%s", this.owningType.getType().getSimpleName(), toDotPath());
	}

	/**
	 * Extracts the {@link PropertyPath} chain from the given source {@link String} and
	 * type.
	 * @param source
	 * @param type
	 * @return
	 */
	public static PropertyPath from(String source, Class<?> type) {
		return from(source, ClassTypeInformation.from(type));
	}

	/**
	 * Extracts the {@link PropertyPath} chain from the given source {@link String} and
	 * {@link TypeInformation}. <br />
	 * Uses {@link #SPLITTER} by default and {@link #SPLITTER_FOR_QUOTED} for
	 * {@link Pattern#quote(String) quoted} literals.
	 * @param source must not be {@literal null}.
	 * @param type
	 * @return
	 */
	public static PropertyPath from(String source, TypeInformation<?> type) {
		Assert.hasText(source, "Source must not be null or empty!");
		Assert.notNull(type, "TypeInformation must not be null or empty!");
		return cache.computeIfAbsent(Key.of(type, source), (it) -> {
			List<String> iteratorSource = new ArrayList<>();
			Matcher matcher = isQuoted(it.path)
					? SPLITTER_FOR_QUOTED.matcher(it.path.replace("\\Q", "").replace("\\E", ""))
					: SPLITTER.matcher("_" + it.path);
			while (matcher.find()) {
				iteratorSource.add(matcher.group(1));
			}
			Iterator<String> parts = iteratorSource.iterator();
			PropertyPath result = null;
			Stack<PropertyPath> current = new Stack<>();
			while (parts.hasNext()) {
				if (result == null) {
					result = create(parts.next(), it.type, current);
					current.push(result);
				}
				else {
					current.push(create(parts.next(), current));
				}
			}
			Assert.state(result != null,
					() -> String.format("Expected parsing to yield a PropertyPath from %s but got null!", source));
			return result;
		});
	}

	private static boolean isQuoted(String source) {
		return source.matches("^\\\\Q.*\\\\E$");
	}

	/**
	 * Creates a new {@link PropertyPath} as subordinary of the given
	 * {@link PropertyPath}.
	 * @param source
	 * @param base
	 * @return
	 */
	private static PropertyPath create(String source, Stack<PropertyPath> base) {
		PropertyPath previous = base.peek();
		PropertyPath propertyPath = create(source, previous.typeInformation.getRequiredActualType(), base);
		previous.next = propertyPath;
		return propertyPath;
	}

	/**
	 * Factory method to create a new {@link PropertyPath} for the given {@link String}
	 * and owning type. It will inspect the given source for camel-case parts and traverse
	 * the {@link String} along its parts starting with the entire one and chewing off
	 * parts from the right side then. Whenever a valid property for the given class is
	 * found, the tail will be traversed for subordinary properties of the just found one
	 * and so on.
	 * @param source
	 * @param type
	 * @return
	 */
	private static PropertyPath create(String source, TypeInformation<?> type, List<PropertyPath> base) {
		return create(source, type, "", base);
	}

	/**
	 * Tries to look up a chain of {@link PropertyPath}s by trying the given source first.
	 * If that fails it will split the source apart at camel case borders (starting from
	 * the right side) and try to look up a {@link PropertyPath} from the calculated head
	 * and recombined new tail and additional tail.
	 * @param source
	 * @param type
	 * @param addTail
	 * @return
	 */
	private static PropertyPath create(String source, TypeInformation<?> type, String addTail,
			List<PropertyPath> base) {
		Assert.isTrue(base.size() <= 1000, PARSE_DEPTH_EXCEEDED);
		PropertyReferenceException exception = null;
		PropertyPath current = null;
		try {
			current = new PropertyPath(source, type, base);
			if (!base.isEmpty()) {
				base.get(base.size() - 1).next = current;
			}
			List<PropertyPath> newBase = new ArrayList<>(base);
			newBase.add(current);
			if (StringUtils.hasText(addTail)) {
				current.next = create(addTail, current.actualTypeInformation, newBase);
			}
			return current;

		}
		catch (PropertyReferenceException ex) {
			if (current != null) {
				throw ex;
			}
			exception = ex;
		}
		Pattern pattern = Pattern.compile("\\p{Lu}\\p{Ll}*$");
		Matcher matcher = pattern.matcher(source);
		if (matcher.find() && matcher.start() != 0) {
			int position = matcher.start();
			String head = source.substring(0, position);
			String tail = source.substring(position);
			try {
				return create(head, type, tail + addTail, base);
			}
			catch (PropertyReferenceException ex) {
				throw ex.hasDeeperResolutionDepthThan(exception) ? ex : exception;
			}
		}
		throw exception;
	}

	private static final class Key {

		private final TypeInformation<?> type;

		private final String path;

		private Key(TypeInformation<?> type, String path) {
			this.type = type;
			this.path = path;
		}

		TypeInformation<?> getType() {
			return this.type;
		}

		String getPath() {
			return this.path;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (!(obj instanceof Key)) {
				return false;
			}
			Key other = (Key) obj;
			boolean result = true;
			result = result && ObjectUtils.nullSafeEquals(this.type, other.type);
			result = result && ObjectUtils.nullSafeEquals(this.path, other.path);
			return result;
		}

		@Override
		public int hashCode() {
			int result = ObjectUtils.nullSafeHashCode(this.type);
			result = 31 * result + ObjectUtils.nullSafeHashCode(this.path);
			return result;
		}

		@Override
		public String toString() {
			return "PropertyPath.Key(type=" + this.getType() + ", path=" + this.getPath() + ")";
		}

		static Key of(TypeInformation<?> type, String path) {
			return new Key(type, path);
		}

	}

}
