/*
 * Copyright 2017 8x8, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com._8x8.cloud.platform.jaxrs.ext.search.elasticsearch;

import org.apache.cxf.jaxrs.ext.search.ConditionType;
import org.apache.cxf.jaxrs.ext.search.PrimitiveStatement;
import org.apache.cxf.jaxrs.ext.search.SearchCondition;
import org.apache.cxf.jaxrs.ext.search.fiql.FiqlParser;
import org.apache.cxf.jaxrs.ext.search.visitor.AbstractSearchConditionVisitor;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import static org.apache.cxf.jaxrs.ext.search.ConditionType.AND;
import static org.apache.cxf.jaxrs.ext.search.fiql.FiqlParser.CONDITION_MAP;

/**
 * Provides a visitor that can be used to produce an output that is also a FIQL string. This class is particularly handy
 * when you need to pass around FIQL filters around a more complex system, where each piece of which may have its own models.
 * <p>
 * In addition to spitting out FIQL, this visitor also has another neat trick: it can make use of
 * {@link FiqlTransformationFunction} at a field-level granularity. This means that you can override the specific output
 * of a given property with a callback. Such behavior comes in handy when you need to full control over how a specific
 * field is transformed, such as when using collections in Elasticsearch. Arrays, sets, and other collections implicitly
 * look like standard mappings, but are not - though, they may be mapped to fields on your models.
 */
public class TranslatingQueryBuilderVisitor<T> extends AbstractSearchConditionVisitor<T, String> {
    /**
     * Holds the default date format string to use on {@link Date} fields if none is specified via the constructor.
     */
    @SuppressWarnings("WeakerAccess")
    public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";

    private static final String OPEN_PAREN = "(";
    private static final String CLOSE_PAREN = ")";
    private static final String EMPTY_STRING = "";

    private final Stack<List<String>> stateStack = new Stack<>();
    private final SimpleDateFormat simpleDateFormat;
    private final Map<String, FiqlTransformationFunction> transformationFunctions;
    private final static FiqlTransformationFunction defaultHandler = (property, operation, value) -> String.format("%s%s%s", property, operation, value);

    Stack<List<String>> getStateStack() {
        return stateStack;
    }

    SimpleDateFormat getSimpleDateFormat() {
        return simpleDateFormat;
    }

    Map<String, FiqlTransformationFunction> getTransformationFunctions() {
        return transformationFunctions;
    }

    FiqlTransformationFunction getDefaultHandler() {
        return defaultHandler;
    }

    /**
     * Constructs a translating visitor.
     * <p>
     * Please note that due to the way {@link FiqlParser}s handle dates, you'll need to make sure that the date format
     * being used is of the same granularity as the parser or you'll lose precision. This can be achieved via setting
     * the {@code search.date-format} property on the parser. By default, FIQL chops anything at the minute, and second
     * level of precision. See <a href="http://cxf.apache.org/docs/jax-rs-search.html#JAX-RSSearch-Usingdatesinqueries">
     * the CXF documentation</a> for details.
     *
     * @param fieldMap                A mapping of field values to aliases. Property names will be translated from the key, to the
     *                                value, as with other FIQL visitors.
     * @param dateFormat              An optional string representing a {@link java.text.DateFormat}. This will be used when the FIQL
     *                                expression contains a date.
     * @param transformationFunctions An optional mapping of property names to {@link FiqlTransformationFunction}. This
     *                                allows for a more fine-grained approach to transforming FIQL queries.
     * @see <a href="http://cxf.apache.org/docs/jax-rs-search.html#JAX-RSSearch-Usingdatesinqueries">the CXF documentation.</a>
     */
    public TranslatingQueryBuilderVisitor(final Map<String, String> fieldMap, final String dateFormat,
                                          final Map<String, FiqlTransformationFunction> transformationFunctions) {
        // Honor any field map overrides.
        super(null == fieldMap ? new HashMap<>() : fieldMap);

        // If they've given us a date output formatter, use it. Otherwise, use a granular date/time for the standard FIQL output.
        simpleDateFormat = new SimpleDateFormat((null != dateFormat) ? dateFormat : DEFAULT_DATE_FORMAT);

        // Likewise if we've been given any custom transformation functions.
        this.transformationFunctions = (null == transformationFunctions) ? new HashMap<>() : transformationFunctions;

        // Start that stack off right.
        getStateStack().push(new ArrayList<>());
    }

    @Override
    public void visit(final SearchCondition<T> searchCondition) {
        final PrimitiveStatement statement = searchCondition.getStatement();

        // If we've got a primitive statement, we're at a leaf expression like foo==bar.
        if (null != statement) {
            if (statement.getProperty() != null) {
                getStateStack().peek().add(buildSimpleExpression(statement));
            }
        } else {
            // Otherwise we're within a composite structure like (foo==bar OR baz==quux)
            getStateStack().push(new ArrayList<>());

            // Recurse down our tree till we hit our leaves, building our state as we go.
            searchCondition.getSearchConditions().forEach(x -> x.accept(this));

            // Unwind the stack and build our current level of glue.
            final String builder = buildCompositeExpression(searchCondition.getConditionType(), getStateStack().pop());

            // Slap the currently transformed value back onto the stack so we can re-compose it.
            getStateStack().peek().add(builder);
        }
    }

    @Override
    public String getQuery() {
        return getStateStack().isEmpty() || getStateStack().peek().isEmpty() ? EMPTY_STRING : getStateStack().pop().get(0);
    }

    /**
     * Provides a mechanism to create a leaf-level string representing a single expression, such as
     * {@code foo==bar}. In the event of a more complex expression with multiple statements, a composite expression will
     * be created consisting of multiple of these leaf-level nodes.
     *
     * @param statement The {@link PrimitiveStatement} representing the leaf-level node.
     * @return A string representing the expression, if known, else an empty string.
     * @see #buildCompositeExpression(ConditionType, List)
     */
    String buildSimpleExpression(final PrimitiveStatement statement) {
        final String propertyName = getReallyRealPropertyName(statement.getProperty());
        final String operation = CONDITION_MAP.get(statement.getCondition());

        return (null == operation) ? EMPTY_STRING : getTransformationFunctions()
                .getOrDefault(propertyName, getDefaultHandler())
                .apply(propertyName, operation, getDateSafeValue(doGetPrimitiveFieldClass(statement)));
    }

    /**
     * Provides a mechanism to create a composite expression, consisting of two or more leaf-level expressions that
     * actually compare property values.
     *
     * @param conditionType The {@link ConditionType} representing the composite operation taking place (EG: {@code AND}, @{code OR}).
     * @param parts         The parts of the composite expression to apply the {@link ConditionType} to.
     * @return A FIQL string consisting of two or more expressions.
     * @see #buildSimpleExpression(PrimitiveStatement)
     */
    String buildCompositeExpression(final ConditionType conditionType, final List<String> parts) {
        final StringBuilder builder = new StringBuilder();

        // If we're a compound statement, add the opening parenthesis.
        if (requiresParenthesisWrapping()) {
            builder.append(OPEN_PAREN);
        }

        // Glue together all our parts appropriately.
        for (int partIndex = 0; partIndex < parts.size(); partIndex++) {
            final String part = parts.get(partIndex);
            builder.append(part);

            // If there are more parts in our composite, let's glue them together.
            if (partIndex < parts.size() - 1) {
                builder.append(conditionType.equals(AND) ? FiqlParser.AND : FiqlParser.OR);
            }
        }

        // Likewise, if we're a compound statement we also need a closing parenthesis.
        if (requiresParenthesisWrapping()) {
            builder.append(CLOSE_PAREN);
        }

        return builder.toString();
    }

    /**
     * Provides a convenience method to determine whether or not an expression, or set of expressions, requires wrapping
     * with parenthesis.
     *
     * @return {@code True} if the statement requires wrapping, else {@code false}. This is generally {@code true} if the
     * state stack has a depth greater than 1, IE: there's more than one level of expression being {@code AND}'ed or
     * {@code OR}'ed together.
     */
    boolean requiresParenthesisWrapping() {
        return getStateStack().size() > 1;
    }

    /**
     * Provides a test-friendly wrapper around the protected {@link #getRealPropertyName(String)} method.
     *
     * @param property The name of the property to really retrieve the name for. For real.
     * @return The field-mapped name of the property, if known, else the name of the property as passed in.
     */
    String getReallyRealPropertyName(final String property) {
        return super.getRealPropertyName(property);
    }

    /**
     * Date handling in FIQL is a little funky. So, let's take a date and turn it back into a particular date/time format.
     *
     * @param classValue The set of metadata regarding the type described in the given class value.
     * @return A date-safe string that can be re-used for constructing a FIQL statement.
     */
    String getDateSafeValue(final ClassValue classValue) {
        return Date.class.isAssignableFrom(classValue.getCls()) ?
                getSimpleDateFormat().format(classValue.getValue()) : classValue.getValue().toString();
    }

    /**
     * Provides a test-friendly way of dealing with
     * {@link #getPrimitiveFieldClass(PrimitiveStatement, String, Class, Type, Object)} as most things to do with the
     * {@link org.apache.cxf.jaxrs.ext.search.visitor.AbstractSearchConditionVisitor.ClassValue} are protected.
     *
     * @param statement The {@link PrimitiveStatement} to get the value of.
     * @return A set of metadata regarding the type described in the given {@link PrimitiveStatement}.
     */
    ClassValue doGetPrimitiveFieldClass(final PrimitiveStatement statement) {
        return getPrimitiveFieldClass(statement, statement.getProperty(),
                statement.getValue().getClass(), statement.getValueType(), statement.getValue());
    }
}