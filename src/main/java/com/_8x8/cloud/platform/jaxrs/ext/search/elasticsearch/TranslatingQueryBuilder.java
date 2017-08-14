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

import org.apache.cxf.jaxrs.ext.search.SearchUtils;
import org.apache.cxf.jaxrs.ext.search.fiql.FiqlParser;

import java.util.HashMap;
import java.util.Map;

/**
 * Provides a class to make using {@link TranslatingQueryBuilderVisitor}s less cumbersome to use.
 */
public class TranslatingQueryBuilder<T> {
    private final FiqlParser<T> fiqlParser;
    private final Map<String, String> fieldMap;
    private final String dateFormat;
    private final Map<String, FiqlTransformationFunction> transformationFunctions;

    FiqlParser<T> getFiqlParser() {
        return fiqlParser;
    }

    Map<String, String> getFieldMap() {
        return fieldMap;
    }

    String getDateFormat() {
        return dateFormat;
    }

    Map<String, FiqlTransformationFunction> getTransformationFunctions() {
        return transformationFunctions;
    }

    /**
     * Constructs a query builder that allows for easy translation of FIQL expressions.
     * <p>
     * Please see the {@link TranslatingQueryBuilderVisitor} constructor Javadocs for details.
     *
     * @param clazz                   The model class to use when translating expressions.
     * @param fieldMap                An optional mapping of field names in the format of {@code A -> B}, such that
     *                                fields of name {@code A} will be translated to name {@code B}.
     * @param dateFormat              An optional date format, compatible with {@link java.text.DateFormat} instances,
     *                                for parsing {@link java.util.Date} entities within translated expressions.
     * @param transformationFunctions An optional mapping of post-translation field names to
     *                                {@link FiqlTransformationFunction} for custom fine-grained, field level transformations.
     * @see TranslatingQueryBuilderVisitor
     */
    public TranslatingQueryBuilder(final Class<T> clazz, final Map<String, String> fieldMap, final String dateFormat,
                                   final Map<String, FiqlTransformationFunction> transformationFunctions) {
        final Map<String, String> parserConfiguration = new HashMap<>();

        // Make sure to propagate the custom date format, if any, so we can have consistent date handling and we don't
        // wind up losing part of our precision on a round-trip.
        if (null != dateFormat) {
            parserConfiguration.put(SearchUtils.DATE_FORMAT_PROPERTY, dateFormat);
        }

        this.fiqlParser = new FiqlParser<>(clazz, parserConfiguration);
        this.fieldMap = fieldMap;
        this.dateFormat = dateFormat;
        this.transformationFunctions = transformationFunctions;
    }

    /**
     * Translates a FIQL query string based on the type assigned to the given {@link TranslatingQueryBuilder}.
     * The output of the translation will be a syntactically valid FIQL query string.
     *
     * @param filter The FIQL query filter to translate to another form.
     * @return A translated FIQL query filter, matching the mappings and translations specified in the constructor.
     */
    public String translateFiqlQuery(final String filter) {
        final TranslatingQueryBuilderVisitor<T> visitor = createVisitor(getFieldMap(), getDateFormat(), getTransformationFunctions());
        visitor.visit(getFiqlParser().parse(filter));

        return visitor.getQuery();
    }

    /**
     * Provides a test-friendly method for creating instances of {@link TranslatingQueryBuilderVisitor} to translate
     * FIQL expressions.
     *
     * @return A non-null, valid, and fully primed {@link TranslatingQueryBuilderVisitor}.
     */
    TranslatingQueryBuilderVisitor<T> createVisitor(final Map<String, String> fieldMap, final String dateFormat,
                                                    final Map<String, FiqlTransformationFunction> transformationFunctions) {
        return new TranslatingQueryBuilderVisitor<>(fieldMap, dateFormat, transformationFunctions);
    }
}
