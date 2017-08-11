/*
 * Copyright (c) 2017 by 8x8, Inc. All rights reserved.
 *
 * This software is the confidential and proprietary information
 * of 8x8, Inc. ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you
 * entered into with 8x8, Inc.
 */

package com._8x8.cloud.platform.jaxrs.ext.search.elasticsearch.model;

import com._8x8.cloud.platform.jaxrs.ext.search.elasticsearch.TranslatingQueryBuilderVisitor;

/**
 * Provides another test model we can use for testing our {@link TranslatingQueryBuilderVisitor}.
 */
@SuppressWarnings("unused")
public class OtherMetadata {
    private String foo;
    private Long bar;

    public String getFoo() {
        return foo;
    }

    public void setFoo(final String foo) {
        this.foo = foo;
    }

    public Long getBar() {
        return bar;
    }

    public void setBar(final Long bar) {
        this.bar = bar;
    }
}
