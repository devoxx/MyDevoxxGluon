/**
 * Copyright (c) 2018, Gluon Software
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation and/or other materials provided
 *    with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse
 *    or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.devoxx.views.helper;

import com.gluonhq.connect.provider.RestClient;
import com.gluonhq.connect.source.FileDataSource;
import com.gluonhq.connect.source.RestDataSource;
import com.gluonhq.impl.cloudlink.client.PrivateStorage;
import com.gluonhq.impl.cloudlink.client.data.function.CachingInputStream;
import javafx.concurrent.Task;
import javafx.scene.image.Image;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonWriter;
import java.io.*;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ETagImageTask extends Task<Image> {

    private static final String DEVOXX_IMAGE = "_devoxx_image2";
    private static final Logger LOGGER = Logger.getLogger(ETagImageTask.class.getName());

    private final String url;
    private final String eTagFileName;
    private final String imageFileName;
    private final String incompleteFileName;
    
    private Image image;

    public ETagImageTask(String id, String url) {
        this.url = url;
        this.imageFileName = id + DEVOXX_IMAGE + url.substring(url.lastIndexOf("."));
        this.eTagFileName = imageFileName + ".etag";
        this.incompleteFileName = imageFileName + ".incomplete";

        try {
            final File incompleteFile = createFile(incompleteFileName);
            if (incompleteFile != null && incompleteFile.exists()) {
                // delete etag and incomplete jpeg files
                incompleteFile.delete();
                createFile(eTagFileName).delete();
            } else {
                final FileDataSource imageDataSource = createCacheImageDataSource(imageFileName);
                if (imageDataSource != null && imageDataSource.getFile().exists()) {
                    InputStream inputStream = imageDataSource.getInputStream();
                    image = new Image(inputStream);
                }
            }
        } catch (IOException e) {
            // do nothing
        }
    }

    public Optional<Image> image() {
        return Optional.ofNullable(image);
    }

    @Override
    protected Image call() throws Exception {
        final FileDataSource eTagDataSource = createETagDataSource(eTagFileName);
        String eTag = readFromETag(eTagDataSource);
        RestClient client = createMediaRestClient(url, eTag);

        final RestDataSource restDataSource = client.createRestDataSource();
        final FileDataSource cacheImageDataSource = createCacheImageDataSource(imageFileName);
        final InputStream inputStream = restDataSource.getInputStream();
        if (inputStream == null) {
            return null;
        }

        if (cacheImageDataSource != null &&
                cacheImageDataSource.getFile().exists() &&
                restDataSource.getResponseCode() == 304) {
            return image != null ? image : new Image(cacheImageDataSource.getInputStream());
        }
        // TODO: Check if it works when ETag is updated
        writeToETag(eTagDataSource, restDataSource);
        final FileDataSource incompleteImageDataSource = createCacheImageDataSource(incompleteFileName);
        if (incompleteImageDataSource != null) {
            updateProgress(1.0, 1.0);
            try (CachingInputStream cis = new RenameCachingStream(inputStream, incompleteImageDataSource, cacheImageDataSource)) {
                return new Image(cis);
            }
        }
        return new Image(inputStream);
    }

    private RestClient createMediaRestClient(String url, String eTag) {
        return RestClient.create()
                .method("GET")
                .header("If-None-Match", eTag)
                .host(url)
                .readTimeout(30000)
                .connectTimeout(60000);
    }

    private String readFromETag(FileDataSource eTagDataSource) {
        if (eTagDataSource != null && eTagDataSource.getFile().exists()) {
            try (JsonReader reader = Json.createReader(eTagDataSource.getInputStream())) {
                JsonObject jsonObject = reader.readObject();
                return jsonObject.getString("eTag");

            } catch (IOException e) {
                LOGGER.log(Level.FINE, "Could not read ETag file.", e);
            }
        }
        return "";
    }

    private void writeToETag(FileDataSource eTagDataSource, RestDataSource restDataSource) {
        final List<String> eTagString = restDataSource.getResponseHeaders().get("ETag");
        if (eTagString != null && eTagString.size() > 0) {
            try (JsonWriter writer = Json.createWriter(eTagDataSource.getOutputStream())) {
                writer.write(Json.createObjectBuilder().add("eTag", eTagString.get(0)).build());
            } catch (IOException e) {
                LOGGER.log(Level.FINE, "Could not write to ETag file.", e);
            }
        }
    }

    private FileDataSource createETagDataSource(String name) {
        final File eTagFile = createFile(name);
        if (eTagFile != null) {
            return new FileDataSource(eTagFile);
        }
        return null;
    }

    private FileDataSource createCacheImageDataSource(String mediaName) {
        File cache = createFile(mediaName);
        if (cache != null) {
            return new FileDataSource(cache);
        }
        return null;
    }

    private File createFile(String name) {
        File root = PrivateStorage.get();
        if (root != null) {
            return new File(root, name);
        }
        return null;
    }

    /**
     * Writes the input stream to the source's output stream.
     * Once completed, renames the source file to destination file.
     */
    private class RenameCachingStream extends CachingInputStream {

        private FileDataSource source;
        private FileDataSource destination;

        RenameCachingStream(InputStream in, FileDataSource source, FileDataSource destination) throws IOException {
            super(in, source.getOutputStream());
            this.source = source;
            this.destination = destination;
        }

        @Override
        public void close() throws IOException {
            try {
                super.close();
            } finally {
                source.getFile().renameTo(destination.getFile());
            }
        }
    }
}
