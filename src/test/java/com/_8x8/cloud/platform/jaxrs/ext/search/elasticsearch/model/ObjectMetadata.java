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
