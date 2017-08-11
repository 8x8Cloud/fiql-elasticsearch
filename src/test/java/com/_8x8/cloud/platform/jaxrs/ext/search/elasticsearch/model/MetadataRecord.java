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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Provides a sample class for testing our expressions. This is a subset of the indexer model.
 */
@SuppressWarnings("unused")
public class MetadataRecord {
    private String tenantName;
    private String containerName;
    private Long containerId;
    private Long storedBytes;
    private Date updatedTime;
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

    public Long getStoredBytes() {
        return storedBytes;
    }

    public void setStoredBytes(final Long storedBytes) {
        this.storedBytes = storedBytes;
    }

    public Date getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(final Date updatedTime) {
        this.updatedTime = updatedTime;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(final Status status) {
        this.status = status;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(final List<String> tags) {
        this.tags = tags;
    }
}