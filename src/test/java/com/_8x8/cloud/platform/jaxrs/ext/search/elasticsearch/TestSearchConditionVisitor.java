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

import org.apache.cxf.jaxrs.ext.search.SearchCondition;
import org.apache.cxf.jaxrs.ext.search.collections.CollectionCheckInfo;
import org.apache.cxf.jaxrs.ext.search.visitor.AbstractSearchConditionVisitor;

import java.util.Set;

/**
 * The {@link AbstractSearchConditionVisitor.ClassValue} class that we need access to is unfortunately protected. Let's
 * cheat our way around this...
 */
public class TestSearchConditionVisitor extends AbstractSearchConditionVisitor<Object, Object> {
    protected TestSearchConditionVisitor() {
        super(null);
    }

    @Override
    public void visit(final SearchCondition<Object> sc) {

    }

    @Override
    public Object getQuery() {
        return null;
    }

    @SuppressWarnings("unchecked")
    public class ClassValue extends AbstractSearchConditionVisitor.ClassValue {
        public ClassValue(final Class<?> this$0, final Object cls, final CollectionCheckInfo value, final Set<String> collInfo) {
            super(this$0, cls, value, collInfo);
        }

        @Override
        public CollectionCheckInfo getCollectionCheckInfo() {
            return super.getCollectionCheckInfo();
        }

        @Override
        public Object getValue() {
            return super.getValue();
        }

        @Override
        public Class<?> getCls() {
            return super.getCls();
        }
    }
}