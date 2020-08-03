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
package uk.ac.ebi.ega.egafuse.service;

import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;

import jnr.ffi.Pointer;
import uk.ac.ebi.ega.egafuse.model.CacheKey;

public class EgaChunkBufferService implements IEgaChunkBufferService {
    private static final Logger LOGGER = LoggerFactory.getLogger(EgaChunkBufferService.class);
    private int cachePrefetch;
    private long chunkSize;
    private AsyncLoadingCache<CacheKey, byte[]> cache;

    public EgaChunkBufferService(long chunkSize, int cachePrefetch, AsyncLoadingCache<CacheKey, byte[]> cache) {
        this.chunkSize = chunkSize;
        this.cachePrefetch = cachePrefetch;
        this.cache = cache;
    }

    @Override
    public int fillBuffer(Pointer buffer, String fileId, long fileSize, long bytesToRead, long offset) {
        int minBytesToRead = (int) Math.min(fileSize - offset, bytesToRead);
        int chunkIndex = (int) (offset / chunkSize);

        if (offset >= fileSize || minBytesToRead <= 0)
            return -1;

        prefetchChunk(fileId, chunkIndex, fileSize);

        try {
            byte[] chunk = cache.get(getCacheKey(fileId, chunkIndex, fileSize)).get();
            if (chunk != null) {
                int chunkOffset = (int) (offset - chunkIndex * chunkSize);
                buffer.put(0L, chunk, chunkOffset, minBytesToRead);
                return minBytesToRead;
            }
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("Chunk {} could not be retrieved for file {} bytesToRead {} offset {} ", chunkIndex, fileId,
                    bytesToRead, offset);
            LOGGER.error("Error in reading from cache - {} ", e.getMessage(), e);
        }
        return -1;
    }

    private void prefetchChunk(String fileId, int chunkIndex, long fileSize) {
        int maxChunk = (int) (fileSize / chunkSize);
        int endChunk = Math.min(chunkIndex + cachePrefetch, maxChunk);

        while (chunkIndex <= endChunk) {
            cache.get(getCacheKey(fileId, chunkIndex++, fileSize));
        }
    }

    private CacheKey getCacheKey(String fileId, int chunkIndex, long fileSize) {
        long startCoordinate = chunkIndex * chunkSize;
        long chunkBytesToRead = ((startCoordinate + chunkSize) > fileSize) ? (fileSize - startCoordinate) : chunkSize;
        return new CacheKey(startCoordinate, chunkBytesToRead, fileId);
    }
}
