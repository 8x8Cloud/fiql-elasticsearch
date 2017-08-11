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
import org.elasticsearch.index.query.QueryBuilder;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Tests the {@link ElasticsearchQueryBuilder} at the integration level.
 */
public class ElasticsearchQueryBuilderIT {
    private final ElasticsearchQueryBuilder<MetadataRecord> elasticsearchQueryBuilder =
            new ElasticsearchQueryBuilder<>(MetadataRecord.class);

    /**
     * Tests {@link ElasticsearchQueryBuilder#generateQueryBuilder(String)} to make sure it generates the query we expect.
     */
    @Test
    public void testGenerateQueryBuilder() throws Exception {
        final String fiqlFilter = "tenantName==taters,(containerName==delicious;tenantName==dinner)";

        // Generate one of our newfangled queries
        final QueryBuilder generatedBuilder = elasticsearchQueryBuilder.generateQueryBuilder(fiqlFilter);

        // Use out-of-the-box approach
        final FiqlParser<MetadataRecord> parser = new FiqlParser<>(MetadataRecord.class);
        final ElasticsearchQueryBuilderVisitor<MetadataRecord> visitor = new ElasticsearchQueryBuilderVisitor<>();

        visitor.visit(parser.parse(fiqlFilter));
        final QueryBuilder builder = visitor.getQuery();

        assertThat(generatedBuilder.toString(), is(builder.toString()));
    }
}
