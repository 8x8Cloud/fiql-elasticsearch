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
import org.apache.cxf.jaxrs.ext.search.collections.CollectionCheckInfo;
import org.apache.cxf.jaxrs.ext.search.visitor.AbstractSearchConditionVisitor;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.index.query.WildcardQueryBuilder;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

// TODO: [greg.feigenson@8x8.com 3/23/17] - Do we need to add aliasing support here? Does that even make sense since we're not relational?
// TODO: [greg.feigenson@8x8.com 3/23/17] - NULL/Bool handling?

/**
 * Provides a FIQL visitor for translating to a basic Elasticsearch query. Given an entity, will attempt to emit a
 * {@link QueryBuilder} that can later be composed via a {@link org.elasticsearch.action.search.SearchRequestBuilder} or
 * other such mechanism. Pagination, sorting, field projections and other functionality will be handled there.
 * <p>
 * <b>WARNING: this class is inherently un-threadsafe by design.</b> This class cannot meaningfully support concurrency.
 * Attempting to cache, or re-use this class is inherently dangerous and must not be done. There is no meaningful re-use
 * possible due to the Apache designs, so please treat this as a prototypical scope, and not as a singleton.
 */
public class ElasticsearchQueryBuilderVisitor<T> extends AbstractSearchConditionVisitor<T, QueryBuilder> {
    private static final String WILDCARD_CHARACTER = "*";
    private final Stack<List<QueryBuilder>> stateStack = new Stack<>();

    Stack<List<QueryBuilder>> getStateStack() {
        return stateStack;
    }

    /**
     * Constructs an instance of an {@link ElasticsearchQueryBuilderVisitor}, using a blank field map.
     * <p>
     * This visitor does not support field aliasing as there is no current use case for it.
     */
    public ElasticsearchQueryBuilderVisitor() {
        // We don't support aliasing for now.
        super(new HashMap<>());
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
            final QueryBuilder builder = buildCompositeExpression(searchCondition.getConditionType(), getStateStack().pop());

            // Slap the currently transformed value back onto the stack so we can re-compose it.
            getStateStack().peek().add(builder);
        }
    }

    @Override
    public QueryBuilder getQuery() {
        return (getStateStack().isEmpty() || getStateStack().peek().isEmpty()) ? null : getStateStack().pop().get(0);
    }

    /**
     * Provides a mechanism to create a leaf-level {@link QueryBuilder} representing a single expression, such as
     * {@code foo==bar}. In the event of a more complex expression with multiple statements, a composite expression will
     * be created consisting of multiple of these leaf-level nodes.
     *
     * @param statement The {@link PrimitiveStatement} representing the leaf-level node.
     * @return A {@link QueryBuilder} representing the expression, if known, else {@code null}.
     * @see #buildCompositeExpression(ConditionType, List)
     */
    QueryBuilder buildSimpleExpression(final PrimitiveStatement statement) {
        // Use the utility methods to unwrap our actual value. This may not just be a flat value, and CXF may need to coalesce it.
        final ClassValue classValue = doGetPrimitiveFieldClass(statement);

        // We don't support stuff like count() operators.
        validateNotCollectionCheck(statement, classValue);

        // It looks like Elasticsearch clients no longer handle enums: https://github.com/elastic/elasticsearch/issues/22867.
        // Let's go ahead and grab the string value if we've been passed an enum.
        final Object value = getEnumSafeValue(classValue);

        // Wildcards will automatically convert the value to a string - hard to do prefix matching on numbers etc.
        final String valueString = value.toString();
        final boolean isWildcard = valueString.contains(WILDCARD_CHARACTER);

        final String property = statement.getProperty();

        switch (statement.getCondition()) {
            // We only support wildcards on (in)equality because the other operators make no sense in Elasticsearch.
            case EQUALS:
                if (isWildcard) {
                    return createWildcardQuery(property, valueString);
                }

                return createTermQuery(property, value);
            case NOT_EQUALS:
                if (isWildcard) {
                    return createBoolQueryBuilder().mustNot(createWildcardQuery(property, valueString));
                }

                return createBoolQueryBuilder().mustNot(createTermQuery(property, value));

            // The rest of our queries are simple non-wildcard ranges. The Elasticsearch client should take care of our conversions
            // for us.
            case LESS_THAN:
                return createRangeQuery(property).lt(value);
            case LESS_OR_EQUALS:
                return createRangeQuery(property).lte(value);
            case GREATER_THAN:
                return createRangeQuery(property).gt(value);
            case GREATER_OR_EQUALS:
                return createRangeQuery(property).gte(value);

            // OR/AND are handled via composite expressions, and we don't know what CUSTOM is.
            default:
                return null;
        }
    }

    /**
     * Provides a mechanism to create a composite expression, consisting of two or more leaf-level expressions that
     * actually compare property values.
     *
     * @param conditionType The {@link ConditionType} representing the composite operation taking place (EG: {@code AND}, @{code OR}).
     * @param parts         The parts of the composite expression to apply the {@link ConditionType} to.
     * @return A composite {@link QueryBuilder} consisting of two or more expressions.
     * @see #buildSimpleExpression(PrimitiveStatement)
     */
    QueryBuilder buildCompositeExpression(final ConditionType conditionType, final List<QueryBuilder> parts) {
        final BoolQueryBuilder boolQueryBuilder = createBoolQueryBuilder();

        for (int partIndex = 0; partIndex < parts.size(); partIndex++) {
            final QueryBuilder part = parts.get(partIndex);
            final QueryBuilder previousPart = partIndex > 0 ? getPreviousQueryPart(boolQueryBuilder, conditionType) : null;

            // Try and fold this into the previous statement part IFF possible.
            if (null != previousPart && canMergeQueryParts(previousPart, part, conditionType)) {
                mergeQueryParts(previousPart, part);
            } else {
                // If no folding is possible, just keep on going...
                if (conditionType.equals(ConditionType.AND)) {
                    boolQueryBuilder.must(part);
                } else {
                    boolQueryBuilder.should(part);
                }
            }
        }

        return boolQueryBuilder;
    }

    /**
     * Provides a convenience method to get either the value of a property, or the string representation if the type of
     * the property is an enum.
     * <p>
     * As of Elasticsearch 5, the Java client no longer automatically converts enums via a call to {@link #toString()}.
     * See <a href="https://github.com/elastic/elasticsearch/issues/22867">https://github.com/elastic/elasticsearch/issues/22867</a>
     * for details.
     *
     * @param classValue The FIQL {@link org.apache.cxf.jaxrs.ext.search.visitor.AbstractSearchConditionVisitor.ClassValue}
     *                   to determine an enum-safe value for.
     * @return The value of the property in question, or the string representation via {@link #toString()} if the property
     * is an enum.
     * @see <a href="https://github.com/elastic/elasticsearch/issues/22867">https://github.com/elastic/elasticsearch/issues/22867</a>
     */
    Object getEnumSafeValue(final ClassValue classValue) {
        return classValue.getCls().isEnum() ? classValue.getValue().toString() : classValue.getValue();
    }

    /**
     * Provides a convenience method to fetch the previous query part from the ongoing {@link BoolQueryBuilder}.
     *
     * @param compoundStatement The {@link BoolQueryBuilder} currently being built into a compound statement.
     * @param conditionType     The {@link ConditionType} of the builder. This tells us where to look for our query part.
     * @return The appropriate {@link QueryBuilder} for the previous query part.
     */
    QueryBuilder getPreviousQueryPart(final BoolQueryBuilder compoundStatement, final ConditionType conditionType) {
        final List<QueryBuilder> builderParts = ConditionType.AND.equals(conditionType) ? compoundStatement.must() : compoundStatement.should();
        return builderParts.get(builderParts.size() - 1);
    }

    /**
     * Provides a convenience method to determine whether or not two query parts can be merged into a single part. This
     * is possible IFF:
     * <p>
     * <pre>
     *     * Neither part is null
     *     * The {@link ConditionType} being applied to both query parts is {@link ConditionType#AND}
     *     * Both query parts are {@link RangeQueryBuilder} instances (EG: no term or wildcard queries)
     *     * Both query parts refer to the same field in Elasticsearch
     *     * Both query parts are within the same expression (IE: within the same level of parenthesis).
     * </pre>
     *
     * @param previousPart  The previous query part to check the merging ability of.
     * @param currentPart   The current query part to check the merging ability of.
     * @param conditionType The {@link ConditionType} being applied to both query parts.
     * @return {@code True} if the current query part can be merged into the previous query part, else {@code false}.
     * @see #mergeQueryParts(QueryBuilder, QueryBuilder)
     */
    boolean canMergeQueryParts(final QueryBuilder previousPart, final QueryBuilder currentPart, final ConditionType conditionType) {
        // We can only merge together AND'ed range queries.
        if (ConditionType.AND.equals(conditionType) &&
                RangeQueryBuilder.class.isAssignableFrom(previousPart.getClass()) &&
                RangeQueryBuilder.class.isAssignableFrom(currentPart.getClass())) {
            final RangeQueryBuilder previousRangeQuery = (RangeQueryBuilder) previousPart;
            final RangeQueryBuilder currentRangeQuery = (RangeQueryBuilder) currentPart;

            // It also helps if both queries apply to the same field.
            if (previousRangeQuery.fieldName().equals(currentRangeQuery.fieldName())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Provides a convenience method to merge two query parts together. Assuming that the value of
     * {@link #canMergeQueryParts(QueryBuilder, QueryBuilder, ConditionType)} is {@code true}, this method will attempt
     * to take the values out of the current query part and put them into the appropriate fields in the previous query part.
     *
     * @param previousPart The previous query part to merge into.
     * @param part         The current query part to merge into the previous.
     * @see #canMergeQueryParts(QueryBuilder, QueryBuilder, ConditionType)
     */
    void mergeQueryParts(final QueryBuilder previousPart, final QueryBuilder part) {
        final RangeQueryBuilder previousRangeQuery = (RangeQueryBuilder) previousPart;
        final RangeQueryBuilder currentRangeQuery = (RangeQueryBuilder) part;

        // If the "from" field is filled in, we know we're greater than/greater than or equal to.
        if (null == currentRangeQuery.to()) {
            previousRangeQuery.from(currentRangeQuery.from());
            previousRangeQuery.includeLower(currentRangeQuery.includeLower());
        } else {
            // Otherwise we're less than/less than or equal to.
            previousRangeQuery.to(currentRangeQuery.to());
            previousRangeQuery.includeUpper(currentRangeQuery.includeUpper());
        }
    }

    /**
     * Provides a convenience method to throw a validation exception if the expression passed uses a forbidden collection
     * operator, such as {@code count()}. Elasticsearch doesn't really support this type of stuff.
     *
     * @param statement  The {@link PrimitiveStatement} being evaluated.
     * @param classValue The {@link ClassValue} derived from the JAX-RS search framework for the given expression.
     */
    void validateNotCollectionCheck(final PrimitiveStatement statement, final ClassValue classValue) {
        final CollectionCheckInfo collectionCheckInfo = classValue.getCollectionCheckInfo();

        // The spec only seems to have a single operator (count), and we don't support it.
        if (null != collectionCheckInfo) {
            throw new IllegalArgumentException(String.format("Query contains an illegal operation: %s %s %s %s",
                    statement.getProperty(), statement.getCondition(), collectionCheckInfo.getCollectionCheckType(), collectionCheckInfo.getCollectionCheckValue()));
        }
    }

    /**
     * Provides a test-friendly method for creating a {@link BoolQueryBuilder} for representing boolean queries.
     *
     * @return A {@link BoolQueryBuilder}, for all your bool query building needs.
     */
    BoolQueryBuilder createBoolQueryBuilder() {
        return new BoolQueryBuilder();
    }

    /**
     * Provides a test-friendly method for creating a {@link RangeQueryBuilder} for representing range-based queries.
     *
     * @param propertyName The name of the property to set upon the range query.
     * @return A {@link RangeQueryBuilder} to be used to represent part of a range.
     */
    RangeQueryBuilder createRangeQuery(final String propertyName) {
        return new RangeQueryBuilder(propertyName);
    }

    /**
     * Provides a test-friendly method for creating a {@link TermQueryBuilder} for representing a term query.
     * <p>
     * Please note that term queries might behave differently depending on your mappings.
     *
     * @param propertyName The name of the property to match via the term query.
     * @param value        The value of the term to match.
     * @return A {@link TermQueryBuilder} for querying by a given term.
     */
    TermQueryBuilder createTermQuery(final String propertyName, final Object value) {
        return new TermQueryBuilder(propertyName, value);
    }

    /**
     * Provides a test-friendly method for creating a {@link WildcardQueryBuilder} for building a string-based prefix
     * wildcard query.
     *
     * @param propertyName The name of the property to match via the wildcard query.
     * @param value        The value of the prefix match.
     * @return A {@link WildcardQueryBuilder} for querying via a prefix match.
     */
    WildcardQueryBuilder createWildcardQuery(final String propertyName, final String value) {
        return new WildcardQueryBuilder(propertyName, value);
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
