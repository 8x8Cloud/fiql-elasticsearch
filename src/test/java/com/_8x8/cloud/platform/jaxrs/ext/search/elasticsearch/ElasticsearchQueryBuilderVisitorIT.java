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
import org.apache.commons.io.IOUtils;
import org.apache.cxf.jaxrs.ext.search.SearchParseException;
import org.apache.cxf.jaxrs.ext.search.fiql.FiqlParser;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Tests the {@link ElasticsearchQueryBuilderVisitor} at the integration level.
 */
public class ElasticsearchQueryBuilderVisitorIT {
    /**
     * Holds the default {@link TimeZone} prior to running the test, so that we can make sure to reset when we're done.
     */
    private static final TimeZone defaultTimezone = TimeZone.getDefault();

    /**
     * Holds an instance of the {@link FiqlParser} we'll use for parsing the actual expressions. This will in turn be
     * passed to our {@link ElasticsearchQueryBuilderVisitor}.
     */
    private final FiqlParser<MetadataRecord> parser = new FiqlParser<>(MetadataRecord.class);

    /**
     * Hold on to a {@link TestName} rule so that we can do some clever JSON comparison at the end of our test.
     */
    @Rule
    public final TestName testName = new TestName();

    @Before
    public void setUp() throws Exception {
        // Our known good JSON fragments are taken in GMT to avoid localization issues. Let's set this for our tests so
        // we don't wind up in trouble comparing serialized time formats.
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
    }

    @After
    public void tearDown() throws Exception {
        TimeZone.setDefault(defaultTimezone);
    }

    /**
     * Tests the equality operator on a property that is a {@link String}.
     */
    @Test
    public void testStringEquality() throws Exception {
        doTest("tenantName==TestTenant");
    }

    /**
     * Tests the equality operator on a property that is a {@link String}, where the FIQL expression is a prefix
     * wildcard ({@code *}).
     */
    @Test
    public void testStringEqualityForWildcard() throws Exception {
        doTest("tenantName==Test*");
    }

    /**
     * Tests the equality operator on a property that is a {@link Long}.
     */
    @Test
    public void testLongEquality() throws Exception {
        doTest("storedBytes==5");
    }

    /**
     * Tests the equality operator on a property that is a {@link Date}. The default date formatter for JAX-RS Search
     * is {@code yyyy-MM-DD}.
     */
    @Test
    public void testDateEquality() throws Exception {
        doTest("updatedTime==2010-03-11");
    }

    /**
     * Tests the equality operator on a property that is an {@link Enum}. These should get translated to strings under
     * the hood.
     */
    @Test
    public void testEnumEquality() throws Exception {
        doTest("status==AVAILABLE");
    }

    /**
     * Tests the inequality operator on a property that is a {@link String}.
     */
    @Test
    public void testStringInequality() throws Exception {
        doTest("tenantName!=TestTenant");
    }

    /**
     * Tests the inequality operator on a property that is a {@link String}, where the FIQL expression is a prefix
     * wildcard ({@code *}).
     */
    @Test
    public void testStringInequalityForWildcard() throws Exception {
        doTest("tenantName!=Test*");
    }

    /**
     * Tests the inequality operator on a property that is a {@link Long}.
     */
    @Test
    public void testLongInequality() throws Exception {
        doTest("storedBytes!=5");
    }

    /**
     * Tests the inequality operator on a property that is a {@link Date}. The default date formatter for JAX-RS Search
     * is {@code yyyy-MM-DD}.
     */
    @Test
    public void testDateInequality() throws Exception {
        doTest("updatedTime!=2010-03-11");
    }

    /**
     * Tests the inequality operator on a property that is an {@link Enum}. These should get translated to strings under
     * the hood.
     */
    @Test
    public void testEnumInequality() throws Exception {
        doTest("status!=AVAILABLE");
    }

    /**
     * Tests the less than operator on a property that is a {@link Long}.
     */
    @Test
    public void testLongLessThan() throws Exception {
        doTest("storedBytes=lt=3");
    }

    /**
     * Tests the less than operator on a property that is a {@link Date}.
     */
    @Test
    public void testDateLessThan() throws Exception {
        doTest("updatedTime=lt=2010-03-11");
    }

    /**
     * Tests the greater than operator on a property that is a {@link Long}.
     */
    @Test
    public void testLongGreaterThan() throws Exception {
        doTest("storedBytes=gt=3");
    }

    /**
     * Tests the greater than operator on a property that is a {@link Date}.
     */
    @Test
    public void testDateGreaterThan() throws Exception {
        doTest("updatedTime=gt=2010-03-11");
    }

    /**
     * Tests the less than, or equal to operator on a property that is a {@link Long}.
     */
    @Test
    public void testLongLessThanOrEqualTo() throws Exception {
        doTest("storedBytes=le=3");
    }

    /**
     * Tests the less than, or equal to operator on a property that is a {@link Date}.
     */
    @Test
    public void testDateLessThanOrEqualTo() throws Exception {
        doTest("updatedTime=le=2010-03-11");
    }

    /**
     * Tests the greater than, or equal to operator on a property that is a {@link Long}.
     */
    @Test
    public void testLongGreaterThanOrEqualTo() throws Exception {
        doTest("storedBytes=ge=3");
    }

    /**
     * Tests the greater than, or equal to operator on a property that is a {@link Date}.
     */
    @Test
    public void testDateGreaterThanOrEqualTo() throws Exception {
        doTest("updatedTime=ge=2010-03-11");
    }

    /**
     * Tests an AND operator on a two-part AND operation.
     */
    @Test
    public void testAndOperator() throws Exception {
        doTest("tenantName==taters;containerName==delicious");
    }

    /**
     * Tests the case where we have two range operators in a compound statement that can be "collapsed" into a single
     * {@link RangeQueryBuilder}. So instead of {@code storedBytes=gt=100;storedBytes=lt=1000} issuing a {@code MUST}
     * across two {@link RangeQueryBuilder} - one for each term - we can issue a single {@link RangeQueryBuilder} that
     * encapsulates both criteria.
     */
    @Test
    public void testAndConditionRangeQueryCollapsing() throws Exception {
        doTest("storedBytes=gt=100;storedBytes=lt=1000");
    }

    /**
     * Tests the same thing as {@link #testAndConditionRangeQueryCollapsing()}, but with a reversed statement order in
     * the FIQL. This makes sure that query folding works properly in both directions.
     */
    @Test
    public void testAndConditionRangeQueryCollapsingForReversedQueryParts() throws Exception {
        doTest("storedBytes=lt=1000;storedBytes=gt=100");
    }

    /**
     * Tests query collapsing for the case where one of the values is greater than, or equal rather than a simple
     * greater than.
     */
    @Test
    public void testAndConditionRangeQueryCollapsingForGreaterThanOrEqualTo() throws Exception {
        doTest("storedBytes=ge=100;storedBytes=lt=1000");
    }

    /**
     * Tests query collapsing for the case where one of the values is less than, or equal rather than a simple
     * less than.
     */
    @Test
    public void testAndConditionRangeQueryCollapsingForLessThanOrEqualTo() throws Exception {
        doTest("storedBytes=gt=100;storedBytes=le=1000");
    }

    /**
     * Tests query collapsing for the case where someone has passed a filter that looks something like
     * {@code storedBytes=gt=100;storedBytes=le=1000;storedBytes=lt=500}. This checks to make sure that we properly
     * respect the inclusion values ({@code include_lower)/{@code include_upper}}.
     */
    @Test
    public void testAndConditionRangeQueryCollapsingForLessThanOrEqualToThenLessThan() throws Exception {
        doTest("storedBytes=gt=100;storedBytes=le=1000;storedBytes=lt=500");
    }

    /**
     * Tests query collapsing for the case where there is both a greater than/equal to, as well as a less than/equal to
     * being collapsed together.
     */
    @Test
    public void testAndConditionRangeQueryCollapsingForLessThanOrEqualToAndGreaterThanOrEqualTo() throws Exception {
        doTest("storedBytes=ge=100;storedBytes=le=1000");
    }

    /**
     * Tests the case where someone tries to apply {@code AND} condition collapsing multiple times to the same field.
     * This should be a "last value wins" sort of deal.
     */
    @Test
    public void testMultipleEquivalentAndConditionRangeQueryCollapsing() throws Exception {
        doTest("storedBytes=gt=100;storedBytes=lt=3000;storedBytes=lt=1000");
    }

    /**
     * Tests to make sure that {@code AND} condition collapsing only gets applied when the fields being referred to are
     * the same in both query parts. This is to prevent against  {@code storedBytes=gt=100;containerId=lt=1000} being
     * collapsed into a single query part.
     */
    @Test
    public void testAndConditionCollapsingForDifferentFields() throws Exception {
        doTest("storedBytes=gt=100;containerId=lt=1000");
    }

    /**
     * Tests to make sure that {@code AND} condition collapsing only gets applied when both query parts are
     * {@link RangeQueryBuilder} instances. This is to prevent against {@code storedBytes=gt=100;storedBytes==1000}
     * being collapsed into a single query part.
     */
    @Test
    public void testAndConditionCollapsingForDifferentQueryTypes() throws Exception {
        doTest("storedBytes=gt=100;storedBytes==1000");
    }

    /**
     * Tests to make sure that {@code AND} condition collapsing does not attempt to collapse queries that are at
     * different levels of the abstract syntax tree, as separated by parenthesis.
     * <p>
     * For example, if a user attempted to parse {@code (storedBytes=ge=300;storedBytes=le=400);storedBytes=ge=100} this
     * should be seen as two discrete queries: a compound query of what's in the parens
     * {@code (storedBytes <= 400 && storedBytes >= 300)}, and the second query of {@code storedBytes >= 100}. These must
     * not be collapsed into each other.
     */
    @Test
    public void testAndConditionCollapsingWithParenthesis() throws Exception {
        doTest("(storedBytes=ge=300;storedBytes=le=400);storedBytes=ge=100");
    }

    /**
     * Tests to make sure that {@code OR} leaf nodes do not get collapsed together, even when they're
     * {@link RangeQueryBuilder}s of the same field.
     */
    @Test
    public void testOrConditionRangeQueryCollapsing() throws Exception {
        doTest("storedBytes=gt=100,storedBytes=lt=1000");
    }

    /**
     * Tests an OR operator on a two-part OR operation.
     */
    @Test
    public void testOrOperator() throws Exception {
        doTest("tenantName==taters,containerName==delicious");
    }

    /**
     * Tests both the AND and OR operator in the same query.
     */
    @Test
    public void testBothAndOperatorAndOrOperator() throws Exception {
        doTest("tenantName==taters,(containerName==delicious;tenantName==dinner)");
    }

    /**
     * Tests a simple equality check on a property that is a {@link List} of {@link String}s. Some of the other visitors
     * will translate this into {@code [value]} due to the way they handle value resolution. In this case, we need a
     * scalar value.
     */
    @Test
    public void testCollectionQuery() throws Exception {
        doTest("tags==user:1234,tags==user:1234");
    }

    /**
     * Tests the {@code count} function on a property that is not a collection. In this case it should be stripped out
     * by the FIQL parser and treated as a normal field.
     */
    @Test
    public void testCountOnNonCollectionProperty() throws Exception {
        // In this case it acts like storedBytes=ge=2, and strips the count() function.
        doTest("count(storedBytes)=ge=2");
    }

    /**
     * Tests the {@code count} function on a collection property. As this has special meaning to FIQL, this operation
     * will actually fail with an exception explaining why.
     */
    @Test
    public void testCountOnCollectionProperty() throws Exception {
        try {
            doTest("count(tags)=ge=2");
            Assert.fail("Ooops, this should have broken our visitor.");
        } catch (final IllegalArgumentException ex) {
            assertThat(ex.getMessage(), is("Query contains an illegal operation: tags GREATER_OR_EQUALS SIZE 2"));
        }
    }

    /**
     * Under the hood, the FIQL library expects all dates to come in as something that can be formatted by a
     * {@link java.text.DateFormat} instance. This unfortunately prohibits us from using epochal timestamps.
     */
    @Test
    public void testParsingEpochalTimeForDate() throws Exception {
        try {
            doTest("updatedTime==1490334452");
            Assert.fail("Ooops, this should have broken our visitor.");
        } catch (final SearchParseException ex) {
            assertThat(ex.getMessage(), is("Can parse 1490334452 neither as date nor duration"));
        }
    }

    /**
     * Provides a convenience method to do a test run. Takes the {@link FiqlParser} created with the test, parses the
     * input FIQL, generates an AST and runs that through the {@link ElasticsearchQueryBuilderVisitor}. Once the visitor
     * is done visiting, the subsequent {@link QueryBuilder} returned is dumped to pretty-printed JSON and checked
     * against the expected expression.
     *
     * @param fiqlExpression The FIQL expression to visit.
     */
    private void doTest(final String fiqlExpression) throws Exception {
        // Build our QueryBuilder from our expression.
        final ElasticsearchQueryBuilderVisitor<MetadataRecord> elasticsearchQueryBuilderVisitor =
                new ElasticsearchQueryBuilderVisitor<>();
        elasticsearchQueryBuilderVisitor.visit(parser.parse(fiqlExpression));

        // Compare the output of this test run to our input file containing our expected result.
        final String testResourceName = String.format("/ElasticsearchQueryBuilderVisitorIT/%s.json", testName.getMethodName());

        try (final InputStream expected = getClass().getResourceAsStream(testResourceName)) {
            if (null == expected) {
                throw new RuntimeException(String.format("Failed to find resource %s, are you sure your file exists? Perhaps this is an invalid test scenario?", testResourceName));
            }

            assertThat(elasticsearchQueryBuilderVisitor.getQuery().toString(),
                    is(IOUtils.toString(expected, Charset.defaultCharset())));
        }
    }
}
