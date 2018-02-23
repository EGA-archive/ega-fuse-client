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

import eu.elixir.ega.ebi.egafuse.EgaFuse;
import eu.elixir.ega.ebi.egafuse.SSLUtilities;
import eu.elixir.ega.ebi.egafuse.dto.EgaFileDto;
import eu.elixir.ega.ebi.egafuse.filesystems.EgaApiDirectory;
import eu.elixir.ega.ebi.egafuse.filesystems.EgaApiPath;
import jnr.ffi.Pointer;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import ru.serce.jnrfuse.FuseFillDir;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

/**
 * @author asenf
 */
public class EgaNodeDirectory extends EgaApiDirectory {

    private String orgName; // Username - mapped to File ReEncryption Format

    public EgaNodeDirectory(String name, EgaApiDirectory parent) {
        super(name, parent);
        orgName = parent.getName();
    }

    @Override
    public synchronized void read(Pointer buf, FuseFillDir filler) {
        if (contents == null || contents.size() == 0) {
            String org = EgaFuse.getOrg(name);
            if (org.toLowerCase().contains("publicgpg_")) {
                getDatasets(org);
            } else {
                getFiles();
            }
        }

        for (EgaApiPath p : contents) {
            filler.apply(buf, p.getName(), null, 0);
        }
    }

    /*
     * Obtain list of Datasets; 'Orgs' may access all datasets (any restrictions happen in Access)
     */
    private void getDatasets(String org) {
        String plainOrg = org.substring(10);
        String urlOrg = plainOrg;
        try {
            urlOrg = URLEncoder.encode(plainOrg, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
        }
        System.out.println("urlOrg: " + urlOrg);

        OkHttpClient client = SSLUtilities.getUnsafeOkHttpClient();

        // List all Datasets
        Request datasetRequest = new Request.Builder()
                .url(getCentralUrl() + "/app/datasets/" + urlOrg)
                .addHeader("Authorization", "Basic " + getBasicCode())
                .build();

        try {
            // Execute the request and retrieve the response.
            Response response = null;
            int tryCount = 9;
            while (tryCount-- > 0 && (response == null || !response.isSuccessful())) {
                try {
                    response = client.newCall(datasetRequest).execute();
                } catch (Exception ex) {
                }
            }
            ResponseBody body = response.body();
            List<String> datasets = STRING_JSON_ADAPTER.fromJson(body.source());
            body.close();
            System.out.println(datasets.size() + " datasets found.");

            for (String dataset : datasets) {
                EgaNodeDirectory egaNodeDirectory = new EgaNodeDirectory(dataset, this);
                contents.add(egaNodeDirectory);
            }
        } catch (IOException ex) {
            System.out.println("Error getting Datasets [EgaNodeDirectory]: " + ex.toString());
        }

    }

    private void getFiles() {
        String datasetId = name;
        if (datasetId.endsWith("/")) {
            datasetId = datasetId.substring(0, datasetId.length() - 1);
        }

        OkHttpClient client = SSLUtilities.getUnsafeOkHttpClient();

        Request fileRequest = new Request.Builder()
                .url(getCentralUrl() + "/app/datasets/" + datasetId + "/files")
                .addHeader("Authorization", "Basic " + getBasicCode())
                .build();

        try {
            // Add List all Files for this Dataset
            Response fileResponse = null;
            int tryCount = 9;
            while (tryCount-- > 0 && (fileResponse == null || !fileResponse.isSuccessful())) {
                try {
                    fileResponse = client.newCall(fileRequest).execute();
                } catch (Exception ex) {
                }
            }
            ResponseBody fileBody = fileResponse.body();

            List<EgaFileDto> files = FILE_JSON_ADAPTER.fromJson(fileBody.source());
            fileBody.close();

            for (EgaFileDto file : files) {
                String filename = file.getFileName();
                if (filename.contains("/")) {
                    filename = filename.substring(filename.lastIndexOf("/") + 1);
                }
                String fileUrl = getBaseUrl() + "/files/" + file.getFileId();
                EgaNodeFile newFile = new EgaNodeFile(filename, this, file);
                contents.add(newFile);
            }
        } catch (IOException ex) {
            System.out.println("Error getting Files [EgaNodeDirectory]: " + ex.toString());
        }

    }

    public String getOrgName() {
        return orgName;
    }
}
