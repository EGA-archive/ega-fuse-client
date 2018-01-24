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

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import eu.elixir.ega.ebi.egafuse.EgaFuse;
import eu.elixir.ega.ebi.egafuse.dto.EgaFileDto;
import eu.elixir.ega.ebi.egafuse.dto.EgaTicketDto;
import java.util.ArrayList;
import java.util.List;
import jnr.ffi.Pointer;
import ru.serce.jnrfuse.FuseFillDir;
import ru.serce.jnrfuse.struct.FileStat;

/**
 *
 * @author asenf
 */
public class EgaApiDirectory extends EgaApiPath {
    
    protected final Moshi MOSHI = new Moshi.Builder().build();
    protected final JsonAdapter<List<String>> STRING_JSON_ADAPTER = MOSHI.adapter(Types.newParameterizedType(List.class, String.class));
    protected final JsonAdapter<List<EgaFileDto>> FILE_JSON_ADAPTER = MOSHI.adapter(Types.newParameterizedType(List.class, EgaFileDto.class));
    protected final JsonAdapter<List<EgaTicketDto>> TICKET_JSON_ADAPTER = MOSHI.adapter(Types.newParameterizedType(List.class, EgaTicketDto.class));
    
    protected List<EgaApiPath> contents = new ArrayList<>();    
    
    public EgaApiDirectory(String name, EgaApiDirectory parent) {
        super(name, parent);
    }

    // Operations
    public synchronized void add(EgaApiPath p) {
        contents.add(p);
        p.setParent(this);
    }

    public synchronized void deleteChild(EgaApiPath child) {
        contents.remove(child);
    }

    @Override
    public EgaApiPath find(String path) {

        if (super.find(path) != null) {
            return super.find(path);
        }
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        synchronized (this) {
            if (!path.contains("/")) {
                for (EgaApiPath p : contents) {
                    if (p.getName().equals(path)) {
                        return p;
                    }
                }
                return null;
            }
            String nextName = path.substring(0, path.indexOf("/"));
            String rest = path.substring(path.indexOf("/"));
            for (EgaApiPath p : contents) {
                if (p.getName().equals(nextName)) {
                    return p.find(rest);
                }
            }
        }
        return null;
    }

    @Override
    public void getattr(FileStat stat) {
        //stat.st_mode.set(FileStat.S_IFDIR | 0755);
        stat.st_mode.set(FileStat.S_IFDIR | 0550);
        String name_ = getRootName();
        stat.st_uid.set(EgaFuse.getUid(name_));
        stat.st_gid.set(EgaFuse.getGid(name_));
    }

    public synchronized void read(Pointer buf, FuseFillDir filler) {
        for (EgaApiPath p : contents) {
            filler.apply(buf, p.getName(), null, 0);
        }
    }
}
