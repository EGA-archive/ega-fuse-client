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
import jnr.ffi.types.off_t;
import jnr.ffi.types.size_t;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.serce.jnrfuse.ErrorCodes;
import ru.serce.jnrfuse.FuseFillDir;
import ru.serce.jnrfuse.FuseStubFS;
import ru.serce.jnrfuse.struct.FileStat;
import ru.serce.jnrfuse.struct.FuseFileInfo;

import java.nio.file.Paths;

public class EgaFuse extends FuseStubFS {
    private String mountPath;
    private EgaDirectory rootDirectory;

    private static final Logger LOGGER = LoggerFactory.getLogger(EgaFuse.class);

    public EgaFuse(EgaDirectory rootDirectory, String mountPath) {
        this.rootDirectory = rootDirectory;
        this.mountPath = mountPath;
    }

    public void start() {
        try {
            String[] args_ = new String[]{"-o", "allow_other"};
            LOGGER.info("Mounting on path: {}", mountPath);
            this.mount(Paths.get(mountPath), true, false, args_);
        } finally {
            this.umount();
        }
    }

    @Override
    public int readdir(String path, Pointer buf, FuseFillDir filter, @off_t long offset, FuseFileInfo fi) {
        EgaPath p = getPath(path);
        if (p == null) {
            return -ErrorCodes.ENOENT();
        }
        if (!(p instanceof EgaDirectory)) {
            return -ErrorCodes.ENOTDIR();
        }
        filter.apply(buf, ".", null, 0);
        filter.apply(buf, "..", null, 0);
        ((EgaDirectory) p).read(buf, filter);
        return 0;
    }

    @Override
    public int getattr(String path, FileStat stat) {
        EgaPath p = getPath(path);
        if (p != null) {
            p.getattr(stat);
            stat.st_uid.set(getContext().uid.get());
            stat.st_gid.set(getContext().gid.get());
            return 0;
        }
        return -ErrorCodes.ENOENT();
    }

    @Override
    public int read(String path, Pointer buf, @size_t long size, @off_t long offset, FuseFileInfo fi) {
        EgaPath p = getPath(path);
        if (p == null) {
            return -ErrorCodes.ENOENT();
        }
        if (!(p instanceof EgaFile)) {
            return -ErrorCodes.EISDIR();
        }
        return ((EgaFile) p).read(buf, size, offset);
    }

    @Override
    public int unlink(String path) {
        EgaPath p = getPath(path);
        if (p == null) {
            return -ErrorCodes.ENOENT();
        }
        p.delete();
        return 0;
    }

    @Override
    public int open(String path, FuseFileInfo fi) {
        EgaPath p = getPath(path);
        if (p == null) {
            return -ErrorCodes.ENOENT();
        }
        if (!(p instanceof EgaFile)) {
            return -ErrorCodes.EISDIR();
        } else {
            ((EgaFile) p).open();
        }
        return 0;
    }

    private EgaPath getPath(String path) {
        return rootDirectory.find(path);
    }

}
