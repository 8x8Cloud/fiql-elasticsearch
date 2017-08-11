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