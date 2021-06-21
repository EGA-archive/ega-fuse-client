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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import uk.ac.ebi.ega.egafuse.exception.ClientProtocolException;

public class EgaDatasetService implements IEgaDatasetService {
    private static final Logger LOGGER = LoggerFactory.getLogger(EgaDatasetService.class);
    private IEgaFileService egaFileService;
    private OkHttpClient okHttpClient;
    private String apiURL;
    private Token token;
    private ObjectMapper mapper;

    public EgaDatasetService(OkHttpClient okHttpClient, String apiURL, Token token, IEgaFileService egaFileService) {
        this.okHttpClient = okHttpClient;
        this.apiURL = apiURL;
        this.token = token;
        this.egaFileService = egaFileService;
        this.mapper = new ObjectMapper();
    }

    @Override
    public List<EgaDirectory> getDatasets() {
        try {
            Request datasetRequest = new Request.Builder().url(apiURL + "/metadata/datasets")
                    .addHeader("Authorization", "Bearer " + token.getBearerToken()).build();

            try (Response response = okHttpClient.newCall(datasetRequest).execute()) {
                return buildResponseGetDataset(response);
            } catch (IOException e) {
                throw new IOException("Unable to execute request. Can't be retried.", e);
            } catch (ClientProtocolException e) {
                throw e;
            }
        } catch (Exception e) {
            LOGGER.error("Error in get dataset - {}", e.getMessage());
        }
        return Collections.emptyList();
    }

    private List<EgaDirectory> buildResponseGetDataset(final Response response)
            throws IOException, ClientProtocolException {
        final int status = response.code();
        switch (status) {
        case 200:
            List<String> datasets = mapper.readValue(response.body().string(), new TypeReference<List<String>>() {
            });
            List<EgaDirectory> egaDirectorys = new ArrayList<>();
            for (String dataset : datasets) {
                EgaDirectory egaDirectory = new EgaDirectory(dataset, this, egaFileService);
                egaDirectorys.add(egaDirectory);
            }
            return egaDirectorys;
        default:
            LOGGER.error("status: {}", status);
            throw new ClientProtocolException(response.body().string());
        }
    }

    @Override
    public void buildSubDirectoryFromFilePath(List<EgaFile> egaFiles, EgaDirectory datasetRootNode) {
        Map<String, EgaDirectory> filePathDirectory = new HashMap<>();

        for (EgaFile egaFile : egaFiles) {
            EgaDirectory currentDirectory = null;
            String filePath = recreateFilePath(egaFile.getFile().getDisplayFilePath().trim());
                    
            // if file does not have any directory
            if (filePath.endsWith(".cip")) {
                filePathDirectory.put(filePath, datasetRootNode);
                datasetRootNode.add(egaFile);
            }
            // if directory already created
            else if (filePathDirectory.containsKey(filePath)) {
                currentDirectory = filePathDirectory.get(filePath);
                currentDirectory.add(egaFile);
            }
            // if directory doesn't exists
            else {
                createSubDirectory(filePath, filePathDirectory, currentDirectory, datasetRootNode, egaFile);
            }
        }
    }
    
    private String recreateFilePath(String filePath) {
        String pathSoFar = "";
        for(String subPath: filePath.split("/")) {
            if(!subPath.trim().isEmpty()) {
                pathSoFar = pathSoFar.isEmpty() ? subPath.trim() : pathSoFar.concat("/").concat(subPath.trim());
            }            
        }
        return pathSoFar;
    }

    private void createSubDirectory(String filePath, Map<String, EgaDirectory> filePathDirectory,
            EgaDirectory currentDirectory, EgaDirectory datasetRootNode, EgaFile egaFile) {
        String subPaths[] = filePath.split("/");
        String pathSoFar = "";
        EgaDirectory subPathDirectory = null;

        // create directory
        for (String subPath : subPaths) {
            pathSoFar = pathSoFar.isEmpty() ? subPath.trim() : pathSoFar.concat("/").concat(subPath.trim());

            if (filePathDirectory.get(pathSoFar) == null) {
                subPathDirectory = new EgaDirectory(subPath, this, egaFileService);

                // create first child of datasetRootNode
                if (currentDirectory == null) {
                    datasetRootNode.add(subPathDirectory);
                    filePathDirectory.put(pathSoFar, subPathDirectory);
                } else {
                    currentDirectory.add(subPathDirectory);
                    filePathDirectory.put(pathSoFar, subPathDirectory);
                }
            } else {
                subPathDirectory = filePathDirectory.get(pathSoFar);
            }
            currentDirectory = subPathDirectory;
        }

        // add file to current directory
        currentDirectory.add(egaFile);
    }

}
