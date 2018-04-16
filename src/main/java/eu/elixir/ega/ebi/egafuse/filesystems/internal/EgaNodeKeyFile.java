/*
 * Copyright 2018 ELIXIR EGA
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

import com.squareup.moshi.Moshi;
import eu.elixir.ega.ebi.egafuse.EgaFuse;
import eu.elixir.ega.ebi.egafuse.Glue;
import eu.elixir.ega.ebi.egafuse.dto.EgaFileDto;
import eu.elixir.ega.ebi.egafuse.filesystems.EgaApiDirectory;
import eu.elixir.ega.ebi.egafuse.filesystems.EgaApiFile;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.logging.Level;
import java.util.logging.Logger;
import jnr.ffi.Pointer;
import ru.serce.jnrfuse.struct.FileStat;

/**
 * @author asenf
 */
public class EgaNodeKeyFile extends EgaApiFile {

    private final Moshi MOSHI = new Moshi.Builder().build();
    private String format = null;
    private EgaFileDto theFile;
    private byte[] dirKey;
    private String urlEncodedKey;

    public EgaNodeKeyFile(String name, EgaApiDirectory parent) {
        super(name, parent);
        setType();
        generateKey();
        try {
            format = ((EgaNodeDirectory) parent).getOrgName();
            format = EgaFuse.getOrg(format); // Translate User to Org/Key
        } catch (Throwable t) {
            System.out.println(t.toString());
        }

    }

    @Override
    protected void setType() {
        type = "KEY";
    }

    @Override
    public void getattr(FileStat stat) {
        //stat.st_mode.set(FileStat.S_IFREG | 0444);
        stat.st_mode.set(FileStat.S_IFREG | 0550);
        long size = urlEncodedKey.length();
        stat.st_size.set(size);
        String name_ = getRootName();
        stat.st_uid.set(EgaFuse.getUid(name_));
        stat.st_gid.set(EgaFuse.getGid(name_));
    }

    // Read Bytes from API
    public int read(Pointer buffer, long size, long offset) {
        // Get the size of the file
        long fsize = urlEncodedKey.length();
        int foffset = (int) Math.min(fsize, offset);
        int bytesToRead = (int) Math.min(fsize - foffset, size);

        try {
            buffer.put(0L, urlEncodedKey.getBytes(), foffset, bytesToRead);
            
        } catch (Throwable e) {
            System.out.println(e);
            return 0;
        }
        return bytesToRead;
    }

    // Generate encryption key for dir
    private void generateKey() {       
        this.dirKey = Glue.getInstance().GenerateRandomString(8, 4, 5, 2, 9, 3);
        try {
            this.urlEncodedKey = URLEncoder.encode(new String(dirKey), "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(EgaNodeKeyFile.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public String getUrlEncodedKey() {
        return this.urlEncodedKey;
    }
}
