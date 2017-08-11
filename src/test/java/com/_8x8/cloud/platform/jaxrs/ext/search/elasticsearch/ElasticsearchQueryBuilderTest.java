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
import org.apache.cxf.jaxrs.ext.search.fiql.FiqlParser;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.stream.Stream;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Tests the {@link ElasticsearchQueryBuilder} at the unit level.
 */
public class ElasticsearchQueryBuilderTest {
    @SuppressWarnings("unchecked")
    private final FiqlParser<MetadataRecord> fiqlParser = (FiqlParser<MetadataRecord>) mock(FiqlParser.class);

    @SuppressWarnings("unchecked")
    private final ElasticsearchQueryBuilderVisitor<MetadataRecord> visitor =
            (ElasticsearchQueryBuilderVisitor<MetadataRecord>) mock(ElasticsearchQueryBuilderVisitor.class);

    private final ElasticsearchQueryBuilder<MetadataRecord> elasticsearchQueryBuilder =
            spy(new ElasticsearchQueryBuilder<>(MetadataRecord.class));

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Before
    public void setUp() throws Exception {
        doReturn(fiqlParser).when(elasticsearchQueryBuilder).getFiqlParser();
        doReturn(visitor).when(elasticsearchQueryBuilder).createVisitor();
    }

    /**
     * Tests {@link ElasticsearchQueryBuilder#generateQueryBuilder(String)} to make sure it does what we expect.
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    public void testGenerateQueryBuilder() throws Exception {
        elasticsearchQueryBuilder.generateQueryBuilder("taters");

        verify(elasticsearchQueryBuilder).generateQueryBuilder("taters");
        verify(elasticsearchQueryBuilder).createVisitor();
        verify(elasticsearchQueryBuilder).getFiqlParser();

        verify(visitor).visit(any());
        verify(visitor).getQuery();

        verify(fiqlParser).parse("taters");

        verifyNoMoreCollaboration();
    }

    /**
     * Tests {@link ElasticsearchQueryBuilder#createVisitor()} to make sure it does what we expect.
     */
    @Test
    public void testCreateVisitor() throws Exception {
        doCallRealMethod().when(elasticsearchQueryBuilder).createVisitor();

        final ElasticsearchQueryBuilderVisitor<MetadataRecord> builder = elasticsearchQueryBuilder.createVisitor();

        assertThat(builder, is(notNullValue()));
    }

    private void verifyNoMoreCollaboration(final Object... additionalCollaborators) {
        verifyNoMoreInteractions(fiqlParser, visitor, elasticsearchQueryBuilder);
        Stream.of(additionalCollaborators)
                .forEach(Mockito::verifyNoMoreInteractions);
    }
}
