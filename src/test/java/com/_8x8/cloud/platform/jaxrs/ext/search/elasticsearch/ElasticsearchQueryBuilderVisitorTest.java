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

import com._8x8.cloud.platform.jaxrs.ext.search.elasticsearch.model.MetadataRecord;
import com._8x8.cloud.platform.jaxrs.ext.search.elasticsearch.model.Status;
import org.apache.cxf.jaxrs.ext.search.ConditionType;
import org.apache.cxf.jaxrs.ext.search.PrimitiveStatement;
import org.apache.cxf.jaxrs.ext.search.SearchCondition;
import org.apache.cxf.jaxrs.ext.search.collections.CollectionCheck;
import org.apache.cxf.jaxrs.ext.search.collections.CollectionCheckInfo;
import org.apache.cxf.jaxrs.ext.search.visitor.AbstractSearchConditionVisitor;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.index.query.WildcardQueryBuilder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Stack;
import java.util.stream.Stream;

import static org.apache.cxf.jaxrs.ext.search.ConditionType.AND;
import static org.apache.cxf.jaxrs.ext.search.ConditionType.CUSTOM;
import static org.apache.cxf.jaxrs.ext.search.ConditionType.EQUALS;
import static org.apache.cxf.jaxrs.ext.search.ConditionType.GREATER_OR_EQUALS;
import static org.apache.cxf.jaxrs.ext.search.ConditionType.GREATER_THAN;
import static org.apache.cxf.jaxrs.ext.search.ConditionType.LESS_OR_EQUALS;
import static org.apache.cxf.jaxrs.ext.search.ConditionType.LESS_THAN;
import static org.apache.cxf.jaxrs.ext.search.ConditionType.NOT_EQUALS;
import static org.apache.cxf.jaxrs.ext.search.ConditionType.OR;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Tests the {@link ElasticsearchQueryBuilderVisitor} at the unit level.
 */
public class ElasticsearchQueryBuilderVisitorTest {
    private final ElasticsearchQueryBuilderVisitor<MetadataRecord> visitor = spy(new ElasticsearchQueryBuilderVisitor<>());
    private final PrimitiveStatement statement = mock(PrimitiveStatement.class);
    private final TestSearchConditionVisitor.ClassValue classValue = mock(TestSearchConditionVisitor.ClassValue.class);
    private final WildcardQueryBuilder wildcardQueryBuilder = mock(WildcardQueryBuilder.class);
    private final BoolQueryBuilder boolQueryBuilder = mock(BoolQueryBuilder.class);
    private final TermQueryBuilder termQueryBuilder = mock(TermQueryBuilder.class);
    private final RangeQueryBuilder rangeQueryBuilder = mock(RangeQueryBuilder.class);

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        doReturn(classValue).when(visitor).doGetPrimitiveFieldClass(any(PrimitiveStatement.class));

        doReturn(wildcardQueryBuilder).when(visitor).createWildcardQuery(anyString(), anyString());
        doReturn(boolQueryBuilder).when(visitor).createBoolQueryBuilder();
        doReturn(termQueryBuilder).when(visitor).createTermQuery(anyString(), any());
        doReturn(rangeQueryBuilder).when(visitor).createRangeQuery(anyString());

        doReturn(boolQueryBuilder).when(boolQueryBuilder).mustNot(any(QueryBuilder.class));
        doReturn(rangeQueryBuilder).when(rangeQueryBuilder).lt(any(RangeQueryBuilder.class));
        doReturn(rangeQueryBuilder).when(rangeQueryBuilder).lte(any(RangeQueryBuilder.class));
        doReturn(rangeQueryBuilder).when(rangeQueryBuilder).gt(any(RangeQueryBuilder.class));
        doReturn(rangeQueryBuilder).when(rangeQueryBuilder).gte(any(RangeQueryBuilder.class));

        // We'll test our enum-safe getter separately.
        doAnswer(answer -> answer.getArgumentAt(0, TestSearchConditionVisitor.ClassValue.class).getValue())
                .when(visitor).getEnumSafeValue(classValue);
    }

    /**
     * Tests {@link ElasticsearchQueryBuilderVisitor#visit(SearchCondition)} for a primitive statement (IE: foo==bar).
     */
    @SuppressWarnings({"unchecked", "ResultOfMethodCallIgnored"})
    @Test
    public void testVisitForPrimitiveStatement() throws Exception {
        final SearchCondition<MetadataRecord> searchCondition = mock(SearchCondition.class);
        final QueryBuilder builder = mock(QueryBuilder.class);

        doReturn(statement).when(searchCondition).getStatement();
        doReturn(builder).when(visitor).buildSimpleExpression(statement);
        doReturn("potato").when(statement).getProperty();

        visitor.visit(searchCondition);

        verify(visitor).visit(searchCondition);
        verify(visitor).getStateStack();
        verify(visitor).buildSimpleExpression(statement);

        verify(searchCondition).getStatement();

        verify(statement).getProperty();

        verifyNoMoreCollaboration(searchCondition, builder);

        // We should get back our builder too.
        assertThat(visitor.getStateStack().size(), is(1));
        assertThat(visitor.getStateStack().peek(), is(Collections.singletonList(builder)));
    }

    /**
     * Tests {@link ElasticsearchQueryBuilderVisitor#visit(SearchCondition)} for a primitive statement (IE: foo==bar),
     * but in this case the statement property is null. Not entirely sure how this happens...
     */
    @SuppressWarnings({"unchecked", "ResultOfMethodCallIgnored"})
    @Test
    public void testVisitForPrimitiveStatementWithNullProperty() throws Exception {
        final SearchCondition<MetadataRecord> searchCondition = mock(SearchCondition.class);
        final QueryBuilder builder = mock(QueryBuilder.class);

        doReturn(statement).when(searchCondition).getStatement();
        doReturn(builder).when(visitor).buildSimpleExpression(statement);

        visitor.visit(searchCondition);

        verify(visitor).visit(searchCondition);

        verify(searchCondition).getStatement();

        verify(statement).getProperty();

        verifyNoMoreCollaboration(searchCondition, builder);

        // We should get back our builder too.
        assertThat(visitor.getStateStack().size(), is(1));
        assertThat(visitor.getStateStack().peek().isEmpty(), is(true));
    }

    /**
     * Tests {@link ElasticsearchQueryBuilderVisitor#visit(SearchCondition)} for a compound statement, such as something
     * with an AND or OR in it.
     */
    @SuppressWarnings({"unchecked", "ResultOfMethodCallIgnored"})
    @Test
    public void testVisitForComplexStatement() throws Exception {
        final List<SearchCondition> searchConditions = new ArrayList<>();
        final SearchCondition<MetadataRecord> nestedCondition = mock(SearchCondition.class);
        searchConditions.add(nestedCondition);

        final SearchCondition<MetadataRecord> searchCondition = mock(SearchCondition.class);
        final QueryBuilder builder = mock(QueryBuilder.class);

        doReturn(searchConditions).when(searchCondition).getSearchConditions();
        doReturn(builder).when(visitor).buildCompositeExpression(any(ConditionType.class), anyListOf(QueryBuilder.class));

        visitor.visit(searchCondition);

        verify(visitor).visit(searchCondition);
        verify(visitor, times(3)).getStateStack();
        verify(visitor).buildCompositeExpression(any(ConditionType.class), anyListOf(QueryBuilder.class));

        verify(searchCondition).getStatement();
        verify(searchCondition).getSearchConditions();
        verify(searchCondition).getConditionType();

        verify(searchCondition).getStatement();

        verify(nestedCondition).accept(visitor);

        verifyNoMoreCollaboration(searchCondition, builder, nestedCondition);

        // We should get back our builder too.
        assertThat(visitor.getStateStack().size(), is(1));
        assertThat(visitor.getStateStack().peek(), is(Collections.singletonList(builder)));
    }

    /**
     * Tests {@link ElasticsearchQueryBuilderVisitor#getQuery()} to make sure it does what we expect.
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    public void testGetQuery() throws Exception {
        // If our stack is empty, our query is null.
        doReturn(new Stack<List<QueryBuilder>>()).when(visitor).getStateStack();
        assertThat(visitor.getQuery(), is(nullValue()));

        // Likewise, if our stack contains an empty list of state.
        final List<QueryBuilder> queryBuilders = new ArrayList<>();
        final Stack<List<QueryBuilder>> stateStack = new Stack<>();
        stateStack.push(queryBuilders);

        doReturn(stateStack).when(visitor).getStateStack();
        assertThat(visitor.getQuery(), is(nullValue()));

        // But if we've got a builder, we should get it back.
        final QueryBuilder queryBuilder = mock(QueryBuilder.class);
        queryBuilders.add(queryBuilder);

        assertThat(visitor.getQuery(), is(queryBuilder));
    }

    /**
     * Tests {@link ElasticsearchQueryBuilderVisitor#buildSimpleExpression(PrimitiveStatement)} for the case where
     * we're building a {@link ConditionType#EQUALS} expression.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testBuildSimpleEqualsExpression() throws Exception {
        final Object value = "hello";
        final String property = "property";

        doReturn(value).when(classValue).getValue();
        doReturn(EQUALS).when(statement).getCondition();
        doReturn(property).when(statement).getProperty();

        assertThat(visitor.buildSimpleExpression(statement), instanceOf(TermQueryBuilder.class));

        verify(visitor).buildSimpleExpression(statement);
        verify(visitor).doGetPrimitiveFieldClass(statement);
        verify(visitor).validateNotCollectionCheck(statement, classValue);
        verify(visitor).createTermQuery(property, value);
        verify(visitor).getEnumSafeValue(classValue);

        verify(statement).getProperty();
        verify(statement).getCondition();

        verifyNoMoreCollaboration();
    }

    /**
     * Tests {@link ElasticsearchQueryBuilderVisitor#buildSimpleExpression(PrimitiveStatement)} for the case where
     * we're building a {@link ConditionType#EQUALS} expression with a prefix wildcard.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testBuildSimpleEqualsWildcardExpression() throws Exception {
        final Object value = "hello*";
        final String property = "property";

        doReturn(value).when(classValue).getValue();
        doReturn(EQUALS).when(statement).getCondition();
        doReturn(property).when(statement).getProperty();

        assertThat(visitor.buildSimpleExpression(statement), instanceOf(WildcardQueryBuilder.class));

        verify(visitor).buildSimpleExpression(statement);
        verify(visitor).doGetPrimitiveFieldClass(statement);
        verify(visitor).validateNotCollectionCheck(statement, classValue);
        verify(visitor).createWildcardQuery(property, value.toString());
        verify(visitor).getEnumSafeValue(classValue);

        verify(statement).getProperty();
        verify(statement).getCondition();

        verifyNoMoreCollaboration();
    }

    /**
     * Tests {@link ElasticsearchQueryBuilderVisitor#buildSimpleExpression(PrimitiveStatement)} for the case where
     * we're building a {@link ConditionType#NOT_EQUALS} expression.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testBuildSimpleInequalityExpression() throws Exception {
        final Object value = "hello";
        final String property = "property";

        doReturn(value).when(classValue).getValue();
        doReturn(NOT_EQUALS).when(statement).getCondition();
        doReturn(property).when(statement).getProperty();

        assertThat(visitor.buildSimpleExpression(statement), instanceOf(BoolQueryBuilder.class));

        verify(visitor).buildSimpleExpression(statement);
        verify(visitor).doGetPrimitiveFieldClass(statement);
        verify(visitor).validateNotCollectionCheck(statement, classValue);
        verify(visitor).createBoolQueryBuilder();
        verify(visitor).createTermQuery(property, value);
        verify(visitor).getEnumSafeValue(classValue);

        verify(statement).getProperty();
        verify(statement).getCondition();

        verify(boolQueryBuilder).mustNot(termQueryBuilder);

        verifyNoMoreCollaboration();
    }

    /**
     * Tests {@link ElasticsearchQueryBuilderVisitor#buildSimpleExpression(PrimitiveStatement)} for the case where
     * we're building a {@link ConditionType#NOT_EQUALS} expression with a prefix wildcard.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testBuildSimpleInequalityWildcardExpression() throws Exception {
        final Object value = "hello*";
        final String property = "property";

        doReturn(value).when(classValue).getValue();
        doReturn(NOT_EQUALS).when(statement).getCondition();
        doReturn(property).when(statement).getProperty();

        assertThat(visitor.buildSimpleExpression(statement), instanceOf(BoolQueryBuilder.class));

        verify(visitor).buildSimpleExpression(statement);
        verify(visitor).doGetPrimitiveFieldClass(statement);
        verify(visitor).validateNotCollectionCheck(statement, classValue);
        verify(visitor).createBoolQueryBuilder();
        verify(visitor).createWildcardQuery(property, value.toString());
        verify(visitor).getEnumSafeValue(classValue);

        verify(statement).getProperty();
        verify(statement).getCondition();

        verify(boolQueryBuilder).mustNot(any(WildcardQueryBuilder.class));

        verifyNoMoreCollaboration();
    }

    /**
     * Tests {@link ElasticsearchQueryBuilderVisitor#buildSimpleExpression(PrimitiveStatement)} for the case where
     * we're building a {@link ConditionType#LESS_THAN} expression.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testBuildSimpleLessThanExpression() throws Exception {
        final Object value = "hello";
        final String property = "property";

        doReturn(value).when(classValue).getValue();
        doReturn(LESS_THAN).when(statement).getCondition();
        doReturn(property).when(statement).getProperty();

        assertThat(visitor.buildSimpleExpression(statement), instanceOf(RangeQueryBuilder.class));

        verify(visitor).buildSimpleExpression(statement);
        verify(visitor).doGetPrimitiveFieldClass(statement);
        verify(visitor).validateNotCollectionCheck(statement, classValue);
        verify(visitor).createRangeQuery(property);
        verify(visitor).getEnumSafeValue(classValue);

        verify(statement).getProperty();
        verify(statement).getCondition();

        verify(rangeQueryBuilder).lt(value);

        verifyNoMoreCollaboration();
    }

    /**
     * Tests {@link ElasticsearchQueryBuilderVisitor#buildSimpleExpression(PrimitiveStatement)} for the case where
     * we're building a {@link ConditionType#LESS_OR_EQUALS} expression.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testBuildSimpleLessThanOrEqualsExpression() throws Exception {
        final Object value = "hello";
        final String property = "property";

        doReturn(value).when(classValue).getValue();
        doReturn(LESS_OR_EQUALS).when(statement).getCondition();
        doReturn(property).when(statement).getProperty();

        assertThat(visitor.buildSimpleExpression(statement), instanceOf(RangeQueryBuilder.class));

        verify(visitor).buildSimpleExpression(statement);
        verify(visitor).doGetPrimitiveFieldClass(statement);
        verify(visitor).validateNotCollectionCheck(statement, classValue);
        verify(visitor).createRangeQuery(property);
        verify(visitor).getEnumSafeValue(classValue);

        verify(statement).getProperty();
        verify(statement).getCondition();

        verify(rangeQueryBuilder).lte(value);

        verifyNoMoreCollaboration();
    }

    /**
     * Tests {@link ElasticsearchQueryBuilderVisitor#buildSimpleExpression(PrimitiveStatement)} for the case where
     * we're building a {@link ConditionType#GREATER_THAN} expression.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testBuildSimpleGreaterThanExpression() throws Exception {
        final Object value = "hello";
        final String property = "property";

        doReturn(value).when(classValue).getValue();
        doReturn(GREATER_THAN).when(statement).getCondition();
        doReturn(property).when(statement).getProperty();

        assertThat(visitor.buildSimpleExpression(statement), instanceOf(RangeQueryBuilder.class));

        verify(visitor).buildSimpleExpression(statement);
        verify(visitor).doGetPrimitiveFieldClass(statement);
        verify(visitor).validateNotCollectionCheck(statement, classValue);
        verify(visitor).createRangeQuery(property);
        verify(visitor).getEnumSafeValue(classValue);

        verify(statement).getProperty();
        verify(statement).getCondition();

        verify(rangeQueryBuilder).gt(value);

        verifyNoMoreCollaboration();
    }

    /**
     * Tests {@link ElasticsearchQueryBuilderVisitor#buildSimpleExpression(PrimitiveStatement)} for the case where
     * we're building a {@link ConditionType#GREATER_OR_EQUALS} expression.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testBuildSimpleGreaterThanOrEqualsExpression() throws Exception {
        final Object value = "hello";
        final String property = "property";

        doReturn(value).when(classValue).getValue();
        doReturn(GREATER_OR_EQUALS).when(statement).getCondition();
        doReturn(property).when(statement).getProperty();

        assertThat(visitor.buildSimpleExpression(statement), instanceOf(RangeQueryBuilder.class));

        verify(visitor).buildSimpleExpression(statement);
        verify(visitor).doGetPrimitiveFieldClass(statement);
        verify(visitor).validateNotCollectionCheck(statement, classValue);
        verify(visitor).createRangeQuery(property);
        verify(visitor).getEnumSafeValue(classValue);

        verify(statement).getProperty();
        verify(statement).getCondition();

        verify(rangeQueryBuilder).gte(value);

        verifyNoMoreCollaboration();
    }

    /**
     * Tests {@link ElasticsearchQueryBuilderVisitor#buildSimpleExpression(PrimitiveStatement)} for the case where
     * someone has passed us a compound operator such as AND or OR.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testBuildSimpleExpressionForCompoundOperator() throws Exception {
        Arrays.asList(AND, OR, CUSTOM).forEach(condition -> {
            final Object value = "hello";
            final String property = "property";
            final ElasticsearchQueryBuilderVisitor<MetadataRecord> localVisitor = spy(new ElasticsearchQueryBuilderVisitor<>());
            final PrimitiveStatement localStatement = mock(PrimitiveStatement.class);

            doReturn(classValue).when(localVisitor).doGetPrimitiveFieldClass(any(PrimitiveStatement.class));

            doAnswer(answer -> answer.getArgumentAt(0, TestSearchConditionVisitor.ClassValue.class).getValue())
                    .when(localVisitor).getEnumSafeValue(classValue);

            doReturn(value).when(classValue).getValue();
            doReturn(condition).when(localStatement).getCondition();
            doReturn(property).when(localStatement).getProperty();

            assertThat(localVisitor.buildSimpleExpression(localStatement), is(nullValue()));

            verify(localVisitor).buildSimpleExpression(localStatement);
            verify(localVisitor).doGetPrimitiveFieldClass(localStatement);
            verify(localVisitor).validateNotCollectionCheck(localStatement, classValue);
            verify(localVisitor).getEnumSafeValue(classValue);

            verify(localStatement).getCondition();

            verifyNoMoreCollaboration();
        });
    }

    /**
     * Tests {@link ElasticsearchQueryBuilderVisitor#buildCompositeExpression(ConditionType, List)} for the case
     * where the passed {@link ConditionType} is {@link ConditionType#AND}. We should AND together the set of parts.
     */
    @Test
    public void testBuildCompositeExpressionForAnd() throws Exception {
        final ConditionType conditionType = AND;
        final QueryBuilder part = mock(QueryBuilder.class);
        final BoolQueryBuilder boolQueryBuilder = mock(BoolQueryBuilder.class);
        final List<QueryBuilder> parts = Collections.singletonList(part);

        doReturn(boolQueryBuilder).when(visitor).createBoolQueryBuilder();

        visitor.buildCompositeExpression(conditionType, parts);

        verify(visitor).buildCompositeExpression(conditionType, parts);
        verify(visitor).createBoolQueryBuilder();

        verify(boolQueryBuilder).must(part);

        verifyNoMoreCollaboration(part, boolQueryBuilder);
    }

    /**
     * Tests {@link ElasticsearchQueryBuilderVisitor#buildCompositeExpression(ConditionType, List)} for the case
     * where the passed {@link ConditionType} is anything but {@link ConditionType#AND}. We should OR together the set
     * of parts.
     */
    @Test
    public void testBuildCompositeExpressionForNotAnd() throws Exception {
        Stream.of(ConditionType.values())
                .filter(type -> !type.equals(AND))
                .forEach(type -> {
                    final ElasticsearchQueryBuilderVisitor<MetadataRecord> localVisitor =
                            spy(new ElasticsearchQueryBuilderVisitor<>());

                    final QueryBuilder part = mock(QueryBuilder.class);
                    final BoolQueryBuilder boolQueryBuilder = mock(BoolQueryBuilder.class);
                    final List<QueryBuilder> parts = Collections.singletonList(part);

                    doReturn(boolQueryBuilder).when(localVisitor).createBoolQueryBuilder();

                    localVisitor.buildCompositeExpression(type, parts);

                    verify(localVisitor).buildCompositeExpression(type, parts);
                    verify(localVisitor).createBoolQueryBuilder();

                    verify(boolQueryBuilder).should(part);

                    verifyNoMoreCollaboration(part, boolQueryBuilder);
                });
    }

    /**
     * Tests {@link ElasticsearchQueryBuilderVisitor#buildCompositeExpression(ConditionType, List)} for the case
     * where we have multiple query parts and can merge them.
     */
    @Test
    public void testBuildCompositeExpressionWithMultipleParts() throws Exception {
        doReturn(true).when(visitor).canMergeQueryParts(any(QueryBuilder.class), any(QueryBuilder.class), any(ConditionType.class));
        doNothing().when(visitor).mergeQueryParts(any(QueryBuilder.class), any(QueryBuilder.class));

        final ConditionType conditionType = AND;
        final QueryBuilder part = mock(QueryBuilder.class);
        final QueryBuilder secondPart = mock(QueryBuilder.class);
        final BoolQueryBuilder boolQueryBuilder = mock(BoolQueryBuilder.class);
        final List<QueryBuilder> parts = Arrays.asList(part, secondPart);

        doReturn(boolQueryBuilder).when(visitor).createBoolQueryBuilder();
        doReturn(part).when(visitor).getPreviousQueryPart(boolQueryBuilder, conditionType);

        visitor.buildCompositeExpression(conditionType, parts);

        verify(visitor).buildCompositeExpression(conditionType, parts);
        verify(visitor).createBoolQueryBuilder();
        verify(visitor).getPreviousQueryPart(boolQueryBuilder, conditionType);
        verify(visitor).canMergeQueryParts(part, secondPart, conditionType);
        verify(visitor).mergeQueryParts(part, secondPart);

        verify(boolQueryBuilder).must(part);

        verifyNoMoreCollaboration(part, boolQueryBuilder);
    }

    /**
     * Tests {@link ElasticsearchQueryBuilderVisitor#buildCompositeExpression(ConditionType, List)} for the case
     * where we have multiple query parts and cannot merge them.
     */
    @Test
    public void testBuildCompositeExpressionWithMultipleUnmergeableParts() throws Exception {
        doReturn(false).when(visitor).canMergeQueryParts(any(QueryBuilder.class), any(QueryBuilder.class), any(ConditionType.class));

        final ConditionType conditionType = AND;
        final QueryBuilder part = mock(QueryBuilder.class);
        final QueryBuilder secondPart = mock(QueryBuilder.class);
        final BoolQueryBuilder boolQueryBuilder = mock(BoolQueryBuilder.class);
        final List<QueryBuilder> parts = Arrays.asList(part, secondPart);

        doReturn(boolQueryBuilder).when(visitor).createBoolQueryBuilder();
        doReturn(part).when(visitor).getPreviousQueryPart(boolQueryBuilder, conditionType);

        visitor.buildCompositeExpression(conditionType, parts);

        verify(visitor).buildCompositeExpression(conditionType, parts);
        verify(visitor).createBoolQueryBuilder();
        verify(visitor).getPreviousQueryPart(boolQueryBuilder, conditionType);
        verify(visitor).canMergeQueryParts(part, secondPart, conditionType);

        verify(boolQueryBuilder).must(part);
        verify(boolQueryBuilder).must(secondPart);

        verifyNoMoreCollaboration(part, boolQueryBuilder);
    }

    /**
     * Tests {@link ElasticsearchQueryBuilderVisitor#getEnumSafeValue(AbstractSearchConditionVisitor.ClassValue)} for the
     * case where the given {@link TestSearchConditionVisitor.ClassValue} represents an enum. Elasticsearch no longer
     * does {@link #toString()} for us on enum values, so we need to do this prior to sending the query over the wire.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testGetEnumSafeValueForEnum() throws Exception {
        doCallRealMethod().when(visitor).getEnumSafeValue(classValue);

        doReturn(Status.AVAILABLE).when(classValue).getValue();
        doReturn(Status.class).when(classValue).getCls();

        assertThat(visitor.getEnumSafeValue(classValue), is("AVAILABLE"));

        verify(visitor).getEnumSafeValue(classValue);

        verify(classValue).getCls();
        verify(classValue).getValue();

        verifyNoMoreCollaboration();
    }

    /**
     * Tests {@link ElasticsearchQueryBuilderVisitor#getEnumSafeValue(AbstractSearchConditionVisitor.ClassValue)} for
     * the case where the {@link TestSearchConditionVisitor.ClassValue} is not an enum. This should behave in a normal
     * fashion.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testGetEnumSafeValueForNonEnum() throws Exception {
        doCallRealMethod().when(visitor).getEnumSafeValue(classValue);

        doReturn("Hello").when(classValue).getValue();
        doReturn(String.class).when(classValue).getCls();

        assertThat(visitor.getEnumSafeValue(classValue), is("Hello"));

        verify(visitor).getEnumSafeValue(classValue);

        verify(classValue).getCls();
        verify(classValue).getValue();

        verifyNoMoreCollaboration();
    }

    /**
     * Tests {@link ElasticsearchQueryBuilderVisitor#getPreviousQueryPart(BoolQueryBuilder, ConditionType)} for the case
     * where we're constructing a compound {@link ConditionType#AND} query.
     */
    @Test
    public void testGetPreviousQueryPartForAndQuery() throws Exception {
        final BoolQueryBuilder compoundBuilder = mock(BoolQueryBuilder.class);
        final QueryBuilder builderTheFirst = mock(QueryBuilder.class);
        final QueryBuilder builderTheSecond = mock(QueryBuilder.class);

        doReturn(Arrays.asList(builderTheFirst, builderTheSecond)).when(compoundBuilder).must();

        assertThat(visitor.getPreviousQueryPart(compoundBuilder, ConditionType.AND), is(builderTheSecond));
    }

    /**
     * Tests {@link ElasticsearchQueryBuilderVisitor#getPreviousQueryPart(BoolQueryBuilder, ConditionType)} for the case
     * where we're constructing a compound {@link ConditionType#OR} query.
     */
    @Test
    public void testGetPreviousQueryPartforOrQuery() throws Exception {
        final BoolQueryBuilder compoundBuilder = mock(BoolQueryBuilder.class);
        final QueryBuilder builderTheFirst = mock(QueryBuilder.class);
        final QueryBuilder builderTheSecond = mock(QueryBuilder.class);

        doReturn(Arrays.asList(builderTheFirst, builderTheSecond)).when(compoundBuilder).should();

        assertThat(visitor.getPreviousQueryPart(compoundBuilder, ConditionType.OR), is(builderTheSecond));
    }

    /**
     * Tests {@link ElasticsearchQueryBuilderVisitor#canMergeQueryParts(QueryBuilder, QueryBuilder, ConditionType)} for
     * the happy path.
     */
    @Test
    public void testCanMergeQueryParts() throws Exception {
        final RangeQueryBuilder previousPart = mock(RangeQueryBuilder.class);
        final RangeQueryBuilder currentPart = mock(RangeQueryBuilder.class);

        doReturn("taters").when(previousPart).fieldName();
        doReturn("taters").when(currentPart).fieldName();

        assertThat(visitor.canMergeQueryParts(previousPart, currentPart, ConditionType.AND), is(true));
    }

    /**
     * Tests {@link ElasticsearchQueryBuilderVisitor#canMergeQueryParts(QueryBuilder, QueryBuilder, ConditionType)} for
     * the case where something other than {@link ConditionType#AND} was passed in. This will usually be something like
     * {@link ConditionType#OR}.
     */
    @Test
    public void testCanMergeQueryPartsForNonAndCondition() throws Exception {
        Stream.of(ConditionType.values())
                .filter(type -> !type.equals(AND))
                .forEach(type -> {
                    final RangeQueryBuilder previousPart = mock(RangeQueryBuilder.class);
                    final RangeQueryBuilder currentPart = mock(RangeQueryBuilder.class);

                    doReturn("taters").when(previousPart).fieldName();
                    doReturn("taters").when(currentPart).fieldName();

                    assertThat(visitor.canMergeQueryParts(previousPart, currentPart, type), is(false));
                });
    }

    /**
     * Tests {@link ElasticsearchQueryBuilderVisitor#canMergeQueryParts(QueryBuilder, QueryBuilder, ConditionType)} for
     * the case where the previous query part is not a {@link RangeQueryBuilder}. You might see this with something like
     * an expression like {@code foo==3;foo=le=400}.
     */
    @Test
    public void testCanMergeQueryPartsForNonRangePreviousQueryPart() throws Exception {
        final TermQueryBuilder previousPart = mock(TermQueryBuilder.class);
        final RangeQueryBuilder currentPart = mock(RangeQueryBuilder.class);

        doReturn("taters").when(previousPart).fieldName();
        doReturn("taters").when(currentPart).fieldName();

        assertThat(visitor.canMergeQueryParts(previousPart, currentPart, ConditionType.AND), is(false));
    }

    /**
     * Tests {@link ElasticsearchQueryBuilderVisitor#canMergeQueryParts(QueryBuilder, QueryBuilder, ConditionType)} for
     * the case where the current query part is not a {@link RangeQueryBuilder}. You might see this with something like
     * an expression like {@code foo=le=300;foo==4}.
     */
    @Test
    public void testCanMergeQueryPartsForNonRangeCurrentQueryPart() throws Exception {
        final RangeQueryBuilder previousPart = mock(RangeQueryBuilder.class);
        final WildcardQueryBuilder currentPart = mock(WildcardQueryBuilder.class);

        doReturn("taters").when(previousPart).fieldName();
        doReturn("taters").when(currentPart).fieldName();

        assertThat(visitor.canMergeQueryParts(previousPart, currentPart, ConditionType.AND), is(false));
    }

    /**
     * Tests {@link ElasticsearchQueryBuilderVisitor#canMergeQueryParts(QueryBuilder, QueryBuilder, ConditionType)} for
     * the case where both query parts are lining up, except they are pointing at different fields. This would be
     * something like {@code foo=le=300;bar=ge=400}.
     */
    @Test
    public void testCanMergeQueryPartsForDifferentFields() throws Exception {
        final RangeQueryBuilder previousPart = mock(RangeQueryBuilder.class);
        final RangeQueryBuilder currentPart = mock(RangeQueryBuilder.class);

        doReturn("taters").when(previousPart).fieldName();
        doReturn("definitely_not_taters").when(currentPart).fieldName();

        assertThat(visitor.canMergeQueryParts(previousPart, currentPart, ConditionType.AND), is(false));
    }

    /**
     * Tests {@link ElasticsearchQueryBuilderVisitor#mergeQueryParts(QueryBuilder, QueryBuilder)} for the case where our
     * current query part is a less than/less than or equal to.
     */
    @Test
    public void testMergeQueryPartsForLessThan() throws Exception {
        final RangeQueryBuilder previousPart = mock(RangeQueryBuilder.class);
        final RangeQueryBuilder currentPart = mock(RangeQueryBuilder.class);
        final String toValue = "4000";
        final boolean includeUpper = true;

        doReturn(toValue).when(currentPart).to();
        doReturn(includeUpper).when(currentPart).includeUpper();

        visitor.mergeQueryParts(previousPart, currentPart);

        verify(visitor).mergeQueryParts(previousPart, currentPart);

        verify(previousPart).to(toValue);
        verify(previousPart).includeUpper(includeUpper);

        verify(currentPart, times(2)).to();
        verify(currentPart).includeUpper();

        verifyNoMoreCollaboration(previousPart, currentPart);
    }

    /**
     * Tests {@link ElasticsearchQueryBuilderVisitor#mergeQueryParts(QueryBuilder, QueryBuilder)} for the case where our
     * current query part is a greater than/greater than or equal to.
     */
    @Test
    public void testMergeQueryPartsForGreaterThan() throws Exception {
        final RangeQueryBuilder previousPart = mock(RangeQueryBuilder.class);
        final RangeQueryBuilder currentPart = mock(RangeQueryBuilder.class);
        final String fromValue = "4000";
        final boolean includeLower = true;

        doReturn(fromValue).when(currentPart).from();
        doReturn(includeLower).when(currentPart).includeLower();

        visitor.mergeQueryParts(previousPart, currentPart);

        verify(visitor).mergeQueryParts(previousPart, currentPart);

        verify(previousPart).from(fromValue);
        verify(previousPart).includeLower(includeLower);

        verify(currentPart).to();
        verify(currentPart).from();
        verify(currentPart).includeLower();

        verifyNoMoreCollaboration(previousPart, currentPart);
    }

    /**
     * Tests {@link ElasticsearchQueryBuilderVisitor#validateNotCollectionCheck(PrimitiveStatement, AbstractSearchConditionVisitor.ClassValue)}
     * for the case where the statement is operating on a single value, and not a collection.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testValidateNotCollectionCheckForSingle() throws Exception {
        final TestSearchConditionVisitor.ClassValue classValue = mock(TestSearchConditionVisitor.ClassValue.class);

        visitor.validateNotCollectionCheck(statement, classValue);

        verify(visitor).validateNotCollectionCheck(statement, classValue);
        verify(classValue).getCollectionCheckInfo();

        verifyNoMoreCollaboration(classValue);
    }

    /**
     * Tests {@link ElasticsearchQueryBuilderVisitor#validateNotCollectionCheck(PrimitiveStatement, AbstractSearchConditionVisitor.ClassValue)}
     * for the case where the statement is operating on a collection property.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testValidateNotCollectionCheckForCollection() throws Exception {
        final TestSearchConditionVisitor.ClassValue classValue = mock(TestSearchConditionVisitor.ClassValue.class);
        final CollectionCheckInfo collectionCheckInfo = mock(CollectionCheckInfo.class);

        doReturn(collectionCheckInfo).when(classValue).getCollectionCheckInfo();
        doReturn("awesomeProperty").when(statement).getProperty();
        doReturn(ConditionType.CUSTOM).when(statement).getCondition();

        doReturn(CollectionCheck.SIZE).when(collectionCheckInfo).getCollectionCheckType();
        doReturn("veryValuable").when(collectionCheckInfo).getCollectionCheckValue();

        try {
            visitor.validateNotCollectionCheck(statement, classValue);
            fail("Oops, we should have caught an exception here...");
        } catch (final IllegalArgumentException ex) {
            assertThat(ex.getMessage(), is("Query contains an illegal operation: awesomeProperty CUSTOM SIZE veryValuable"));
        }

        verify(visitor).validateNotCollectionCheck(statement, classValue);

        verify(statement).getProperty();
        verify(statement).getCondition();

        verify(collectionCheckInfo).getCollectionCheckValue();
        verify(collectionCheckInfo).getCollectionCheckValue();

        verify(classValue).getCollectionCheckInfo();

        verifyNoMoreCollaboration(classValue);
    }


    /**
     * Tests {@link ElasticsearchQueryBuilderVisitor#createBoolQueryBuilder()} for the happy path.
     */
    @Test
    public void testCreateBoolQueryBuilder() throws Exception {
        doCallRealMethod().when(visitor).createBoolQueryBuilder();

        assertThat(visitor.createBoolQueryBuilder(), is(notNullValue()));

        verify(visitor).createBoolQueryBuilder();

        verifyNoMoreCollaboration(visitor);
    }

    /**
     * Tests {@link ElasticsearchQueryBuilderVisitor#createRangeQuery(String)} to make sure it does what we think it does.
     */
    @Test
    public void testCreateRangeQuery() throws Exception {
        doCallRealMethod().when(visitor).createRangeQuery(anyString());

        final RangeQueryBuilder queryBuilder = visitor.createRangeQuery("taters");

        assertThat(queryBuilder.fieldName(), is("taters"));

        verify(visitor).createRangeQuery("taters");

        verifyNoMoreCollaboration(visitor);
    }

    /**
     * Tests {@link ElasticsearchQueryBuilderVisitor#createTermQuery(String, Object)} to make sure it does what we think it does.
     */
    @Test
    public void testCreateTermQuery() throws Exception {
        doCallRealMethod().when(visitor).createTermQuery(anyString(), any());

        final TermQueryBuilder queryBuilder = visitor.createTermQuery("fieldNameTaters", "tatersValue");

        assertThat(queryBuilder.fieldName(), is("fieldNameTaters"));
        assertThat(queryBuilder.value(), is("tatersValue"));

        verify(visitor).createTermQuery("fieldNameTaters", "tatersValue");

        verifyNoMoreCollaboration(visitor);
    }

    /**
     * Tests {@link ElasticsearchQueryBuilderVisitor#createWildcardQuery(String, String)} (String, Object)} to make sure
     * it does what we think it does.
     */
    @Test
    public void testCreateWildcardQuery() throws Exception {
        doCallRealMethod().when(visitor).createWildcardQuery(anyString(), anyString());

        final WildcardQueryBuilder queryBuilder = visitor.createWildcardQuery("fieldNameTaters", "tatersValue");

        assertThat(queryBuilder.fieldName(), is("fieldNameTaters"));
        assertThat(queryBuilder.value(), is("tatersValue"));

        verify(visitor).createWildcardQuery("fieldNameTaters", "tatersValue");

        verifyNoMoreCollaboration(visitor);
    }

    private void verifyNoMoreCollaboration(final Object... additionalCollaborators) {
        verifyNoMoreInteractions(visitor, statement, wildcardQueryBuilder, boolQueryBuilder, termQueryBuilder, rangeQueryBuilder);
        Stream.of(additionalCollaborators)
                .forEach(Mockito::verifyNoMoreInteractions);
    }
}
