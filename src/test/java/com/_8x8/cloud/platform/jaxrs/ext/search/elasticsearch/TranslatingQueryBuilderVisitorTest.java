/*
 * Copyright (c) 2017 by 8x8, Inc. All rights reserved.
 *
 * This software is the confidential and proprietary information
 * of 8x8, Inc. ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you
 * entered into with 8x8, Inc.
 */

package com._8x8.cloud.platform.jaxrs.ext.search.elasticsearch;

import com._8x8.cloud.platform.jaxrs.ext.search.elasticsearch.model.MetadataRecord;
import org.apache.cxf.jaxrs.ext.search.ConditionType;
import org.apache.cxf.jaxrs.ext.search.PrimitiveStatement;
import org.apache.cxf.jaxrs.ext.search.SearchCondition;
import org.apache.cxf.jaxrs.ext.search.visitor.AbstractSearchConditionVisitor;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.TimeZone;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Tests the {@link TranslatingQueryBuilderVisitor} at the unit level.
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
public class TranslatingQueryBuilderVisitorTest {
    private final TestSearchConditionVisitor.ClassValue classValue = mock(TestSearchConditionVisitor.ClassValue.class);
    private final PrimitiveStatement statement = mock(PrimitiveStatement.class);

    private final TranslatingQueryBuilderVisitor<MetadataRecord> visitor =
            spy(new TranslatingQueryBuilderVisitor<>(null, null, null));

    @Before
    public void setUp() throws Exception {
        // FIQL has a nasty habit of using the local timezone... So, let's set our TZ to GMT for now.
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
    }

    /**
     * Tests constructing a {@link TranslatingQueryBuilderVisitor} with all null arguments. We should receive our default
     * configuration: an empty field map, the default date formatter, and no transformation functions.
     */
    @Test
    public void testConstructorForNullArguments() throws Exception {
        final TranslatingQueryBuilderVisitor<MetadataRecord> visitor =
                spy(new TranslatingQueryBuilderVisitor<>(null, null, null));

        // We should get an empty field mapping back.
        assertThat(getFieldMap(visitor).isEmpty(), is(true));

        // We should get back the default date formatter.
        assertThat(visitor.getSimpleDateFormat(), is(new SimpleDateFormat(TranslatingQueryBuilderVisitor.DEFAULT_DATE_FORMAT)));

        // We should have no transformation functions.
        assertThat(visitor.getTransformationFunctions().isEmpty(), is(true));

        // And our state stack is non-empty.
        assertThat(visitor.getStateStack().isEmpty(), is(false));
    }

    /**
     * Tests constructing a {@link TranslatingQueryBuilderVisitor} with a non-null field map. We should, unsurprisingly,
     * get a transformation function back.
     */
    @Test
    public void testConstructorForFieldMap() throws Exception {
        final Map<String, String> fieldMap = new HashMap<>();
        fieldMap.put("A", "B");

        final TranslatingQueryBuilderVisitor<MetadataRecord> visitor =
                spy(new TranslatingQueryBuilderVisitor<>(fieldMap, null, null));

        // We should now have a field mapping.
        assertThat(getFieldMap(visitor), is(fieldMap));

        // The date formatter is still the default.
        assertThat(visitor.getSimpleDateFormat(), is(new SimpleDateFormat(TranslatingQueryBuilderVisitor.DEFAULT_DATE_FORMAT)));

        // We still no transformation functions.
        assertThat(visitor.getTransformationFunctions().isEmpty(), is(true));

        // And our state stack is non-empty.
        assertThat(visitor.getStateStack().isEmpty(), is(false));
    }

    /**
     * Tests constructing a {@link TranslatingQueryBuilderVisitor} with a non-null date format. We should get back a
     * primed {@link SimpleDateFormat} here.
     */
    @Test
    public void testConstructorForDateFormat() throws Exception {
        final String dateFormat = "MM/dd/yyyy";

        final TranslatingQueryBuilderVisitor<MetadataRecord> visitor =
                spy(new TranslatingQueryBuilderVisitor<>(null, dateFormat, null));

        // We should get an empty field mapping back.
        assertThat(getFieldMap(visitor).isEmpty(), is(true));

        // We should get our custom format string here.
        assertThat(visitor.getSimpleDateFormat(), is(new SimpleDateFormat(dateFormat)));

        // We should have no transformation functions.
        assertThat(visitor.getTransformationFunctions().isEmpty(), is(true));

        // And our state stack is non-empty.
        assertThat(visitor.getStateStack().isEmpty(), is(false));
    }

    /**
     * Tests constructing a {@link TranslatingQueryBuilderVisitor} with a non-null mapping of
     * {@link FiqlTransformationFunction}s.
     */
    @Test
    public void testConstructorForTransformationFunction() throws Exception {
        final FiqlTransformationFunction fiqlTransformationFunction = mock(FiqlTransformationFunction.class);
        final Map<String, FiqlTransformationFunction> functionMap = new HashMap<>();
        functionMap.put("A", fiqlTransformationFunction);

        final TranslatingQueryBuilderVisitor<MetadataRecord> visitor =
                spy(new TranslatingQueryBuilderVisitor<>(null, null, functionMap));

        // We should get an empty field mapping back.
        assertThat(getFieldMap(visitor).isEmpty(), is(true));

        // We should get back the default date formatter.
        assertThat(visitor.getSimpleDateFormat(), is(new SimpleDateFormat(TranslatingQueryBuilderVisitor.DEFAULT_DATE_FORMAT)));

        // We should get back our function mapping.
        assertThat(visitor.getTransformationFunctions(), is(functionMap));

        // And our state stack is non-empty.
        assertThat(visitor.getStateStack().isEmpty(), is(false));
    }

    /**
     * Tests constructing a {@link TranslatingQueryBuilderVisitor} with all our glorious parameters fulfilled.
     */
    @Test
    public void testConstructorWithAllValuesNonNull() throws Exception {
        final Map<String, String> fieldMap = new HashMap<>();
        fieldMap.put("A", "B");

        final String dateFormat = "MM/dd/yyyy";

        final FiqlTransformationFunction fiqlTransformationFunction = mock(FiqlTransformationFunction.class);
        final Map<String, FiqlTransformationFunction> functionMap = new HashMap<>();
        functionMap.put("A", fiqlTransformationFunction);

        final TranslatingQueryBuilderVisitor<MetadataRecord> visitor =
                spy(new TranslatingQueryBuilderVisitor<>(fieldMap, dateFormat, functionMap));

        // Much like playing a Country music song in reverse, we get all our stuff back!
        assertThat(getFieldMap(visitor), is(fieldMap));
        assertThat(visitor.getSimpleDateFormat(), is(new SimpleDateFormat(dateFormat)));
        assertThat(visitor.getTransformationFunctions(), is(functionMap));

        // And our state stack is non-empty.
        assertThat(visitor.getStateStack().isEmpty(), is(false));
    }

    /**
     * Tests {@link TranslatingQueryBuilderVisitor#getDefaultHandler()} returns the handler we expect it to.
     */
    @Test
    public void testDefaultHandler() throws Exception {
        final String property = "A";
        final String operation = "==";
        final String value = "1003";

        final String parsedValue = new TranslatingQueryBuilderVisitor(null, null, null).getDefaultHandler()
                .apply(property, operation, value);

        // Out of the box, our default value should be <PROPERTY><OPERATION><VALUE>.
        assertThat(parsedValue, is("A==1003"));
    }

    /**
     * Tests {@link TranslatingQueryBuilderVisitor#visit(SearchCondition)} for a primitive statement (IE: foo==bar).
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testVisitForPrimitiveStatement() throws Exception {
        final String expression = "A==1003";

        final SearchCondition<MetadataRecord> searchCondition = mock(SearchCondition.class);
        doReturn(statement).when(searchCondition).getStatement();
        doReturn(expression).when(visitor).buildSimpleExpression(statement);
        doReturn("asdf").when(statement).getProperty();

        visitor.visit(searchCondition);

        verify(visitor).visit(searchCondition);
        verify(visitor).getStateStack();
        verify(visitor).buildSimpleExpression(statement);

        verify(searchCondition).getStatement();

        verify(statement).getProperty();

        verifyNoMoreCollaboration(searchCondition);

        // Our stack should have our expression in it.
        assertThat(visitor.getStateStack().size(), is(1));
        assertThat(visitor.getStateStack().peek(), is(Collections.singletonList(expression)));
    }

    /**
     * Tests {@link TranslatingQueryBuilderVisitor#visit(SearchCondition)} for a primitive statement (IE: foo==bar),
     * but in this case the statement property is null. Not entirely sure how this happens...
     */
    @SuppressWarnings({"unchecked", "ResultOfMethodCallIgnored"})
    @Test
    public void testVisitForPrimitiveStatementWithNullProperty() throws Exception {
        final String expression = "";

        final SearchCondition<MetadataRecord> searchCondition = mock(SearchCondition.class);
        doReturn(statement).when(searchCondition).getStatement();
        doReturn(expression).when(visitor).buildSimpleExpression(statement);

        visitor.visit(searchCondition);

        verify(visitor).visit(searchCondition);

        verify(searchCondition).getStatement();

        verify(statement).getProperty();

        verifyNoMoreCollaboration(searchCondition);

        // We should get back an empty state stack.
        assertThat(visitor.getStateStack().size(), is(1));
        assertThat(visitor.getStateStack().peek().isEmpty(), is(true));
    }

    /**
     * Tests {@link TranslatingQueryBuilderVisitor#visit(SearchCondition)} for a compound statement, such as something
     * with an AND or OR in it.
     */
    @SuppressWarnings({"unchecked", "ResultOfMethodCallIgnored"})
    @Test
    public void TranslatingQueryBuilderVisitor() throws Exception {
        final List<SearchCondition> searchConditions = new ArrayList<>();
        final SearchCondition<MetadataRecord> nestedCondition = mock(SearchCondition.class);
        searchConditions.add(nestedCondition);

        final SearchCondition<MetadataRecord> searchCondition = mock(SearchCondition.class);
        final String expression = "taters";

        doReturn(searchConditions).when(searchCondition).getSearchConditions();
        doReturn(expression).when(visitor).buildCompositeExpression(any(ConditionType.class), anyListOf(String.class));

        visitor.visit(searchCondition);

        verify(visitor).visit(searchCondition);
        verify(visitor, times(3)).getStateStack();
        verify(visitor).buildCompositeExpression(any(ConditionType.class), anyListOf(String.class));

        verify(searchCondition).getStatement();
        verify(searchCondition).getSearchConditions();
        verify(searchCondition).getConditionType();

        verify(searchCondition).getStatement();

        verify(nestedCondition).accept(visitor);

        verifyNoMoreCollaboration(searchCondition, nestedCondition);

        // We should get back our builder too.
        assertThat(visitor.getStateStack().size(), CoreMatchers.is(1));
        assertThat(visitor.getStateStack().peek(), CoreMatchers.is(Collections.singletonList(expression)));
    }

    /**
     * Tests {@link TranslatingQueryBuilderVisitor#getQuery()} for the case where our state stack is empty. This is most
     * likely impossible.
     */
    @Test
    public void testGetQueryForEmptyStateStack() throws Exception {
        doReturn(new Stack<List<String>>()).when(visitor).getStateStack();
        assertThat(visitor.getQuery(), is(""));
    }

    /**
     * Tests {@link TranslatingQueryBuilderVisitor#getQuery()} for the case where our state stack has a stack frame, but
     * there's nothing in it. Again, most likely impossible.
     */
    @Test
    public void testGetQueryForSingleFrameStateStack() throws Exception {
        final Stack<List<String>> stateStack = new Stack<>();
        stateStack.push(new ArrayList<>());

        doReturn(stateStack).when(visitor).getStateStack();
        assertThat(visitor.getQuery(), is(""));
    }

    /**
     * Tests {@link TranslatingQueryBuilderVisitor#getQuery()} for the case where we've got ourselves a query.
     */
    @Test
    public void testGetQuery() throws Exception {
        final String expression = "A==1003";
        final Stack<List<String>> stateStack = new Stack<>();
        stateStack.push(Collections.singletonList(expression));

        doReturn(stateStack).when(visitor).getStateStack();
        assertThat(visitor.getQuery(), is(expression));
    }

    /**
     * Tests {@link TranslatingQueryBuilderVisitor#buildSimpleExpression(PrimitiveStatement)} for the happy path.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testBuildSimpleExpression() throws Exception {
        // Approximate our default transformation function.
        final FiqlTransformationFunction fiqlTransformationFunction = mock(FiqlTransformationFunction.class);
        doReturn(fiqlTransformationFunction).when(visitor).getDefaultHandler();
        doAnswer(x -> String.format("%s%s%s", x.getArguments()[0], x.getArguments()[1], x.getArguments()[2]))
                .when(fiqlTransformationFunction).apply(anyString(), anyString(), anyString());

        // Override our property name return.
        final String propertyName = "A";
        doReturn(propertyName).when(statement).getProperty();
        doReturn(propertyName).when(visitor).getReallyRealPropertyName(anyString());

        // Deal with the ever awesome ClassValue access modifier...
        doReturn(classValue).when(visitor).doGetPrimitiveFieldClass(statement);
        final String dateSafeValue = "1003";
        doReturn(dateSafeValue).when(visitor).getDateSafeValue(classValue);

        // Wire up the rest of the fields we need to pay homage to the twisted internals of FIQL.
        final ConditionType conditionType = ConditionType.EQUALS;
        doReturn(conditionType).when(statement).getCondition();

        final String value = "1003";
        doReturn(value).when(statement).getValue();

        // Make sure we got the value we expected.
        final String expression = visitor.buildSimpleExpression(statement);
        assertThat(expression, is("A==1003"));

        // Now verify that there callstack.
        verify(visitor).buildSimpleExpression(statement);
        verify(visitor).getReallyRealPropertyName(propertyName);
        verify(visitor).getTransformationFunctions();
        verify(visitor).getDefaultHandler();
        verify(visitor).doGetPrimitiveFieldClass(statement);
        verify(visitor).getDateSafeValue(classValue);

        verify(statement).getProperty();
        verify(statement).getCondition();

        verify(fiqlTransformationFunction).apply("A", "==", "1003");

        verifyNoMoreCollaboration(fiqlTransformationFunction);
    }

    /**
     * Tests {@link TranslatingQueryBuilderVisitor#buildSimpleExpression(PrimitiveStatement)} for the case where the
     * condition type is unknown. This should probably be impossible.
     */
    @Test
    public void testBuildSimpleExpressionForUnknownOperator() throws Exception {
        // Override our property name return.
        final String propertyName = "A";
        doReturn(propertyName).when(statement).getProperty();
        doReturn(propertyName).when(visitor).getReallyRealPropertyName(anyString());

        // We don't support CUSTOM operators, so we should short circuit here.
        final ConditionType conditionType = ConditionType.CUSTOM;
        doReturn(conditionType).when(statement).getCondition();

        // Make sure we got the value we expected.
        final String expression = visitor.buildSimpleExpression(statement);
        assertThat(expression, is(""));

        // Now verify that there call stack.
        verify(visitor).buildSimpleExpression(statement);
        verify(visitor).getReallyRealPropertyName(propertyName);

        verify(statement).getProperty();
        verify(statement).getCondition();
    }

    /**
     * Tests {@link TranslatingQueryBuilderVisitor#buildCompositeExpression(ConditionType, List)} for the case where a
     * single part is present. This is something like {@code foo==bar}.
     */
    @Test
    public void testBuildCompositeExpressionForSinglePart() throws Exception {
        doReturn(false).when(visitor).requiresParenthesisWrapping();

        final ConditionType conditionType = ConditionType.AND;
        final List<String> parts = Collections.singletonList("A==1003");

        final String expression = visitor.buildCompositeExpression(conditionType, parts);

        // Not much has happened here. No parens, no gluing.
        assertThat(expression, is("A==1003"));

        verify(visitor).buildCompositeExpression(conditionType, parts);
        verify(visitor, times(2)).requiresParenthesisWrapping();

        verifyNoMoreCollaboration();
    }

    /**
     * Tests {@link TranslatingQueryBuilderVisitor#buildCompositeExpression(ConditionType, List)} for the case where a
     * multiple parts are present, and need to be AND'ed together. This is something like {@code foo==bar;quux==baz}.
     */
    @Test
    public void testBuildCompositeExpressionForMultiPartAnd() throws Exception {
        doReturn(true).when(visitor).requiresParenthesisWrapping();

        final ConditionType conditionType = ConditionType.AND;
        final List<String> parts = Arrays.asList("A==1003", "B==2002");

        final String expression = visitor.buildCompositeExpression(conditionType, parts);

        // Not much has happened here. No parens, no gluing.
        assertThat(expression, is("(A==1003;B==2002)"));

        verify(visitor).buildCompositeExpression(conditionType, parts);
        verify(visitor, times(2)).requiresParenthesisWrapping();

        verifyNoMoreCollaboration();
    }

    /**
     * Tests {@link TranslatingQueryBuilderVisitor#buildCompositeExpression(ConditionType, List)} for the case where a
     * multiple parts are present, and need to be OR'ed together. This is something like {@code foo==bar,quux==baz}.
     */
    @Test
    public void testBuildCompositeExpressionForMultiPartOr() throws Exception {
        doReturn(true).when(visitor).requiresParenthesisWrapping();

        final ConditionType conditionType = ConditionType.OR;
        final List<String> parts = Arrays.asList("A==1003", "B==2002");

        final String expression = visitor.buildCompositeExpression(conditionType, parts);

        // Not much has happened here. No parens, no gluing.
        assertThat(expression, is("(A==1003,B==2002)"));

        verify(visitor).buildCompositeExpression(conditionType, parts);
        verify(visitor, times(2)).requiresParenthesisWrapping();

        verifyNoMoreCollaboration();
    }

    /**
     * Tests {@link TranslatingQueryBuilderVisitor#requiresParenthesisWrapping()} to make sure it behaves as expected.
     * This call is mostly to make sure we don't wind up with extra parenthesis around a single, simple expression, such
     * as {@code foo==bar}.
     */
    @Test
    public void testRequiresParenthesisWrapping() throws Exception {
        final Stack<List<String>> state = new Stack<>();
        doReturn(state).when(visitor).getStateStack();

        // If our stack is empty, IE: no expression, we need no parenthesis.
        assertThat(visitor.requiresParenthesisWrapping(), is(false));

        // Likewise, a single stackframe is a simple expression (IE: foo==bar).
        state.push(new ArrayList<>());
        assertThat(visitor.requiresParenthesisWrapping(), is(false));

        // If we have two stack frames, however, we've got a complex composite expression (IE: foo==bar;quux==baz).
        state.push(new ArrayList<>());
        assertThat(visitor.requiresParenthesisWrapping(), is(true));
    }

    /**
     * Tests {@link TranslatingQueryBuilderVisitor#getReallyRealPropertyName(String)} to make sure it does what we
     * expect.
     */
    @Test
    public void testGetReallyRealPropertyName() throws Exception {
        final Map<String, String> fieldMap = new HashMap<>();
        fieldMap.put("taters", "notTaters");

        final TranslatingQueryBuilderVisitor<MetadataRecord> visitor =
                spy(new TranslatingQueryBuilderVisitor<>(fieldMap, null, null));

        // Under the hood this calls a protected method that looks up our field mapping.
        assertThat(visitor.getReallyRealPropertyName("taters"), is("notTaters"));
        assertThat(visitor.getReallyRealPropertyName("laters"), is("laters"));
    }

    /**
     * Tests {@link TranslatingQueryBuilderVisitor#getDateSafeValue(AbstractSearchConditionVisitor.ClassValue)} for the
     * case where the value is not a date.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testGetDateSafeValueForNonDate() throws Exception {
        doReturn(String.class).when(classValue).getCls();
        doReturn("test").when(classValue).getValue();

        assertThat(visitor.getDateSafeValue(classValue), is("test"));

        verify(visitor).getDateSafeValue(classValue);

        verify(classValue).getCls();
        verify(classValue).getValue();

        verifyNoMoreCollaboration();
    }

    /**
     * Tests {@link TranslatingQueryBuilderVisitor#getDateSafeValue(AbstractSearchConditionVisitor.ClassValue)} for the
     * case where the value is actually a date.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testGetDateSafeValueForDate() throws Exception {
        doReturn(Date.class).when(classValue).getCls();
        doReturn(new Date(1498545768000L)).when(classValue).getValue();

        assertThat(visitor.getDateSafeValue(classValue), is("2017-06-27T06:42:48"));

        verify(visitor).getDateSafeValue(classValue);
        verify(visitor).getSimpleDateFormat();

        verify(classValue).getCls();
        verify(classValue).getValue();

        verifyNoMoreCollaboration();
    }

    /**
     * Provides a utility method to grab the field map out of our base {@link AbstractSearchConditionVisitor}. This field
     * is set via a chaining call to the super constructor, never to be seen again: it's private, and there are no
     * accessor methods. Thusly must we resort to shenanigans and reflection to verify our field mappings.
     *
     * @param visitor The {@link TranslatingQueryBuilderVisitor} to grab the field map from.
     * @return The field map, ripped straight from the private internals of our base class.
     */
    @SuppressWarnings("unchecked")
    private Map<String, String> getFieldMap(final TranslatingQueryBuilderVisitor<?> visitor) throws Exception {
        final Field field = AbstractSearchConditionVisitor.class.getDeclaredField("fieldMap");
        field.setAccessible(true);

        return (Map<String, String>) field.get(visitor);
    }

    private void verifyNoMoreCollaboration(final Object... additionalCollaborators) {
        verifyNoMoreInteractions(visitor, statement, classValue);
        Stream.of(additionalCollaborators)
                .forEach(Mockito::verifyNoMoreInteractions);
    }
}
