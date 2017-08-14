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
