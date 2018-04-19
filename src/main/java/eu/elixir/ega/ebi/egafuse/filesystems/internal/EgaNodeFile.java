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
package eu.elixir.ega.ebi.egafuse.filesystems.internal;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.squareup.moshi.Moshi;
import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;
import eu.elixir.ega.ebi.egafuse.EgaFuse;
import eu.elixir.ega.ebi.egafuse.dto.EgaFileDto;
import eu.elixir.ega.ebi.egafuse.filesystems.EgaApiDirectory;
import eu.elixir.ega.ebi.egafuse.filesystems.EgaApiFile;
import jnr.ffi.Pointer;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import ru.serce.jnrfuse.struct.FileStat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author asenf
 */
public class EgaNodeFile extends EgaApiFile {

    private static final int PAGE_SIZE = 1024 * 1024 * 10;
    private static final int NUM_PAGES = 8;
    private final Moshi MOSHI = new Moshi.Builder().build();
    private String format = null;
    private EgaFileDto theFile;
    private String urlEncodedKey = null;
    private String base64IV = null;
    private byte[] IV = null;
    private LoadingCache<Integer, byte[]> cache;

    public EgaNodeFile(String name, EgaApiDirectory parent) {
        super(name, parent);
        setType();
        try {
            //format = ((EgaNodeDirectory) parent).getOrgName();
            //format = EgaFuse.getOrg(format); // Translate User to Org/Key
            format = "aes256";
        } catch (Throwable t) {
            System.out.println(t.toString());
        }

        // Init cache
        this.cache = CacheBuilder.newBuilder()
                .maximumSize(NUM_PAGES)
                .concurrencyLevel(NUM_PAGES)
                .build(
                        new CacheLoader<Integer, byte[]>() {
                            public byte[] load(Integer page) throws Exception {
                                return populateCache(page);
                            }
                        });
    }

    public EgaNodeFile(String name, EgaApiDirectory parent, EgaFileDto theFile) {
        super(name, parent);
        this.theFile = theFile;
        setType();

        // Init cache
        this.cache = CacheBuilder.newBuilder()
                .maximumSize(NUM_PAGES)
                .concurrencyLevel(NUM_PAGES)
                .build(
                        new CacheLoader<Integer, byte[]>() {
                            public byte[] load(Integer page) throws Exception {
                                return populateCache(page);
                            }
                        });
    }

    public EgaNodeFile(String name, EgaApiDirectory parent, EgaFileDto theFile, String urlEncodedKey) {
        super(name, parent, (urlEncodedKey!=null));
        this.theFile = theFile;
        this.urlEncodedKey = urlEncodedKey;
        setType();

        // Init cache
        this.cache = CacheBuilder.newBuilder()
                .maximumSize(NUM_PAGES)
                .concurrencyLevel(NUM_PAGES)
                .build(
                        new CacheLoader<Integer, byte[]>() {
                            public byte[] load(Integer page) throws Exception {
                                return populateCache(page);
                            }
                        });
    }

    @Override
    public void getattr(FileStat stat) {
        //stat.st_mode.set(FileStat.S_IFREG | 0444);
        stat.st_mode.set(FileStat.S_IFREG | 0550);
        long size = theFile.getFileSize(); // Including IV
        stat.st_size.set(size);
        String name_ = getRootName();
        stat.st_uid.set(EgaFuse.getUid(name_));
        stat.st_gid.set(EgaFuse.getGid(name_));
    }

    // Read Bytes from API
    public synchronized int read(Pointer buffer, long size, long offset) {
        // Get the size of the file
        long fsize = this.theFile.getFileSize();

        // Adjust for IV.. subtract 16 bytes from offset
        long iv_offset = (offset>16)?(offset-16):0;
        int iv_delta = (int) ((offset<16)?(16-offset):0);
        int bytesToRead = (int) Math.min(fsize - iv_offset, size);

        int cachePage = (int) (iv_offset / PAGE_SIZE); // 0,1,2,...

        try {
            if (this.base64IV==null)
                getIV();
            
            // If part of the IV was requested
            long start_offset = 0L;
            if (iv_delta>0) {
                buffer.put(0L, this.IV, (int) offset, iv_delta);
                start_offset = iv_delta;
            }

            byte[] page = this.get(cachePage);

            int page_offset = (int) (iv_offset - cachePage * PAGE_SIZE);
            bytesToRead -= start_offset;
            int bytesToCopy = Math.min(bytesToRead, page.length - page_offset);
            buffer.put(start_offset+0L, page, page_offset, bytesToCopy);

            int bytesRemaining = bytesToRead - bytesToCopy;
            if (bytesRemaining > 0) {
                page = this.get(cachePage + 1); // this.cache.get(cachePage+1);
                buffer.put(start_offset+bytesToCopy, page, 0, bytesRemaining);
            }

        } catch (ExecutionException e) {
            System.out.println(e);
            return 0;
        }
        return bytesToRead;
    }

    private byte[] get(int page_number) throws ExecutionException {
        long size = (type.equalsIgnoreCase("CIP")) ? theFile.getFileSize() - 16 : theFile.getFileSize();
        int maxPage = (int) (size / PAGE_SIZE + 1);

        int firstPage = page_number > 0 ? page_number - 1 : 0;
        int lastPage = (page_number + NUM_PAGES - 1) > maxPage ? maxPage : (page_number + NUM_PAGES - 1);
        for (int i = firstPage; i < lastPage; i++) {
            final int page_i = i;
            new Thread(() -> {
                try {
                    this.cache.get(page_i);
                } catch (ExecutionException e) {
                }
            }).start();
        }

        return this.cache.get(page_number);
    }

    private byte[] populateCache(int page_number) {
        System.out.println("populateCache(): page_number: " + page_number);
        long endC = page_number * PAGE_SIZE + PAGE_SIZE;
        System.out.println("populateCache() endC: " + endC);
        int toRead = (int) (endC > theFile.getFileSize() ? (theFile.getFileSize() - (page_number * PAGE_SIZE)) : PAGE_SIZE);
        int bytesToRead = toRead;
        System.out.println("populateCache() toRead: " + toRead);
        //int bytesToRead = PAGE_SIZE;
        long offset = page_number * PAGE_SIZE;
        // Prepare buffer to read from file
        byte[] bytesRead = new byte[bytesToRead];

        synchronized (this) {

            try {
                String url = null;                
                byte[] iv = Base64.decode(base64IV);
                byte_increment_fast(iv, offset);
                url = getBaseUrl() + "/files/" + theFile.getFileId() +
                    "?destinationFormat=" + format +
                    "&destinationKey=" + urlEncodedKey +
                    "&destinationIV=" + iv +
                    "&startCoordinate=" + offset +
                    "&endCoordinate=" + (offset + bytesToRead);                    
                    
                Request datasetRequest = new Request.Builder()
                        .url(url)
                        .addHeader("Authorization", "Basic " + getBasicCode())
                        .build();

                // Execute the request and retrieve the response.
                Response response = client.newCall(datasetRequest).execute();
                ResponseBody body = response.body();

                InputStream byteStream = response.body().byteStream();
                int bytesRead_ = 0;
                byte[] buff = new byte[8000];
                ByteArrayOutputStream bao = new ByteArrayOutputStream();

                while ((bytesRead_ = byteStream.read(buff)) != -1) {
                    bao.write(buff, 0, bytesRead_);
                }

                byte[] result = bao.toByteArray();
                bytesRead = Arrays.copyOf(result, bytesToRead);
            } catch (IOException ex) {
                Logger.getLogger(EgaNodeFile.class.getName()).log(Level.SEVERE, null, ex);
            }

        }

        return bytesRead;
    }

    private void byte_increment_fast(byte[] data, long increment) {
        long countdown = increment / 16; // Count number of block updates

        ArrayList<Integer> digits_ = new ArrayList<>();
        int cnt = 0;
        long d = 256, cn = 0;
        while (countdown > cn && d > 0) {
            int l = (int) ((countdown % d) / (d / 256));
            digits_.add(l);
            cn += (l * (d / 256));
            d *= 256;
        }
        int size = digits_.size();
        int[] digits = new int[size];
        for (int i = 0; i < size; i++) {
            digits[size - 1 - i] = digits_.get(i); // intValue()
        }

        int cur_pos = data.length - 1, carryover = 0, delta = data.length - digits.length;

        for (int i = cur_pos; i >= delta; i--) { // Work on individual digits
            int digit = digits[i - delta] + carryover; // convert to integer
            int place = (int) (data[i] & 0xFF); // convert data[] to integer
            int new_place = digit + place;
            if (new_place >= 256) carryover = 1;
            else carryover = 0;
            data[i] = (byte) (new_place % 256);
        }

        // Deal with potential last carryovers
        cur_pos -= digits.length;
        while (carryover == 1 && cur_pos >= 0) {
            data[cur_pos]++;
            if (data[cur_pos] == 0) carryover = 1;
            else carryover = 0;
            cur_pos--;
        }
    }

    private void getIV() {
        try{
            String url = getBaseUrl() + "/files/" + theFile.getFileId() +
                "?destinationFormat=" + format +
                "&destinationKey=" + urlEncodedKey;

            Request datasetRequest = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Basic " + getBasicCode())
                    .build();

            // Execute the request and retrieve the response.
            Response response = client.newCall(datasetRequest).execute();
            ResponseBody body = response.body();

            InputStream byteStream = response.body().byteStream();
            int bytesRead_ = 0;
            this.IV = new byte[16];
            bytesRead_ = byteStream.read(this.IV);
            if (bytesRead_ < 16)
                System.out.println("IV not fully read!");

            this.base64IV = Base64.encode(this.IV);
            
            body.close();
        } catch (IOException ex) {
            Logger.getLogger(EgaNodeFile.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
