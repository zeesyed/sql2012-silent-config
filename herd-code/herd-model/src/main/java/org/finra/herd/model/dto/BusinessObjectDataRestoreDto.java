/*
* Copyright 2015 herd contributors
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
package org.finra.herd.model.dto;

import java.util.List;

import org.finra.herd.model.api.xml.BusinessObjectDataKey;
import org.finra.herd.model.api.xml.StorageFile;

/**
 * A DTO that holds various parameters needed to perform a business object data restore.
 */
public class BusinessObjectDataRestoreDto
{
    /**
     * The business object data key.
     */
    private BusinessObjectDataKey businessObjectDataKey;

    /**
     * This field points to an exception that could be thrown when executing the initiate a business object data restore request.
     */
    private Exception exception;

    /**
     * The new status of the storage unit.
     */
    private String newStorageUnitStatus;

    /**
     * The old status of the storage unit.
     */
    private String oldStorageUnitStatus;

    /**
     * The S3 bucket name.
     */
    private String s3BucketName;

    /**
     * The optional S3 endpoint to use when making S3 service calls.
     */
    private String s3Endpoint;

    /**
     * The S3 key prefix.
     */
    private String s3KeyPrefix;

    /**
     * The storage files.
     */
    private List<StorageFile> storageFiles;

    /**
     * The storage name.
     */
    private String storageName;

    /**
     * Default no-arg constructor.
     */
    public BusinessObjectDataRestoreDto()
    {
        // This is intentionally empty, nothing needed here.
    }

    /**
     * Fully-initialising value constructor.
     *
     * @param businessObjectDataKey the business object data key
     * @param storageName the origin storage name
     * @param s3Endpoint the optional S3 endpoint to use when making S3 service calls
     * @param s3BucketName the origin S3 bucket name
     * @param s3KeyPrefix the origin S3 key prefix
     * @param newStorageUnitStatus the new status of the origin storage unit
     * @param oldStorageUnitStatus the old (previous) status of the origin storage unit
     * @param storageFiles the list of origin storage files
     * @param exception the exception
     */
    public BusinessObjectDataRestoreDto(final BusinessObjectDataKey businessObjectDataKey, final String storageName, final String s3Endpoint,
        final String s3BucketName, final String s3KeyPrefix, final String newStorageUnitStatus, final String oldStorageUnitStatus,
        final List<StorageFile> storageFiles, final Exception exception)
    {
        this.businessObjectDataKey = businessObjectDataKey;
        this.storageName = storageName;
        this.s3Endpoint = s3Endpoint;
        this.s3BucketName = s3BucketName;
        this.s3KeyPrefix = s3KeyPrefix;
        this.newStorageUnitStatus = newStorageUnitStatus;
        this.oldStorageUnitStatus = oldStorageUnitStatus;
        this.storageFiles = storageFiles;
        this.exception = exception;
    }

    @Override
    @SuppressWarnings("PMD.CyclomaticComplexity") // Method is not complex. It's just very repetitive.
    public boolean equals(Object object)
    {
        if (this == object)
        {
            return true;
        }
        if (!(object instanceof BusinessObjectDataRestoreDto))
        {
            return false;
        }

        BusinessObjectDataRestoreDto that = (BusinessObjectDataRestoreDto) object;

        if (businessObjectDataKey != null ? !businessObjectDataKey.equals(that.businessObjectDataKey) : that.businessObjectDataKey != null)
        {
            return false;
        }
        if (exception != null ? !exception.equals(that.exception) : that.exception != null)
        {
            return false;
        }
        if (newStorageUnitStatus != null ? !newStorageUnitStatus.equals(that.newStorageUnitStatus) : that.newStorageUnitStatus != null)
        {
            return false;
        }
        if (oldStorageUnitStatus != null ? !oldStorageUnitStatus.equals(that.oldStorageUnitStatus) : that.oldStorageUnitStatus != null)
        {
            return false;
        }
        if (s3BucketName != null ? !s3BucketName.equals(that.s3BucketName) : that.s3BucketName != null)
        {
            return false;
        }
        if (s3Endpoint != null ? !s3Endpoint.equals(that.s3Endpoint) : that.s3Endpoint != null)
        {
            return false;
        }
        if (s3KeyPrefix != null ? !s3KeyPrefix.equals(that.s3KeyPrefix) : that.s3KeyPrefix != null)
        {
            return false;
        }
        if (storageFiles != null ? !storageFiles.equals(that.storageFiles) : that.storageFiles != null)
        {
            return false;
        }
        if (storageName != null ? !storageName.equals(that.storageName) : that.storageName != null)
        {
            return false;
        }

        return true;
    }

    public BusinessObjectDataKey getBusinessObjectDataKey()
    {
        return businessObjectDataKey;
    }

    public void setBusinessObjectDataKey(BusinessObjectDataKey businessObjectDataKey)
    {
        this.businessObjectDataKey = businessObjectDataKey;
    }

    public Exception getException()
    {
        return exception;
    }

    public void setException(Exception exception)
    {
        this.exception = exception;
    }

    public String getNewStorageUnitStatus()
    {
        return newStorageUnitStatus;
    }

    public void setNewStorageUnitStatus(String newStorageUnitStatus)
    {
        this.newStorageUnitStatus = newStorageUnitStatus;
    }

    public String getOldStorageUnitStatus()
    {
        return oldStorageUnitStatus;
    }

    public void setOldStorageUnitStatus(String oldStorageUnitStatus)
    {
        this.oldStorageUnitStatus = oldStorageUnitStatus;
    }

    public String getS3BucketName()
    {
        return s3BucketName;
    }

    public void setS3BucketName(String s3BucketName)
    {
        this.s3BucketName = s3BucketName;
    }

    public String getS3Endpoint()
    {
        return s3Endpoint;
    }

    public void setS3Endpoint(String s3Endpoint)
    {
        this.s3Endpoint = s3Endpoint;
    }

    public String getS3KeyPrefix()
    {
        return s3KeyPrefix;
    }

    public void setS3KeyPrefix(String s3KeyPrefix)
    {
        this.s3KeyPrefix = s3KeyPrefix;
    }

    public List<StorageFile> getStorageFiles()
    {
        return storageFiles;
    }

    public void setStorageFiles(List<StorageFile> storageFiles)
    {
        this.storageFiles = storageFiles;
    }

    public String getStorageName()
    {
        return storageName;
    }

    public void setStorageName(String storageName)
    {
        this.storageName = storageName;
    }

    @Override
    public int hashCode()
    {
        int result = businessObjectDataKey != null ? businessObjectDataKey.hashCode() : 0;
        result = 31 * result + (exception != null ? exception.hashCode() : 0);
        result = 31 * result + (newStorageUnitStatus != null ? newStorageUnitStatus.hashCode() : 0);
        result = 31 * result + (oldStorageUnitStatus != null ? oldStorageUnitStatus.hashCode() : 0);
        result = 31 * result + (s3Endpoint != null ? s3Endpoint.hashCode() : 0);
        result = 31 * result + (s3BucketName != null ? s3BucketName.hashCode() : 0);
        result = 31 * result + (s3KeyPrefix != null ? s3KeyPrefix.hashCode() : 0);
        result = 31 * result + (storageFiles != null ? storageFiles.hashCode() : 0);
        result = 31 * result + (storageName != null ? storageName.hashCode() : 0);
        return result;
    }
}
