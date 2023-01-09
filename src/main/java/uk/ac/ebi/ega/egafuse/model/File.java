/*
 *
 * Copyright 2020 EMBL - European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package uk.ac.ebi.ega.egafuse.model;

import java.util.List;

public class File {
    private String fileId;
    private List<String> datasetIds;
    private String displayFileName;
    private String fileName;
    private String displayFilePath;
    private String fileStatus;
    private String plainChecksum;
    private String plainChecksumType;
    private long fileSize;

    private String indexFileId;

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public List<String> getDatasetIds() {
        return datasetIds;
    }

    public void setDatasetId(List<String> datasetIds) {
        this.datasetIds = datasetIds;
    }

    public String getDisplayFileName() {
        return displayFileName;
    }

    public void setDisplayFileName(String displayFileName) {
        this.displayFileName = displayFileName;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getDisplayFilePath() {
        return displayFilePath;
    }

    public void setDisplayFilePath(String displayFilePath) {
        this.displayFilePath = displayFilePath;
    }

    public String getFileStatus() {
        return fileStatus;
    }

    public void setFileStatus(String fileStatus) {
        this.fileStatus = fileStatus;
    }

    public String getPlainChecksum() {
        return plainChecksum;
    }

    public void setPlainChecksum(String plainChecksum) {
        this.plainChecksum = plainChecksum;
    }

    public String getPlainChecksumType() {
        return plainChecksumType;
    }

    public void setPlainChecksumType(String plainChecksumType) {
        this.plainChecksumType = plainChecksumType;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getIndexFileId() {
        return indexFileId;
    }

    public void setIndexFileId(String indexFileId) {
        this.indexFileId = indexFileId;
    }

    @Override
    public String toString() {
        return "File [fileId=" + fileId + ", datasetIds=" + datasetIds.toString() + ", displayFileName=" + displayFileName
                + ", fileName=" + fileName + ", displayFilePath=" + displayFilePath + ", fileStatus=" + fileStatus + ", unencryptedChecksum="
                + plainChecksum + ", unencryptedChecksumType=" + plainChecksumType + ", fileSize=" + fileSize
                + ", indexFileId=" + indexFileId
                + "]";
    }
}
