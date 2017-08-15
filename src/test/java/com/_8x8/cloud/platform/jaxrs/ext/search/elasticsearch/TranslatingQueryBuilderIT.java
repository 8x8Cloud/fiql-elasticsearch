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
import com._8x8.cloud.platform.jaxrs.ext.search.elasticsearch.model.MetadataSearchResult;
import org.apache.cxf.jaxrs.ext.search.fiql.FiqlParser;
import org.junit.Before;
import org.junit.Test;

import java.util.TimeZone;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Tests the {@link TranslatingQueryBuilder} at the integration level.
 */
public class TranslatingQueryBuilderIT {
    @Before
    public void setUp() throws Exception {
        // FIQL has a nasty habit of using the local timezone... So, let's set our TZ to GMT for now.
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
    }

    /**
     * Tests {@link TranslatingQueryBuilder#translateFiqlQuery(String)} to make sure it generates the query we expect.
     */
    @Test
    public void testTranslateFiqlQuery() throws Exception {
        final TranslatingQueryBuilder<MetadataRecord> translatingQueryBuilder =
                new TranslatingQueryBuilder<>(MetadataRecord.class, null, null, null);

        final String fiqlFilter = "tenantName==taters,(containerName==delicious;tenantName==dinner)";

        final String generatedFilter = translatingQueryBuilder.translateFiqlQuery(fiqlFilter);

        final FiqlParser<MetadataRecord> parser = new FiqlParser<>(MetadataRecord.class);
        final TranslatingQueryBuilderVisitor<MetadataRecord> visitor =
                new TranslatingQueryBuilderVisitor<>(null, null, null);

        visitor.visit(parser.parse(fiqlFilter));
        final String filter = visitor.getQuery();

        assertThat(generatedFilter, is(filter));
    }

    /**
     * Tests {@link TranslatingQueryBuilder#translateFiqlQuery(String)} for the case where no date format was provided.
     */
    @Test
    public void testTranslateFiqlQueryWithoutDateFormat() throws Exception {
        final TranslatingQueryBuilder<MetadataSearchResult> translatingQueryBuilder =
                new TranslatingQueryBuilder<>(MetadataSearchResult.class, null, null, null);

        final String fiqlFilter = "objectMetadata.lastUpdatedTime==2017-07-04T07:07:07.235-0700";
        String generatedFilter = translatingQueryBuilder.translateFiqlQuery(fiqlFilter);

        // Since we haven't specified a date format, we fall back to the default FIQL format, which is only day-level
        // precision. We then lose all minute/second data.
        assertThat(generatedFilter, is("objectMetadata.lastUpdatedTime==2017-07-04T00:00:00"));
    }

    /**
     * Tests {@link TranslatingQueryBuilder#translateFiqlQuery(String)} for the case where a date format was provided.
     */
    @Test
    public void testTranslatefiqlQueryWithDateFormat() throws Exception {
        final String dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

        final TranslatingQueryBuilder<MetadataSearchResult> translatingQueryBuilder =
                new TranslatingQueryBuilder<>(MetadataSearchResult.class, null, dateFormat, null);

        final String fiqlFilter = "objectMetadata.lastUpdatedTime==2017-07-04T07:07:07.235-0700";
        final String generatedFilter = translatingQueryBuilder.translateFiqlQuery(fiqlFilter);

        // Notice that with our custom dateFormat, it gets passed to both our translator and the FIQL parser, so that
        // we can have consistent date handling.
        assertThat(generatedFilter, is("objectMetadata.lastUpdatedTime==2017-07-04T14:07:07.235+0000"));
    }
}
