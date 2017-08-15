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
import org.apache.cxf.jaxrs.ext.search.SearchUtils;
import org.apache.cxf.jaxrs.ext.search.fiql.FiqlParser;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Tests the {@link TranslatingQueryBuilderVisitor} at the integration level.
 */
public class TranslatingQueryBuilderVisitorIT {
    /**
     * Tests for the case where we do no translation, or transformation. We should get back exactly the string we fed
     * the visitor.
     */
    @Test
    public void testWithEmptyMapping() throws Exception {
        final String fiqlFilter = "tenantName==taters,(containerName==delicious;tenantName==dinner)";

        final FiqlParser<MetadataRecord> parser = new FiqlParser<>(MetadataRecord.class);
        final TranslatingQueryBuilderVisitor<MetadataRecord> visitor = new TranslatingQueryBuilderVisitor<>(null, null, null);

        visitor.visit(parser.parse(fiqlFilter));

        assertThat(fiqlFilter, is(visitor.getQuery()));
    }

    /**
     * Tests the case where we're doing field mappings. We should get back
     */
    @Test
    public void testWithMapping() throws Exception {
        // Map all of our properties except one, so we can make sure the mapping works in both directions.
        final Map<String, String> map = new HashMap<>();
        map.put("objectMetadata.tenantName", "tenantName");
        map.put("objectMetadata.containerName", "containerName");
        map.put("objectMetadata.containerId", "containerId");
        map.put("objectMetadata.sizeInBytes", "storedBytes");
        map.put("objectMetadata.lastUpdatedTime", "updatedTime");

        final String fiqlFilter = "objectMetadata.tenantName==TestTenant,(objectMetadata.containerName==TestContainer;objectMetadata.containerId==1234;objectMetadata.sizeInBytes==300;objectMetadata.lastUpdatedTime==2017-07-04;objectMetadata.status==AVAILABLE)";

        final FiqlParser<MetadataSearchResult> parser = new FiqlParser<>(MetadataSearchResult.class);
        final TranslatingQueryBuilderVisitor<MetadataSearchResult> visitor = new TranslatingQueryBuilderVisitor<>(map, null, null);

        visitor.visit(parser.parse(fiqlFilter));

        assertThat(visitor.getQuery(), is("tenantName==TestTenant,(containerName==TestContainer;containerId==1234;storedBytes==300;updatedTime==2017-07-04T00:00:00;objectMetadata.status==AVAILABLE)"));
    }

    /**
     * Tests using a custom {@link java.text.DateFormat} on a date field. By default we use the most granular allowed
     * by the CXF JAX-RS Search spec, which is {@link TranslatingQueryBuilderVisitor#DEFAULT_DATE_FORMAT}.
     */
    @Test
    public void testCustomDateFormat() throws Exception {
        final String dateFormat = "MM/dd/yyyy";

        final String fiqlFilter = "objectMetadata.lastUpdatedTime==2017-07-04;objectMetadata.status==AVAILABLE";

        final FiqlParser<MetadataSearchResult> parser = new FiqlParser<>(MetadataSearchResult.class);
        final TranslatingQueryBuilderVisitor<MetadataSearchResult> visitor = new TranslatingQueryBuilderVisitor<>(new HashMap<>(), dateFormat, null);

        visitor.visit(parser.parse(fiqlFilter));

        assertThat(visitor.getQuery(), is("objectMetadata.lastUpdatedTime==07/04/2017;objectMetadata.status==AVAILABLE"));
    }

    /**
     * Tests using a custom {@link FiqlTransformationFunction} to modify one of our values into a search tag.
     */
    @Test
    public void testCustomTransformationFunction() throws Exception {
        // Provide a transformation that takes something like "foo==bar" and turns it into "tags==foo:bar".
        final FiqlTransformationFunction elasticTagTransformation = (property, operation, value) -> String.format("tags%s%s:%s", operation, property, value);

        final Map<String, FiqlTransformationFunction> transformationFunctions = new HashMap<>();
        transformationFunctions.put("otherMetadata.foo", elasticTagTransformation);

        final String fiqlFilter = "objectMetadata.status==AVAILABLE,otherMetadata.foo==quux";

        final FiqlParser<MetadataSearchResult> parser = new FiqlParser<>(MetadataSearchResult.class);
        final TranslatingQueryBuilderVisitor<MetadataSearchResult> visitor = new TranslatingQueryBuilderVisitor<>(null, null, transformationFunctions);

        visitor.visit(parser.parse(fiqlFilter));

        assertThat(visitor.getQuery(), is("objectMetadata.status==AVAILABLE,tags==otherMetadata.foo:quux"));
    }

    /**
     * Now we're going to combine all the features together. This is when things get really interesting.
     */
    @Test
    public void testCustomTransformationFunctionAndMappingAndDateFormat() throws Exception {
        // Wire up our field mappings. Our transformation function will be applied after these, so let's map our transformed field.
        final Map<String, String> map = new HashMap<>();
        map.put("otherMetadata.foo", "notFoo");

        // Here's our custom date format.
        final String dateFormat = "MM_dd_yyyy";

        // And our familiar formatting function. This will take place on the transformed field above.
        final FiqlTransformationFunction elasticTagTransformation = (property, operation, value) -> String.format("tags%s%s:%s", operation, property, value);

        final Map<String, FiqlTransformationFunction> transformationFunctions = new HashMap<>();
        transformationFunctions.put("notFoo", elasticTagTransformation);

        final String fiqlFilter = "objectMetadata.lastUpdatedTime==2017-07-04;objectMetadata.status==AVAILABLE,otherMetadata.foo==quux";

        final FiqlParser<MetadataSearchResult> parser = new FiqlParser<>(MetadataSearchResult.class);
        final TranslatingQueryBuilderVisitor<MetadataSearchResult> visitor = new TranslatingQueryBuilderVisitor<>(map, dateFormat, transformationFunctions);

        visitor.visit(parser.parse(fiqlFilter));

        assertThat(visitor.getQuery(), is("(objectMetadata.lastUpdatedTime==07_04_2017;objectMetadata.status==AVAILABLE),tags==notFoo:quux"));
    }

    /**
     * Tests creating a {@link FiqlTransformationFunction} that translates from a string date format to epochal format.
     * Please note all the hoops you'll have to jump through with respect to the {@link FiqlParser}.
     */
    @Test
    public void testDateTransformationFunction() throws Exception {
        // Because the date our transformation function gets is in a string, we'll need to parse it from a String, to a Date,
        // then grab the long from that. Yeah, not ideal... But, it's better than nothing!
        final String dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
        final EpochalDateTransformationFunction epochalDateTransformationFunction = new EpochalDateTransformationFunction(dateFormat);

        // Load up our translation function.
        final Map<String, FiqlTransformationFunction> transformationFunctions = new HashMap<>();
        transformationFunctions.put("objectMetadata.lastUpdatedTime", epochalDateTransformationFunction);

        // Configure the FIQL parser not to strip off minutes from our Dates...
        final Map<String, String> fiqlParserProperties = new HashMap<>();
        fiqlParserProperties.put(SearchUtils.DATE_FORMAT_PROPERTY, dateFormat);

        final FiqlParser<MetadataSearchResult> parser = new FiqlParser<>(MetadataSearchResult.class, fiqlParserProperties);
        final TranslatingQueryBuilderVisitor<MetadataSearchResult> visitor =
                new TranslatingQueryBuilderVisitor<>(null, dateFormat, transformationFunctions);

        visitor.visit(parser.parse("objectMetadata.lastUpdatedTime==2017-07-04T07:07:07.235-0700"));

        final String query = visitor.getQuery();
        assertThat(query, is("objectMetadata.lastUpdatedTime==1499177227235"));
    }

    class EpochalDateTransformationFunction implements FiqlTransformationFunction {
        private final String dateFormat;

        public EpochalDateTransformationFunction(final String dateFormat) {
            this.dateFormat = dateFormat;
        }

        @Override
        public String apply(final String property, final String operation, final String value) {
            try {
                return String.format("%s%s%s", property, operation, new SimpleDateFormat(dateFormat).parse(value).getTime());
            } catch (final Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}
