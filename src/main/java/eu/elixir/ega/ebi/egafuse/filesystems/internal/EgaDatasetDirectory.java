/*
 * Copyright 2016 ELIXIR EGA
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
 */
package eu.elixir.ega.ebi.egafuse.filesystems.internal;

import eu.elixir.ega.ebi.egafuse.SSLUtilities;
import eu.elixir.ega.ebi.egafuse.dto.EgaFileDto;
import eu.elixir.ega.ebi.egafuse.filesystems.EgaApiDirectory;
import eu.elixir.ega.ebi.egafuse.filesystems.EgaApiPath;
import java.io.IOException;
import java.util.List;
import jnr.ffi.Pointer;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import ru.serce.jnrfuse.FuseFillDir;

/**
 *
 * @author asenf
 */
public class EgaDatasetDirectory extends EgaApiDirectory {
    
    public EgaDatasetDirectory(String name, EgaApiDirectory parent) {
        super(name, parent);
    }

    @Override
    public synchronized void read(Pointer buf, FuseFillDir filler) {
        if (contents==null || contents.size() == 0) {
            if (name.equalsIgnoreCase("datasets")) {
                getDatasets();
            } else {
                getFiles();
            }
        }
        
        for (EgaApiPath p : contents) {
            filler.apply(buf, p.getName(), null, 0);
        }
    }

    /*
     * Obtain list of Authorised Datasets from EGA API
     */
    private void getDatasets() {
        OkHttpClient client = SSLUtilities.getUnsafeOkHttpClient();

        // List all Datasets
        Request datasetRequest = new Request.Builder()
            .url(getBaseUrl() + "/metadata/datasets")
            .addHeader("Authorization", "Bearer " + getAccessToken())
            .build();
        
        try {
            // Execute the request and retrieve the response.
            Response response = null;
            int tryCount = 9;
            while (tryCount-->0 && (response == null || !response.isSuccessful())) {
                try {
                    response = client.newCall(datasetRequest).execute();
                } catch (Exception ex) {}
            }
            ResponseBody body = response.body();
            List<String> datasets = STRING_JSON_ADAPTER.fromJson(body.source());
            body.close();
            System.out.println(datasets.size() + " datasets found.");
            
            for (String dataset:datasets) {
                EgaDatasetDirectory egaDatasetDirectory = new EgaDatasetDirectory(dataset, this);
                contents.add(egaDatasetDirectory);
            }            
        } catch (IOException ex) {
            System.out.println("Error getting Datasets [EgaDatasetDirectory]: " + ex.toString());
        }
        
    }

    /*
     * Obtain list of Authorised Files for specified Datasets from EGA API
     */
    private void getFiles() {
        String datasetId = name;
        if (datasetId.endsWith("/")) {
            datasetId = datasetId.substring(0, datasetId.length()-1);
        }
        
        OkHttpClient client = SSLUtilities.getUnsafeOkHttpClient();

        Request fileRequest = new Request.Builder()
            .url(getBaseUrl() + "/metadata/datasets/" + datasetId + "/files")
            .addHeader("Authorization", "Bearer " + getAccessToken())
            .build();
        
        try {
            // Add List all Files for this Dataset
            Response fileResponse = null;
            int tryCount = 9;
            while (tryCount-->0 && (fileResponse == null || !fileResponse.isSuccessful())) {
                try {
                    fileResponse = client.newCall(fileRequest).execute();
                } catch (Exception ex) {}
            }
            ResponseBody fileBody = fileResponse.body();

            List<EgaFileDto> files = FILE_JSON_ADAPTER.fromJson(fileBody.source());
            fileBody.close();            

            for (EgaFileDto file:files) {
                String filename = file.getFileName();
                if (filename.contains("/")) {
                    filename = filename.substring(filename.lastIndexOf("/")+1);
                }
                String fileUrl = getBaseUrl() + "/files/" + file.getFileId();
                EgaRemoteFile newFile = new EgaRemoteFile(filename, this, file);
                contents.add(newFile);
            }
        } catch (IOException ex) {
            System.out.println("Error getting Files [EgaDatasetDirectory]: " + ex.toString());
        }
        
    }
}
