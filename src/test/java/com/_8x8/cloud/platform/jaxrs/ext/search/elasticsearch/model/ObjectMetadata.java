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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Provides another test model we can use for testing our {@link TranslatingQueryBuilderVisitor}.
 */
@SuppressWarnings("unused")
public class ObjectMetadata {
    private String tenantName;
    private String containerName;
    private Long containerId;
    private Long sizeInBytes;
    private Date lastUpdatedTime;
    private Status status;

    private List<String> tags = new ArrayList<>();

    public String getTenantName() {
        return tenantName;
    }

    public void setTenantName(final String tenantName) {
        this.tenantName = tenantName;
    }

    public String getContainerName() {
        return containerName;
    }

    public void setContainerName(final String containerName) {
        this.containerName = containerName;
    }

    public Long getContainerId() {
        return containerId;
    }

    public void setContainerId(final Long containerId) {
        this.containerId = containerId;
    }

    public Long getSizeInBytes() {
        return sizeInBytes;
    }

    public void setSizeInBytes(final Long sizeInBytes) {
        this.sizeInBytes = sizeInBytes;
    }

    public Date getLastUpdatedTime() {
        return lastUpdatedTime;
    }

    public void setLastUpdatedTime(final Date lastUpdatedTime) {
        this.lastUpdatedTime = lastUpdatedTime;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(final Status status) {
        this.status = status;
    }
}
