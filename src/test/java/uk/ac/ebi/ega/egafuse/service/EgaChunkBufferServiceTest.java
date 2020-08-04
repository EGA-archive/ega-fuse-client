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

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;

import jnr.ffi.Pointer;
import uk.ac.ebi.ega.egafuse.config.EgaFuseApplicationConfig;
import uk.ac.ebi.ega.egafuse.model.CacheKey;

@TestPropertySource("classpath:application-test.properties")
@ContextConfiguration(classes = EgaFuseApplicationConfig.class)
@RunWith(SpringRunner.class)
public class EgaChunkBufferServiceTest {
    private IEgaChunkBufferService bufferService;

    @Value("${api.chunksize}")
    private long CHUNK_SIZE;

    @Value("${connectionPerFile}")
    private int PREFETCH;
    
    @Value("${tree}")
    private boolean isTreeStructureEnable;

    @Mock
    private AsyncLoadingCache<CacheKey, byte[]> cache;

    @Mock
    private Pointer pointer;

    @Before
    public void before() {
        bufferService = new EgaChunkBufferService(CHUNK_SIZE, PREFETCH, cache);
    }

    @Test
    public void fillBuffer_WhenGivenFile_ThenReturnsChunkSize() throws JsonProcessingException, InterruptedException, ExecutionException {
        CompletableFuture<byte[]> future = mock(CompletableFuture.class);
        when(cache.get(any())).thenReturn(future);
        when(future.get()).thenReturn(new byte[] {});
        long bytesToRead = 10l;
        int chunksize = bufferService.fillBuffer(pointer, "fileId", 100l, bytesToRead, 0l);
        assertEquals(bytesToRead, chunksize);
    }

    @Test
    public void fillBuffer_WhenGivenNoFile_ThenReturnsNegativeChunkSize()
            throws JsonProcessingException, InterruptedException, ExecutionException {
        CompletableFuture<byte[]> future = mock(CompletableFuture.class);
        when(cache.get(any())).thenReturn(future);
        when(future.get()).thenReturn(null);
        int chunksize = bufferService.fillBuffer(pointer, "fileId", 100l, 10l, 0l);
        assertEquals(-1, chunksize);
    }

    @Test
    public void fillBuffer_WhenGivenExcpetion_ThenReturnsNegativeChunkSize()
            throws JsonProcessingException, InterruptedException, ExecutionException {
        CompletableFuture<byte[]> future = mock(CompletableFuture.class);
        when(cache.get(any())).thenReturn(future);
        when(future.get()).thenThrow(InterruptedException.class);
        int chunksize = bufferService.fillBuffer(pointer, "fileId", 100l, 10l, 0l);
        assertEquals(-1, chunksize);
    }
}
