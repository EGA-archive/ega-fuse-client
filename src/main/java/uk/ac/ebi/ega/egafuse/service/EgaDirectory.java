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

import static uk.ac.ebi.ega.egafuse.config.EgaFuseApplicationConfig.isTreeStructureEnable;

import java.util.ArrayList;
import java.util.List;

import jnr.ffi.Pointer;
import ru.serce.jnrfuse.FuseFillDir;
import ru.serce.jnrfuse.struct.FileStat;

public class EgaDirectory extends EgaPath {
    protected List<EgaPath> contents = new ArrayList<>();
    private IEgaDatasetService egaDatasetService;
    private IEgaFileService egaFileService;

    public EgaDirectory(String name, IEgaDatasetService egaDatasetService,
            IEgaFileService egaFileService) {
        super(name);
        this.egaDatasetService = egaDatasetService;
        this.egaFileService = egaFileService;
    }

    public synchronized void add(EgaPath p) {
        contents.add(p);
        p.setParent(this);
    }

    public synchronized void deleteChild(EgaPath child) {
        contents.remove(child);
    }

    @Override
    public EgaPath find(String path) {
        if (super.find(path) != null) {
            return super.find(path);
        }
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        synchronized (this) {
            if (!path.contains("/")) {
                for (EgaPath p : contents) {
                    if (p.getName().equals(path)) {
                        return p;
                    }
                }
                return null;
            }
            String nextName = path.substring(0, path.indexOf("/"));
            String rest = path.substring(path.indexOf("/"));
            for (EgaPath p : contents) {
                if (p.getName().equals(nextName)) {
                    return p.find(rest);
                }
            }
        }
        return null;
    }

    @Override
    public void getattr(FileStat stat) {
        stat.st_mode.set(FileStat.S_IFDIR | 0444);
    }

    public synchronized void read(Pointer buf, FuseFillDir filler) {
        if (contents.size() == 0) {
            if ("Datasets".equalsIgnoreCase(getName())) {
                egaDatasetService.getDatasets().forEach(this::add);
            } else {
                List<EgaFile> egaFiles = egaFileService.getFiles(this);

                if (isTreeStructureEnable)
                    egaDatasetService.buildSubDirectoryFromFilePath(egaFiles, this);
                else
                    egaFiles.stream().forEach(this::add);
            }
        }

        for (EgaPath p : contents) {
            filler.apply(buf, p.getName(), null, 0);
        }
    }
}
