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

import ru.serce.jnrfuse.struct.FileStat;

public abstract class EgaPath {
    private String name;
    private EgaDirectory parent;

    EgaPath(String name) {
        this(name, null);
    }

    EgaPath(String name, EgaDirectory parent) {
        this.name = name;
        this.parent = parent;
    }

    protected EgaPath find(String path) {
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        if (path.equals(name) || path.isEmpty()) {
            return this;
        }
        return null;
    }

    public abstract void getattr(FileStat stat);

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public EgaPath getParent() {
        return parent;
    }

    public void setParent(EgaDirectory parent) {
        this.parent = parent;
    }

    public synchronized void delete() {
        if (parent != null) {
            parent.deleteChild(this);
            parent = null;
        }
    }
}
