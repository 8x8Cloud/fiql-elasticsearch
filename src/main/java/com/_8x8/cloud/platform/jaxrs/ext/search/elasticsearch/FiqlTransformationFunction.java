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

import java.util.Set;
import java.util.function.Function;

/**
 * Represents a three-arity {@link Function} that can be used to transform a FIQL expression. The function will attempt
 * to override the default FIQL translation behavior in {@link TranslatingQueryBuilderVisitor}.
 * <p>
 * This may be handy in certain cases where the output grammar must conform to a different object model, where the
 * transformation is beyond a simple {@code property-operation-value} format string. An example of this, is if you want
 * collapse {@link Set} properties in Elasticsearch, where something like {@code foo.bar} might need to be translated to
 * {@code someField==foo:bar}.
 *
 * @see Function
 */
@FunctionalInterface
public interface FiqlTransformationFunction {
    /**
     * Applies the given transformation, overriding the default behavior of {@link TranslatingQueryBuilderVisitor} for
     * the given field/operation/value.
     *
     * @param property  The "real" property name, translated via the field name map if given.
     * @param operation The translated expression embodying the operation, in string form. This is the translated basic expression.
     * @param value     The value of the expression to use.
     * @return A FIQL-compliant expression representing the given inputs.
     */
    String apply(String property, String operation, String value);
}
