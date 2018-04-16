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

import eu.elixir.ega.ebi.egafuse.SSLUtilities;
import okhttp3.OkHttpClient;

/**
 * @author asenf
 */
public abstract class EgaApiFile extends EgaApiPath {

    protected String type; // Source Encryption Type (CIP, GPG, SOURCE)
    protected String target = null; // Source Encryption Type (CIP, GPG, SOURCE)
    protected boolean encrypted = false;

    protected OkHttpClient client;

    public EgaApiFile(String name, EgaApiDirectory parent) {
        super(name, parent);
    }
    
    public EgaApiFile(String name, EgaApiDirectory parent, boolean encrypted) {
        super(name, parent);
        this.encrypted = encrypted;
    }

    // Most functionality should be shared between both File and Ticket access
    // File needs Re-encryption Key (plain for CSC)
    // Ticket has key from server; provide option to decrypt locally?
    protected void setType() {
        if (name.toLowerCase().endsWith(".gpg")) {
            type = "GPG";
            if (!encrypted) name = name.substring(0, name.length() - 4);
        } else if (name.toLowerCase().endsWith(".cip")) {
            type = "CIP";
            if (!encrypted) name = name.substring(0, name.length() - 4);
        } else {
            type = "SOURCE";
        }
    }

    // Access File - "Open"
    public int open() {
        client = SSLUtilities.getUnsafeOkHttpClient();

        return 0;
    }
}
