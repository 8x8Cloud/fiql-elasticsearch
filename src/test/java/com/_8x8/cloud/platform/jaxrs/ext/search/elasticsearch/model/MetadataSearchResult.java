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
public class MetadataSearchResult {
    private ObjectMetadata objectMetadata;
    private OtherMetadata otherMetadata;

    public ObjectMetadata getObjectMetadata() {
        return objectMetadata;
    }

    public void setObjectMetadata(final ObjectMetadata objectMetadata) {
        this.objectMetadata = objectMetadata;
    }

    public OtherMetadata getOtherMetadata() {
        return otherMetadata;
    }

    public void setOtherMetadata(final OtherMetadata otherMetadata) {
        this.otherMetadata = otherMetadata;
    }
}
