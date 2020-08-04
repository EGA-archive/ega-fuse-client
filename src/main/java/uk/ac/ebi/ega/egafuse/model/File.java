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

public class File {
    private String fileId;
    private String datasetId;
    private String displayFileName;
    private String fileName;
    private String displayFilePath;
    private String fileStatus;
    private String unencryptedChecksum;
    private String unencryptedChecksumType;
    private long fileSize;

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getDatasetId() {
        return datasetId;
    }

    public void setDatasetId(String datasetId) {
        this.datasetId = datasetId;
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

    public String getUnencryptedChecksum() {
        return unencryptedChecksum;
    }

    public void setUnencryptedChecksum(String unencryptedChecksum) {
        this.unencryptedChecksum = unencryptedChecksum;
    }

    public String getUnencryptedChecksumType() {
        return unencryptedChecksumType;
    }

    public void setUnencryptedChecksumType(String unencryptedChecksumType) {
        this.unencryptedChecksumType = unencryptedChecksumType;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    @Override
    public String toString() {
        return "File [fileId=" + fileId + ", datasetId=" + datasetId + ", displayFileName=" + displayFileName
                + ", fileName=" + fileName + ", displayFilePath=" + displayFilePath + ", fileStatus=" + fileStatus + ", unencryptedChecksum="
                + unencryptedChecksum + ", unencryptedChecksumType=" + unencryptedChecksumType + ", fileSize="
                + fileSize + "]";
    }
}
