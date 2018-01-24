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
package eu.elixir.ega.ebi.egafuse.filesystems;

import eu.elixir.ega.ebi.egafuse.EgaFuse;
import ru.serce.jnrfuse.struct.FileStat;

/**
 *
 * @author asenf
 */
public abstract class EgaApiPath {
    
    protected String name;
    protected EgaApiDirectory parent;

    private EgaApiPath(String name) {
        this(name, null);
    }

    EgaApiPath(String name, EgaApiDirectory parent) {
        this.name = name;
        this.parent = parent;
    }
    
    // Move to Request/Ticket
    //private synchronized void delete() {
    //    if (parent != null) {
    //        parent.deleteChild(this);
    //        parent = null;
    //    }
    //}

    protected EgaApiPath find(String path) {
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        if (path.equals(name) || path.isEmpty()) {
            return this;
        }
        return null;
    }

    public abstract void getattr(FileStat stat);

    //private void rename(String newName) {
    //    while (newName.startsWith("/")) {
    //        newName = newName.substring(1);
    //    }
    //    name = newName;
    //}

    public String getName() {
        return this.name;
    }
    
    public String getRootName() {
        try {
            if (parent.getParent()==null) {
                return name;
            } else {
                return parent.getRootName();
            }
        } catch (Throwable t) {
            return "";
        }
    }
    
    public EgaApiPath getParent() {
        return parent;
    }
    
    public String getAccessToken() {
        return EgaFuse.getToken();
    }
    
    public String getBasicCode() {
        return EgaFuse.getBasicCode();
    }
    
    public String getBaseUrl() {
        return EgaFuse.getApiUrl();
    }
    
    public String getCentralUrl() {
        return EgaFuse.getCentralUrl();
    }
    
    public void setParent(EgaApiDirectory parent) {
        this.parent = parent;
    }
    
    public void setName(String name) {
        this.name = name;
    }

    public synchronized void delete() {
        if (parent != null) {
            parent.deleteChild(this);
            parent = null;
        }
    }
}
