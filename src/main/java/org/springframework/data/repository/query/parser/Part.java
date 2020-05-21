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

package org.springframework.data.repository.query.parser;

import java.beans.Introspector;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.data.mapping.PropertyPath;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * A single part of a method name that has to be transformed into a query part. The actual
 * transformation is defined by a {@link Type} that is determined from inspecting the
 * given part. The query part can then be looked up via {@link #getProperty()}.
 *
 * @author Oliver Gierke
 * @author Martin Baumgartner
 * @author Jens Schauder
 * @author Thomas Darimont
 * @author Michael Cramer
 * @author Phillip Webb
 */
public class Part {

	private static final Pattern IGNORE_CASE = Pattern.compile("Ignor(ing|e)Case");

	private final PropertyPath propertyPath;

	private final Part.Type type;

	private IgnoreCaseType ignoreCase = IgnoreCaseType.NEVER;

	/**
	 * Creates a new {@link Part} from the given method name part, the {@link Class} the
	 * part originates from and the start parameter index.
	 * @param source must not be {@literal null}.
	 * @param clazz must not be {@literal null}.
	 */
	public Part(String source, Class<?> clazz) {
		this(source, clazz, false);
	}

	/**
	 * Creates a new {@link Part} from the given method name part, the {@link Class} the
	 * part originates from and the start parameter index.
	 * @param source must not be {@literal null}.
	 * @param clazz must not be {@literal null}.
	 * @param alwaysIgnoreCase
	 */
	public Part(String source, Class<?> clazz, boolean alwaysIgnoreCase) {
		Assert.hasText(source, "Part source must not be null or empty!");
		Assert.notNull(clazz, "Type must not be null!");
		String partToUse = detectAndSetIgnoreCase(source);
		if (alwaysIgnoreCase && this.ignoreCase != IgnoreCaseType.ALWAYS) {
			this.ignoreCase = IgnoreCaseType.WHEN_POSSIBLE;
		}
		this.type = Type.fromProperty(partToUse);
		this.propertyPath = PropertyPath.from(this.type.extractProperty(partToUse), clazz);
	}

	private String detectAndSetIgnoreCase(String part) {
		Matcher matcher = IGNORE_CASE.matcher(part);
		String result = part;
		if (matcher.find()) {
			this.ignoreCase = IgnoreCaseType.ALWAYS;
			result = part.substring(0, matcher.start()) + part.substring(matcher.end(), part.length());
		}
		return result;
	}

	boolean isParameterRequired() {
		return getNumberOfArguments() > 0;
	}

	/**
	 * Returns how many method parameters are bound by this part.
	 * @return
	 */
	public int getNumberOfArguments() {
		return this.type.getNumberOfArguments();
	}

	/**
	 * @return the propertyPath
	 */
	public PropertyPath getProperty() {
		return this.propertyPath;
	}

	/**
	 * @return the type
	 */
	public Part.Type getType() {
		return this.type;
	}

	/**
	 * Returns whether the {@link PropertyPath} referenced should be matched ignoring
	 * case.
	 * @return
	 */
	public IgnoreCaseType shouldIgnoreCase() {
		return this.ignoreCase;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof Part)) {
			return false;
		}
		Part part = (Part) o;
		if (!ObjectUtils.nullSafeEquals(this.propertyPath, part.propertyPath)) {
			return false;
		}
		if (this.type != part.type) {
			return false;
		}
		return this.ignoreCase == part.ignoreCase;
	}

	@Override
	public int hashCode() {
		int result = ObjectUtils.nullSafeHashCode(this.propertyPath);
		result = 31 * result + ObjectUtils.nullSafeHashCode(this.type);
		result = 31 * result + ObjectUtils.nullSafeHashCode(this.ignoreCase);
		return result;
	}

	@Override
	public String toString() {
		return String.format("%s %s %s", this.propertyPath.getSegment(), this.type, this.ignoreCase);
	}

	/**
	 * The type of a method name part. Used to create query parts in various ways.
	 */
	public enum Type {

		/**
		 * 'Between' method part name type.
		 */
		BETWEEN(2, "IsBetween", "Between"),

		/**
		 * 'Is Not Null' method part name type.
		 */
		IS_NOT_NULL(0, "IsNotNull", "NotNull"),

		/**
		 * 'Is Null' method part name type.
		 */
		IS_NULL(0, "IsNull", "Null"),

		/**
		 * 'Less Than' method part name type.
		 */
		LESS_THAN("IsLessThan", "LessThan"),

		/**
		 * 'Less Than Equal' method part name type.
		 */
		LESS_THAN_EQUAL("IsLessThanEqual", "LessThanEqual"),

		/**
		 * 'Greater Than' method part name type.
		 */
		GREATER_THAN("IsGreaterThan", "GreaterThan"),

		/**
		 * 'Greater Than Equal' method part name type.
		 */
		GREATER_THAN_EQUAL("IsGreaterThanEqual", "GreaterThanEqual"),

		/**
		 * 'Before' method part name type.
		 */
		BEFORE("IsBefore", "Before"),

		/**
		 * 'After' method part name type.
		 */
		AFTER("IsAfter", "After"),

		/**
		 * 'Not Like' method part name type.
		 */
		NOT_LIKE("IsNotLike", "NotLike"),

		/**
		 * 'Like' method part name type.
		 */
		LIKE("IsLike", "Like"),

		/**
		 * 'Starting With' method part name type.
		 */
		STARTING_WITH("IsStartingWith", "StartingWith", "StartsWith"),

		/**
		 * 'Ending With' method part name type.
		 */
		ENDING_WITH("IsEndingWith", "EndingWith", "EndsWith"),

		/**
		 * 'Is Not Empty' method part name type.
		 */
		IS_NOT_EMPTY(0, "IsNotEmpty", "NotEmpty"),

		/**
		 * 'Is Empty' method part name type.
		 */
		IS_EMPTY(0, "IsEmpty", "Empty"),

		/**
		 * 'Not Containing' method part name type.
		 */
		NOT_CONTAINING("IsNotContaining", "NotContaining", "NotContains"),

		/**
		 * 'Containing' method part name type.
		 */
		CONTAINING("IsContaining", "Containing", "Contains"),

		/**
		 * 'Not In' method part name type.
		 */
		NOT_IN("IsNotIn", "NotIn"),

		/**
		 * 'In' method part name type.
		 */
		IN("IsIn", "In"),

		/**
		 * 'Near' method part name type.
		 */
		NEAR("IsNear", "Near"),

		/**
		 * 'Within' method part name type.
		 */
		WITHIN("IsWithin", "Within"),

		/**
		 * 'Refex' method part name type.
		 */
		REGEX("MatchesRegex", "Matches", "Regex"),

		/**
		 * 'Exists' method part name type.
		 */
		EXISTS(0, "Exists"),

		/**
		 * 'True' method part name type.
		 */
		TRUE(0, "IsTrue", "True"),

		/**
		 * 'False' method part name type.
		 */
		FALSE(0, "IsFalse", "False"),

		/**
		 * 'Negating Simple Property' method part name type.
		 */
		NEGATING_SIMPLE_PROPERTY("IsNot", "Not"),

		/**
		 * 'Simple Property' method part name type.
		 */
		SIMPLE_PROPERTY("Is", "Equals");

		// Need to list them again explicitly as the order is important
		// (esp. for IS_NULL, IS_NOT_NULL)
		private static final List<Part.Type> ALL = Arrays.asList(IS_NOT_NULL, IS_NULL, BETWEEN, LESS_THAN,
				LESS_THAN_EQUAL, GREATER_THAN, GREATER_THAN_EQUAL, BEFORE, AFTER, NOT_LIKE, LIKE, STARTING_WITH,
				ENDING_WITH, IS_NOT_EMPTY, IS_EMPTY, NOT_CONTAINING, CONTAINING, NOT_IN, IN, NEAR, WITHIN, REGEX,
				EXISTS, TRUE, FALSE, NEGATING_SIMPLE_PROPERTY, SIMPLE_PROPERTY);

		public static final Collection<String> ALL_KEYWORDS;

		static {
			List<String> allKeywords = new ArrayList<>();
			for (Type type : ALL) {
				allKeywords.addAll(type.keywords);
			}
			ALL_KEYWORDS = Collections.unmodifiableList(allKeywords);
		}

		private final List<String> keywords;

		private final int numberOfArguments;

		/**
		 * Creates a new {@link Type} using the given keyword, number of arguments to be
		 * bound and operator. Keyword and operator can be {@literal null}.
		 * @param numberOfArguments
		 * @param keywords
		 */
		Type(int numberOfArguments, String... keywords) {
			this.numberOfArguments = numberOfArguments;
			this.keywords = Arrays.asList(keywords);
		}

		Type(String... keywords) {
			this(1, keywords);
		}

		/**
		 * Returns all keywords supported by the current {@link Type}.
		 * @return
		 */
		public Collection<String> getKeywords() {
			return Collections.unmodifiableList(this.keywords);
		}

		/**
		 * Returns whether the the type supports the given raw property. Default
		 * implementation checks whether the property ends with the registered keyword.
		 * Does not support the keyword if the property is a valid field as is.
		 * @param property
		 * @return
		 */
		protected boolean supports(String property) {
			for (String keyword : this.keywords) {
				if (property.endsWith(keyword)) {
					return true;
				}
			}
			return false;
		}

		/**
		 * Returns the number of arguments the propertyPath binds. By default this exactly
		 * one argument.
		 * @return
		 */
		public int getNumberOfArguments() {
			return this.numberOfArguments;
		}

		/**
		 * Callback method to extract the actual propertyPath to be bound from the given
		 * part. Strips the keyword from the part's end if available.
		 * @param part
		 * @return
		 */
		public String extractProperty(String part) {
			String candidate = Introspector.decapitalize(part);
			for (String keyword : this.keywords) {
				if (candidate.endsWith(keyword)) {
					return candidate.substring(0, candidate.length() - keyword.length());
				}
			}
			return candidate;
		}

		@Override
		public String toString() {
			return String.format("%s (%s): %s", name(), getNumberOfArguments(), getKeywords());
		}

		/**
		 * Returns the {@link Type} of the {@link Part} for the given raw propertyPath.
		 * This will try to detect e.g. keywords contained in the raw propertyPath that
		 * trigger special query creation. Returns {@link #SIMPLE_PROPERTY} by default.
		 * @param rawProperty
		 * @return
		 */
		public static Part.Type fromProperty(String rawProperty) {
			for (Part.Type type : ALL) {
				if (type.supports(rawProperty)) {
					return type;
				}
			}
			return SIMPLE_PROPERTY;
		}

	}

	/**
	 * The various types of ignore case that are supported.
	 */
	public enum IgnoreCaseType {

		/**
		 * Should not ignore the sentence case.
		 */
		NEVER,

		/**
		 * Should ignore the sentence case, throwing an exception if this is not possible.
		 */
		ALWAYS,

		/**
		 * Should ignore the sentence case when possible to do so, silently ignoring the
		 * option when not possible.
		 */
		WHEN_POSSIBLE

	}

}
