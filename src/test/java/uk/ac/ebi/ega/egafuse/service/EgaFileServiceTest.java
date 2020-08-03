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
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import uk.ac.ebi.ega.egafuse.config.EgaFuseApplicationConfig;
import uk.ac.ebi.ega.egafuse.model.File;

@TestPropertySource("classpath:application-test.properties")
@ContextConfiguration(classes = EgaFuseApplicationConfig.class)
@RunWith(SpringRunner.class)
public class EgaFileServiceTest {
    private IEgaFileService egaFileService;
    private MockInterceptor interceptor;
    private OkHttpClient client;
    private ObjectMapper objectMapper;

    @Value("${app.url}")
    private String APP_URL;

    @Mock
    private EgaChunkBufferService bufferService;

    @Mock
    private Token token;

    @Mock
    private Pointer pointer;

    @Before
    public void before() {
        objectMapper = new ObjectMapper();
        interceptor = new MockInterceptor(UNORDERED);
        client = new OkHttpClient.Builder().addInterceptor(interceptor).build();
        egaFileService = new EgaFileService(client, APP_URL, token, bufferService);
    }

    @Test
    public void getFiles_WhenGivenDirectory_ThenReturnsFiles() throws JsonProcessingException {
        EgaDirectory userDirectory = new EgaDirectory("EGAD00001", null, null);
        List<File> files = new ArrayList<>();
        File file = new File();
        file.setFileId("EGAF00001");
        file.setFileName("test1.cip");
        file.setDisplayFileName("test1");
        file.setFileSize(100l);
        files.add(file);
        interceptor.addRule()
                .get(APP_URL.concat("/metadata/datasets/").concat(userDirectory.getName()).concat("/files"))
                .respond(objectMapper.writeValueAsString(files));

        List<EgaFile> userFile = egaFileService.getFiles(userDirectory);
        assertEquals(1, userFile.size());
        
        File responseFile = userFile.get(0).getFile();
        assertEquals(file.getFileId(), responseFile.getFileId());
        assertEquals(file.getFileSize() - 16, responseFile.getFileSize());
        assertEquals(file.getFileName(), responseFile.getFileName());

    }

    @Test
    public void getFiles_WhenGivenDirectory_ThenReturnsOnlyCipFiles() throws JsonProcessingException {
        EgaDirectory userDirecory = new EgaDirectory("EGAD00001", null, null);
        List<File> files = new ArrayList<>();
        File file1 = new File();
        file1.setFileName("/test1.cip");
        file1.setDisplayFileName("test1");
        File file2 = new File();
        file2.setFileName("/test2.gpg");
        file2.setDisplayFileName("test2");
        files.add(file1);
        files.add(file2);

        interceptor.addRule().get(APP_URL.concat("/metadata/datasets/").concat(userDirecory.getName()).concat("/files"))
                .respond(objectMapper.writeValueAsString(files));

        List<EgaFile> userFile = egaFileService.getFiles(userDirecory);
        assertEquals(1, userFile.size());
        assertEquals(file1.getFileName(), userFile.get(0).getFile().getFileName());
    }

    @Test
    public void getFiles_WhenGivenException_ThenReturnsNoFiles() {
        EgaDirectory userDirecory = new EgaDirectory("EGAD00001", null, null);
        interceptor.addRule().get(APP_URL.concat("/metadata/datasets/").concat(userDirecory.getName()).concat("/files"))
                .respond(500);

        List<EgaFile> userFile = egaFileService.getFiles(userDirecory);
        assertTrue(userFile.isEmpty());
    }
}
