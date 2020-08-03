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

import com.google.common.base.Objects;

public class CacheKey {
    private final long startCoordinate;
    private final long chunkBytesToRead;
    private final String fileId;

    public CacheKey(long startCoordinate, long chunkBytesToRead, String fileId) {
        this.startCoordinate = startCoordinate;
        this.chunkBytesToRead = chunkBytesToRead;
        this.fileId = fileId;
    }

    public long getStartCoordinate() {
        return startCoordinate;
    }

    public long getChunkBytesToRead() {
        return chunkBytesToRead;
    }

    public String getFileId() {
        return fileId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        CacheKey cacheKey = (CacheKey) o;
        return startCoordinate == cacheKey.startCoordinate && chunkBytesToRead == cacheKey.chunkBytesToRead
                && Objects.equal(fileId, cacheKey.fileId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(startCoordinate, chunkBytesToRead, fileId);
    }

    @Override
    public String toString() {
        return "CacheKey{" + "startCoordinate=" + startCoordinate + ", chunkBytesToRead=" + chunkBytesToRead
                + ", fileId='" + fileId + '\'' + '}';
    }
}
