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

import static okhttp3.mock.Behavior.UNORDERED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static uk.ac.ebi.ega.egafuse.config.EgaFuseApplicationConfig.isTreeStructureEnable;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jnr.ffi.Pointer;
import okhttp3.OkHttpClient;
import okhttp3.mock.MockInterceptor;
import ru.serce.jnrfuse.FuseFillDir;
import uk.ac.ebi.ega.egafuse.config.EgaFuseApplicationConfig;
import uk.ac.ebi.ega.egafuse.model.File;

@TestPropertySource("classpath:application-test.properties")
@ContextConfiguration(classes = EgaFuseApplicationConfig.class)
@RunWith(SpringRunner.class)
public class EgaDirectoryTest {
    private EgaDirectory egaParentdirectory;
    private IEgaFileService egaFileService;
    private MockInterceptor interceptor;
    private OkHttpClient client;
    private ObjectMapper objectMapper;
    private IEgaDatasetService egaDatasetService;

    @Value("${app.url}")
    private String APP_URL;

    @Mock
    private Token token;

    @Mock
    private EgaChunkBufferService bufferService;

    @Mock
    private Pointer pointer;

    @Mock
    private FuseFillDir fuseFillDir;

    @Before
    public void before() {
        objectMapper = new ObjectMapper();
        interceptor = new MockInterceptor(UNORDERED);
        client = new OkHttpClient.Builder().addInterceptor(interceptor).build();
        egaFileService = new EgaFileService(client, APP_URL, token, bufferService);
        egaDatasetService = new EgaDatasetService(client, APP_URL, token, egaFileService);
        egaParentdirectory = new EgaDirectory("directory", egaDatasetService, egaFileService);
    }

    @Test
    public void find_WhenGivenDirectoryName_ThenReturnsEgaPath() {
        EgaPath path = egaParentdirectory.find(egaParentdirectory.getName());
        assertEquals(path.getName(), egaParentdirectory.getName());
    }

    @Test
    public void readDatasets_WhenGivenEgaDirectory_ThenReturnsEgaPath() throws JsonProcessingException {
        EgaDirectory directory = new EgaDirectory("dataset1", egaDatasetService, egaFileService);
        egaParentdirectory.add(directory);
        List<EgaDirectory> egaDirectorys = new ArrayList<>();
        egaDirectorys.add(directory);
        
        interceptor.addRule()
        .get(APP_URL.concat("/metadata/datasets/").concat(egaParentdirectory.getName()).concat("/files"))
        .respond(objectMapper.writeValueAsString(egaDirectorys));
        
        egaParentdirectory.read(pointer, fuseFillDir);

        List<EgaPath> contents = egaParentdirectory.contents;
        assertEquals(egaDirectorys.get(0).getName(), contents.get(0).getName());
    }

    @Test
    public void readFiles_WhenGivenEgaFileAndTreeFalse_ThenReturnsEgaPath() throws JsonProcessingException {
        isTreeStructureEnable = false;

        List<File> files = new ArrayList<>();
        File file = new File();
        file.setFileId("EGAF00001");
        file.setFileName("test1.cip");
        file.setDisplayFileName("test1");
        file.setDisplayFilePath("A/B");
        file.setFileSize(100l);
        files.add(file);

        interceptor.addRule()
                .get(APP_URL.concat("/metadata/datasets/").concat(egaParentdirectory.getName()).concat("/files"))
                .respond(objectMapper.writeValueAsString(files));

        egaParentdirectory.read(pointer, fuseFillDir);

        List<EgaPath> contents = egaParentdirectory.contents;
        assertEquals(file.getDisplayFileName(), contents.get(0).getName());
    }

    @Test
    public void readFiles_WhenGivenEgaFileAndTreeTrueOneFile_ThenReturnsEgaPath() throws JsonProcessingException {
        isTreeStructureEnable = true;

        List<File> files = new ArrayList<>();
        File file = new File();
        file.setFileId("EGAF00001");
        file.setFileName("test1.cip");
        file.setDisplayFileName("test1");
        file.setDisplayFilePath("A/B");
        file.setFileSize(100l);
        files.add(file);

        interceptor.addRule()
                .get(APP_URL.concat("/metadata/datasets/").concat(egaParentdirectory.getName()).concat("/files"))
                .respond(objectMapper.writeValueAsString(files));

        egaParentdirectory.read(pointer, fuseFillDir);

        EgaDirectory firstDirectory = (EgaDirectory) egaParentdirectory.contents.get(0);
        EgaDirectory secondDirectory = (EgaDirectory) firstDirectory.contents.get(0);
        EgaFile fileOutput = (EgaFile) secondDirectory.contents.get(0);

        assertEquals(file.getDisplayFilePath().split("/")[0], firstDirectory.getName());
        assertEquals(file.getDisplayFilePath().split("/")[1], secondDirectory.getName());
        assertEquals(file.getDisplayFileName(), fileOutput.getName());
    }
    
    @Test
    public void readFiles_WhenGivenEgaFileAndTreeTrueMoreFiles_ThenReturnsEgaPath() throws JsonProcessingException {
        isTreeStructureEnable = true;

        File file1 = new File();
        file1.setFileId("test");
        file1.setFileName("test.cip");
        file1.setDisplayFileName("test.cip");
        file1.setDisplayFilePath("a");

        File file2 = new File();
        file2.setFileId("test2");
        file2.setFileName("test2.cip");
        file2.setDisplayFileName("test2.cip");
        file2.setDisplayFilePath("a");
        
        File file3 = new File();
        file3.setFileId("test");
        file3.setFileName("test.cip");
        file3.setDisplayFileName("test.cip");
        file3.setDisplayFilePath("a/b");

        File file4 = new File();
        file4.setFileId("test");
        file4.setFileName("test.cip");
        file4.setDisplayFileName("test.cip");
        file4.setDisplayFilePath("/test.cip");

        List<File> egaFiles = new ArrayList<>();
        egaFiles.add(file1);
        egaFiles.add(file2);
        egaFiles.add(file3);
        egaFiles.add(file4);

        interceptor.addRule()
                .get(APP_URL.concat("/metadata/datasets/").concat(egaParentdirectory.getName()).concat("/files"))
                .respond(objectMapper.writeValueAsString(egaFiles));

        egaParentdirectory.read(pointer, fuseFillDir);

        EgaDirectory firstDirectory = (EgaDirectory) egaParentdirectory.contents.get(0);  
        EgaFile firstDirectoryFirstFile = (EgaFile) egaParentdirectory.contents.get(1);      
        
        EgaFile secondDirectoryFirstFile = (EgaFile) firstDirectory.contents.get(0);
        EgaFile secondDirectorySecondFile = (EgaFile) firstDirectory.contents.get(1);
        EgaDirectory secondDirectory = (EgaDirectory) firstDirectory.contents.get(2);
        
        EgaFile thirdDirectoryFirstFile = (EgaFile) secondDirectory.contents.get(0);

        assertEquals(file1.getDisplayFilePath().split("/")[0], firstDirectory.getName());
        assertEquals(file1.getDisplayFileName(), secondDirectoryFirstFile.getName());
        assertEquals(file2.getDisplayFilePath().split("/")[0], firstDirectory.getName());
        assertEquals(file2.getDisplayFileName(), secondDirectorySecondFile.getName());        
        assertEquals(file3.getDisplayFilePath().split("/")[0], firstDirectory.getName());
        assertEquals(file3.getDisplayFilePath().split("/")[1], secondDirectory.getName());
        assertEquals(file3.getDisplayFileName(), thirdDirectoryFirstFile.getName());        
        assertEquals(file4.getDisplayFileName(), firstDirectoryFirstFile.getName());
    }

    @Test
    public void deleteChild_WhenGivenEgaFile_ThenReturnsNoPath() {
        EgaFile egaFile = new EgaFile("files1", egaParentdirectory);
        List<EgaFile> egaFiles = new ArrayList<>();
        egaFiles.add(egaFile);

        egaParentdirectory.deleteChild(egaFile);
        List<EgaPath> contents = egaParentdirectory.contents;
        assertTrue(contents.isEmpty());
    }
}
