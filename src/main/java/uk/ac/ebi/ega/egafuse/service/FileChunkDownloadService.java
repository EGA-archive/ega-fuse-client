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

import java.io.DataInputStream;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.web.util.UriComponentsBuilder;

import com.google.common.io.CountingInputStream;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import uk.ac.ebi.ega.egafuse.exception.ClientProtocolException;
import uk.ac.ebi.ega.egafuse.model.CacheKey;

public class FileChunkDownloadService implements IFileChunkDownloadService {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileChunkDownloadService.class);
    private OkHttpClient okHttpClient;
    private String apiURL;
    private Token token;

    public FileChunkDownloadService(OkHttpClient okHttpClient, String apiURL, Token token) {
        this.okHttpClient = okHttpClient;
        this.apiURL = apiURL;
        this.token = token;
    }

    @Retryable(value = {IOException.class, ClientProtocolException.class}, maxAttemptsExpression = "${connection.maxAttempts}", 
            backoff = @Backoff(delayExpression = "${connection.backoff}"))
    public byte[] downloadChunk(CacheKey cacheKey) throws IOException, ClientProtocolException {
        long startCoordinate = cacheKey.getStartCoordinate();
        long bytesToRead = cacheKey.getChunkBytesToRead();
       
        UriComponentsBuilder builder =  UriComponentsBuilder.fromPath(apiURL
                                                            .concat("/files/"))
                                                            .path(cacheKey.getFileId())
                                                            .queryParam("destinationFormat", "plain")
                                                            .queryParam("startCoordinate", startCoordinate)
                                                            .queryParam("endCoordinate", (startCoordinate + bytesToRead));

        LOGGER.info("url = " + builder.toUriString());

        Request fileRequest;
        try {
            fileRequest = new Request.Builder().url(builder.toUriString()).addHeader("Authorization",
                    "Bearer " + token.getBearerToken())
                    .build();
            try (Response response = okHttpClient.newCall(fileRequest).execute()) {
                return buildResponseDownloadFiles(response, bytesToRead);
            } catch (IOException e) {
                throw new IOException("Unable to execute request. Can be retried.", e);
            } catch (ClientProtocolException e) {
                throw new ClientProtocolException(e.toString());
            }
        } catch (IOException e) {
            LOGGER.error("Error in downloading file - {}", e.getMessage(), e);
            throw new IOException("Unable to execute request. Can be retried.", e);
        }
    }

    private byte[] buildResponseDownloadFiles(final Response response, long bytesToRead)
            throws IOException, ClientProtocolException {
        final int status = response.code();
        switch (status) {
        case 200:
        case 206:
            byte[] buffer = new byte[(int) bytesToRead];
            try (DataInputStream dis = new DataInputStream(response.body().byteStream())) {
                dis.readFully(buffer);
            }
            return buffer;
        default:
            LOGGER.error("status: {}", status);
            throw new ClientProtocolException(response.body().string());
        }
    }
}
