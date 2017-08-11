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
import org.apache.cxf.jaxrs.ext.search.AbstractSearchConditionParser;
import org.apache.cxf.jaxrs.ext.search.SearchUtils;
import org.apache.cxf.jaxrs.ext.search.fiql.FiqlParser;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Tests the {@link TranslatingQueryBuilder} at the unit level.
 */
public class TranslatingQueryBuilderTest {
    @SuppressWarnings("unchecked")
    private final FiqlParser<MetadataRecord> fiqlParser = (FiqlParser<MetadataRecord>) mock(FiqlParser.class);

    @SuppressWarnings("unchecked")
    private final TranslatingQueryBuilderVisitor<MetadataRecord> visitor =
            (TranslatingQueryBuilderVisitor<MetadataRecord>) mock(TranslatingQueryBuilderVisitor.class);

    private final Map<String, String> fieldMap = new HashMap<>();
    private final String dateformat = "MM/dd/yyyy";
    private final Map<String, FiqlTransformationFunction> transformationFunctionMap = new HashMap<>();

    private final TranslatingQueryBuilder<MetadataRecord> builder =
            spy(new TranslatingQueryBuilder<>(MetadataRecord.class, fieldMap, dateformat, transformationFunctionMap));

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Before
    public void setUp() throws Exception {
        doReturn(fiqlParser).when(builder).getFiqlParser();
        doReturn(visitor).when(builder).createVisitor(anyMapOf(String.class, String.class), anyString(),
                anyMapOf(String.class, FiqlTransformationFunction.class));
    }

    /**
     * Tests {@link TranslatingQueryBuilder#translateFiqlQuery(String)} to make sure it does what we expect.
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    public void testTranslateFiqlQuery() throws Exception {
        builder.translateFiqlQuery("taters");

        verify(builder).translateFiqlQuery("taters");
        verify(builder).createVisitor(fieldMap, dateformat, transformationFunctionMap);
        verify(builder).getFiqlParser();
        verify(builder).getFieldMap();
        verify(builder).getDateFormat();
        verify(builder).getTransformationFunctions();

        verify(visitor).visit(any());
        verify(visitor).getQuery();

        verify(fiqlParser).parse("taters");

        verifyNoMoreCollaboration();
    }

    /**
     * Tests {@link TranslatingQueryBuilder#createVisitor(Map, String, Map)} to make sure it does what we expect.
     */
    @Test
    public void testCreateVisitor() throws Exception {
        doCallRealMethod().when(builder).createVisitor(anyMapOf(String.class, String.class), anyString(),
                anyMapOf(String.class, FiqlTransformationFunction.class));

        final TranslatingQueryBuilderVisitor<MetadataRecord> translatingQueryBuilderVisitor =
                builder.createVisitor(fieldMap, dateformat, transformationFunctionMap);

        assertThat(translatingQueryBuilderVisitor, is(notNullValue()));
    }

    /**
     * Tests constructing a {@link TranslatingQueryBuilder} with a valid date format will also configure our {@link FiqlParser}.
     */
    @Test
    public void testConstructorWithDateFormat() throws Exception {
        final TranslatingQueryBuilder<MetadataRecord> translatingQueryBuilder =
                new TranslatingQueryBuilder<>(MetadataRecord.class, fieldMap, dateformat, transformationFunctionMap);

        final Map<String, String> contextConfiguration = getConfiguration(translatingQueryBuilder.getFiqlParser());

        assertThat(contextConfiguration.size(), is(1));
        assertThat(contextConfiguration.get(SearchUtils.DATE_FORMAT_PROPERTY), is(dateformat));
    }

    /**
     * Tests constructing a {@link TranslatingQueryBuilder} without a date format will not configure our corresponding
     * {@link FiqlParser}.
     */
    @Test
    public void testConstructorWithoutDateFormat() throws Exception {
        final TranslatingQueryBuilder<MetadataRecord> translatingQueryBuilder =
                new TranslatingQueryBuilder<>(MetadataRecord.class, fieldMap, null, transformationFunctionMap);

        final Map<String, String> contextConfiguration = getConfiguration(translatingQueryBuilder.getFiqlParser());

        assertThat(contextConfiguration.isEmpty(), is(true));
    }

    /**
     * Provides a utility method to grab the context configuration out of our base {@link TranslatingQueryBuilderVisitor}.
     * This field is set via a chaining call to the super constructor, never to be seen again: it's private, and
     * there are no accessor methods. Thusly must we resort to shenanigans and reflection to verify our field mappings.
     *
     * @param parser The {@link FiqlParser} to grab the context from.
     * @return The context map, ripped straight from the private internals of our base class.
     */
    @SuppressWarnings("unchecked")
    private Map<String, String> getConfiguration(final FiqlParser<?> parser) throws Exception {
        final Field field = AbstractSearchConditionParser.class.getDeclaredField("contextProperties");
        field.setAccessible(true);

        return (Map<String, String>) field.get(parser);
    }

    private void verifyNoMoreCollaboration(final Object... additionalCollaborators) {
        verifyNoMoreInteractions(fiqlParser, visitor, builder);
        Stream.of(additionalCollaborators)
                .forEach(Mockito::verifyNoMoreInteractions);
    }
}
