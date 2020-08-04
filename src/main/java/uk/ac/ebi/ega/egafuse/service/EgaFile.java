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

import jnr.ffi.Pointer;
import ru.serce.jnrfuse.struct.FileStat;
import uk.ac.ebi.ega.egafuse.model.File;

public class EgaFile extends EgaPath {
    private File file;
    private IEgaChunkBufferService egaChunkBufferService;

    public EgaFile(String name, EgaDirectory parent) {
        super(name, parent);
    }

    public EgaFile(String name, File file, IEgaChunkBufferService egaChunkBufferService) {
        super(name);
        this.file = file;
        this.egaChunkBufferService = egaChunkBufferService;
    }

    @Override
    public void getattr(FileStat stat) {
        stat.st_mode.set(FileStat.S_IFREG | 0444);
        stat.st_size.set(file.getFileSize());
    }

    public int read(Pointer buffer, long size, long offset) {
        return egaChunkBufferService.fillBuffer(buffer, file.getFileId(), file.getFileSize(), size, offset);
    }

    public int open() {
        return 0;
    }
    
    public File getFile() {
        return file;
    }
}
