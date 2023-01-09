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

import okhttp3.OkHttpClient;
import okhttp3.mock.MockInterceptor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.ega.egafuse.config.EgaFuseApplicationConfig;
import uk.ac.ebi.ega.egafuse.exception.ClientProtocolException;
import uk.ac.ebi.ega.egafuse.model.CacheKey;

import java.io.IOException;

import static okhttp3.mock.Behavior.UNORDERED;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

@TestPropertySource("classpath:application-test.properties")
@ContextConfiguration(classes = EgaFuseApplicationConfig.class)
@RunWith(SpringRunner.class)
public class FileChunkDownloadServiceTest {
    private IFileChunkDownloadService fileChunkDownloadService;
    private MockInterceptor interceptor;
    private OkHttpClient client;

    @Value("${app.url}")
    private String APP_URL;

    @Mock
    private Token token;

    @Before
    public void before() {
        interceptor = new MockInterceptor(UNORDERED);
        client = new OkHttpClient.Builder().addInterceptor(interceptor).build();
        fileChunkDownloadService = new FileChunkDownloadService(client, APP_URL, token);
    }

    @Test
    public void downloadChunk_WhenGivenCacheKey_ThenReturnsFileBytes() throws ClientProtocolException, IOException {
        byte[] file = "testfiledata".getBytes();
        CacheKey cacheKey = new CacheKey(0, file.length, "EGAF00001");
        String url = getUrl(cacheKey);
        String header = getRangeHeader(cacheKey);
        interceptor.addRule().get(url).header("Range", header).respond(file);

        byte[] chunk = fileChunkDownloadService.downloadChunk(cacheKey);

        assertArrayEquals(file, chunk);
    }

    @Test(expected = ClientProtocolException.class)
    public void downloadChunk_WhenGivenExceptionByAppUrl_ThenThrowsException() throws IOException, ClientProtocolException {
        CacheKey cacheKey = new CacheKey(0, 12, "EGAF00001");
        String url = getUrl(cacheKey);
        String rangeHeader = getRangeHeader(cacheKey);
        interceptor.addRule().get(url).header("Range", rangeHeader).respond(500);

        fileChunkDownloadService.downloadChunk(cacheKey);
    }

    private String getUrl(CacheKey cacheKey) {
        return APP_URL.trim() + "/files/" + cacheKey.getFileId() + "?destinationFormat=plain";
    }

    private static String getRangeHeader(CacheKey cacheKey) {
        long startCoordinate = cacheKey.getStartCoordinate();
        long endCoordinate = startCoordinate + cacheKey.getChunkBytesToRead();
        String header = "bytes= " + startCoordinate + "-" + (endCoordinate - 1);
        return header;
    }
}
