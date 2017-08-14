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