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
