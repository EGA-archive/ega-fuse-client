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

import com.squareup.moshi.Moshi;
import com.google.common.cache.*;
import eu.elixir.ega.ebi.egafuse.EgaFuse;

import eu.elixir.ega.ebi.egafuse.dto.EgaFileDto;
import eu.elixir.ega.ebi.egafuse.filesystems.EgaApiDirectory;
import eu.elixir.ega.ebi.egafuse.filesystems.EgaApiFile;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import jnr.ffi.Pointer;

import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import ru.serce.jnrfuse.struct.FileStat;

import java.util.concurrent.ExecutionException;
/**
 *
 * @author asenf
 */
public class EgaRemoteFile extends EgaApiFile {

    private EgaFileDto theFile;

    private final Moshi MOSHI = new Moshi.Builder().build();
   
    private LoadingCache<Integer, byte[]> cache;

    private static final int PAGE_SIZE = 512*1024;
    private static final int NUM_PAGES = 22;
 
    public EgaRemoteFile(String name, EgaApiDirectory parent) {
        super(name, parent);
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

    public EgaRemoteFile(String name, EgaApiDirectory parent, EgaFileDto theFile) {
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
    
    @Override
    public void getattr(FileStat stat) {
        stat.st_mode.set(FileStat.S_IFREG | 0444);
        long size = (type.equalsIgnoreCase("CIP"))?theFile.getFileSize()-16:theFile.getFileSize();
        stat.st_size.set(size);
    } 

    // Read Bytes from API
    public int read(Pointer buffer, long size, long offset) {
        long fsize = (type.equalsIgnoreCase("CIP"))?this.theFile.getFileSize()-16:this.theFile.getFileSize();
        int bytesToRead = (int) Math.min(fsize - offset, size);

        int cachePage = (int)(offset / PAGE_SIZE); // 0,1,2,... 

        System.out.println("read() offset: " + offset + " size: " + size);
        System.out.println("read() fsize: " + fsize + " bytesToRead: " + bytesToRead);

        try {
            byte[] page = this.get(cachePage);

            int page_offset = (int)(offset-cachePage*PAGE_SIZE);
            int bytesToCopy = Math.min(bytesToRead, page.length-page_offset);
            buffer.put(0L, page, page_offset, bytesToCopy);

            int bytesRemaining = bytesToRead - bytesToCopy;
            if (bytesRemaining > 0) {
                page = this.get(cachePage+1); // this.cache.get(cachePage+1);
                buffer.put(bytesToCopy, page, 0, bytesRemaining);
            }

        } catch(ExecutionException e) { 
            System.out.println(e);
            return 0; 
        }
        return bytesToRead;
    }

    private byte[] get(int page_number) throws ExecutionException {
        long size = (type.equalsIgnoreCase("CIP"))?theFile.getFileSize()-16:theFile.getFileSize();
        int maxPage = (int) (size / PAGE_SIZE + 1);

        int firstPage = page_number>0?page_number-1:0;
        int lastPage = (page_number+NUM_PAGES-1)>maxPage?maxPage:(page_number+NUM_PAGES-1);
	for (int i=firstPage; i < lastPage; i++) { 
            final int page_i = i;
            new Thread(() -> {
                try {
                      this.cache.get(page_i);
                } catch(ExecutionException e) {}
            }).start();
	}

	return this.cache.get(page_number);
   }

   private byte[] populateCache(int page_number) throws IOException {
	System.out.println("populateCache(): page_number: " + page_number);
        long endC = page_number*PAGE_SIZE+PAGE_SIZE;
        int toRead = (int) (endC>theFile.getFileSize()-16?(theFile.getFileSize()-16-(page_number*PAGE_SIZE)):PAGE_SIZE);
        int bytesToRead = toRead;
        //int bytesToRead = PAGE_SIZE;
	long offset = page_number * PAGE_SIZE;
        // Prepare buffer to read from file
        byte[] bytesRead = new byte[bytesToRead];

        synchronized (this) {

            try {
                String format = "plain";
                
                String url = getBaseUrl() + "/files/" + theFile.getFileId() + 
                        "?destinationFormat=" + format + 
                        "&startCoordinate=" + offset + 
                        "&endCoordinate=" + (offset+bytesToRead);
                //System.out.println(url);
                Request fileRequest = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + getAccessToken())
                    .build();

                // Execute the request and retrieve the response (1 re-try)
                Response response = null;
                try {
                    response = client.newCall(fileRequest).execute();
                    if (response.code()==500) { // Expired Token - Try Refresh
                        EgaFuse.refreshAuthorize();

                        fileRequest = new Request.Builder()
                        .url(url)
                        .addHeader("Authorization", "Bearer " + getAccessToken())
                        .build();

                        response = client.newCall(fileRequest).execute();
                    } else if (!response.isSuccessful()) { // error response - re-try one time
                        Thread.sleep(500);
                        response = client.newCall(fileRequest).execute();
                    } 
                } catch (SocketTimeoutException ex) {
                    Thread.sleep(500);
                    try {response = client.newCall(fileRequest).execute();} catch (SocketTimeoutException ex1) {return null;}
                }
                
                ResponseBody body = response.body();
                
                InputStream byteStream = response.body().byteStream();
                int bytesRead_ = 0;
                byte[] buff = new byte[8000];
                ByteArrayOutputStream bao = new ByteArrayOutputStream();

                while((bytesRead_ = byteStream.read(buff)) != -1) {
                   bao.write(buff, 0, bytesRead_);
                }

                byte[] result = bao.toByteArray();
                bytesRead = Arrays.copyOf(result, bytesToRead);
                
                response.close();
                bao.close();
                bao = null;
            } catch (Exception ex) {
                Logger.getLogger(EgaRemoteFile.class.getName()).log(Level.SEVERE, null, ex);
                throw new IOException();
            }

        }

	return bytesRead;
  }
    
}