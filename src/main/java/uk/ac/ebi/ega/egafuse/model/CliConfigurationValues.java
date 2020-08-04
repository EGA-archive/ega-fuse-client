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
package uk.ac.ebi.ega.egafuse.model;

import java.nio.file.Path;

public class CliConfigurationValues {
    private boolean isTreeStructureEnable;
    private int connection;
    private int connectionPerFile;
    private int maxCache;
    private Path mountPath;
    private Credential credential;

    public int getConnection() {
        return connection;
    }

    public void setConnection(int connection) {
        this.connection = connection;
    }

    public int getConnectionPerFile() {
        return connectionPerFile;
    }

    public void setConnectionPerFile(int connectionPerFile) {
        this.connectionPerFile = connectionPerFile;
    }

    public int getMaxCache() {
        return maxCache;
    }

    public void setMaxCache(int maxCache) {
        this.maxCache = maxCache;
    }

    public Path getMountPath() {
        return mountPath;
    }

    public void setMountPath(Path mountPath) {
        this.mountPath = mountPath;
    }

    public Credential getCredential() {
        return credential;
    }

    public void setCredential(Credential credential) {
        this.credential = credential;
    }

    public boolean isTreeStructureEnable() {
        return isTreeStructureEnable;
    }

    public void setTreeStructureEnable(boolean isTreeStructureEnable) {
        this.isTreeStructureEnable = isTreeStructureEnable;
    }

    @Override
    public String toString() {
        return "CliConfigurationValues [credential=" + credential + ", maxCache=" + maxCache + ", connection="
                + connection + ", connectionPerFile=" + connectionPerFile + ", mountPath=" + mountPath
                + ", isTreeStructureEnable=" + isTreeStructureEnable + "]";
    }
}
