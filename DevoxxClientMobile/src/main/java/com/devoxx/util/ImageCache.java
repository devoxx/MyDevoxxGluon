/**
 * Copyright (c) 2016, Gluon Software
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
package com.devoxx.util;

import com.gluonhq.attach.cache.Cache;
import com.gluonhq.attach.cache.CacheService;
import com.gluonhq.attach.storage.StorageService;
import com.gluonhq.attach.util.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.image.Image;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.DoubleProperty;

public class ImageCache {

    private static final Logger LOG = Logger.getLogger(ImageCache.class.getName());
    
    private static final int BUFFER_SIZE = 4096;

    /**
     * Prevents from loading big image files that consume a lot of memory
     * on devices that don't have much, trying to avoid Out Of Memory
     * 
     * FIXME: Adjust to a lower value if memory issues are still present
     */
    private static final int MAX_FILE_LENGTH = 500_000; // bytes

    private static final Cache<String, Image> memoryImageCache;

    private static final AtomicInteger THREAD_NUMBER = new AtomicInteger(0);
    private static ExecutorService executorService = Executors.newFixedThreadPool(5, runnable -> {
        Thread thread = Executors.defaultThreadFactory().newThread(runnable);
        thread.setName("ImageProgressListenerThread-" + THREAD_NUMBER.getAndIncrement());
        thread.setDaemon(true);
        return thread;
    });

    static {
        memoryImageCache = CacheService.create()
            .map(c -> c.<String, Image>getCache("OTNImageCache"))
            .orElseGet(null);
    }
    private static final Optional<File> imageStore = initImageStore(); // null if storage is not available
    
    private static class ImageProgressListener implements ChangeListener<Number> {

        private final Image image;
        private final String imageId;
        private final Consumer<Image> downloadFinished;
        private final DoubleProperty progress;

        ImageProgressListener(Image image, String imageId, Consumer<Image> downloadFinished, DoubleProperty progress) {
            this.image = image;
            this.imageId = imageId;
            this.downloadFinished = downloadFinished;
            this.progress = progress;
            if (!Platform.isIOS()) {
                // Limit download size: if size exceeds maximum it will be cancelled
                limitDownloadSize();
            }
        }

        @Override
        public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
            if (progress != null) {
                progress.set(newValue.doubleValue());
            }
            if (1.0 == (Double) newValue && !image.isError()) {
                memoryImageCache.put(imageId, image);
                storeInFileCache(imageId);
                if (downloadFinished != null) {
                    downloadFinished.accept(image);
                }
                image.progressProperty().removeListener(this);
            } else if (image.isError()) {
                if (DevoxxLogging.LOGGING_ENABLED) {
                    LOG.log(Level.WARNING, "Error downloading image " + imageId + ": " + image.getException());
                }
                if (progress != null) {
                    progress.set(-1);
                }
                image.progressProperty().removeListener(this);
            } 
        }
        
        private void limitDownloadSize() {
            executorService.execute(() -> {
                try {
                    URL url = new URL(imageId);
                    URLConnection conn = url.openConnection();
                    if (conn.getContentLength() > MAX_FILE_LENGTH) {
                        image.cancel();
                    }
                } catch (IOException ex) {
                    LOG.log(Level.SEVERE, null, ex);
                    image.cancel();
                }
            });
        }
    }

    private static Optional<File> initImageStore() {
        try {
            File rootDir = StorageService.create()
                    .flatMap(StorageService::getPrivateStorage)
                    .orElseThrow(() -> new IOException("Private storage file not available"));
            
            File imageDir = new File(rootDir, "otn-images");
            imageDir.mkdir();
            return Optional.of(imageDir);
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
            return Optional.empty();
        }
    }

    /**
     * Checks if the image given by its url is already available at the local cache.
     * 
     * It doesn't perform any further action
     * 
     * @param urlString image url
     * @return true if the image file is available at the local cache
     */
    public static boolean isInLocalCache(String urlString) {
        return isInLocalCache(urlString, false);
    }

    /**
     * Checks if the image given by its url is already available at the local cache.
     * 
     * In case it is not available, a download can be forced for future uses
     * 
     * @param urlString image url
     * @param allowDownload if set to true and file is not available it will start to
     * download it for future use
     * @return true if the image file is available at the local cache
     */
    public static boolean isInLocalCache(String urlString, boolean allowDownload) {
        return get(urlString, () -> null, null, allowDownload, false, null) != null;
    }
    
    /**
     * Retrieves image by url. Uses file storage for caching if possible
     * @param urlString image url
     * @return image or null if image wa not retrieved
     */
    public static Image get(String urlString) {
        return get(urlString, () -> null, null);
    }
    
    /**
     * Retrieves image by url. Uses file storage for caching if possible
     * 
     * In case image is not available in memory or locally, it will try to download it
     * 
     * @param urlString image url
     * @param defaultImage a supplier with the default image in case it fails
     * @param downloadFinished a consumer that will be accepted when the image download finishes
     * @return image or image produced by "defaultImage" supplier if image was not retrieved
     */
    public static Image get(String urlString, Supplier<Image> defaultImage, Consumer<Image> downloadFinished) {
        return get(urlString, defaultImage, downloadFinished, true, true, null);
    }
    
    /**
     * Retrieves image by url. Uses file storage for caching if possible
     * 
     * In case image is not available in memory or locally, it will try to download it
     * 
     * @param urlString image url
     * @param defaultImage a supplier with the default image in case it fails
     * @param downloadFinished a consumer that will be accepted when the image download finishes
     * @param progress a double property that can be used to monitor the image download progress 
     * @return image or image produced by "defaultImage" supplier if image was not retrieved
     */
    public static Image get(String urlString, Supplier<Image> defaultImage, Consumer<Image> downloadFinished, DoubleProperty progress) {
        return get(urlString, defaultImage, downloadFinished, true, false, progress);
    }
    
    private static Image get(String urlString, Supplier<Image> defaultImage, Consumer<Image> downloadFinished, boolean allowDownload, boolean allowPlaceholder, DoubleProperty progress) {

        // 1. If empty image -> default image
        if (Strings.isNullOrEmpty(urlString)) {
            if (progress != null) {
                progress.set(1);
            }
            return defaultImage.get();
        }

        // 2. If image in memory cache
        if (memoryImageCache != null) {
            Image img = memoryImageCache.get(urlString);
            if (img != null) {
                if (DevoxxLogging.LOGGING_ENABLED) {
                    LOG.log(Level.FINE, "Image loaded from the image cache: " + urlString);
                }
                if (progress != null) {
                    progress.set(1);
                }
                return img;
            }
        } else {
            LOG.log(Level.WARNING, "Error: No image cache available");
        }

        // 3. Local file or download or default
        return imageStore.map(store -> {

            // 3.1 Image in classpath
            Image image = getFromClasspath(urlString, () -> null);
            if (image != null) {
                if (DevoxxLogging.LOGGING_ENABLED) {
                    LOG.log(Level.FINE, "Image loaded from the classpath: " + urlString);
                }
                if (memoryImageCache != null) {
                    memoryImageCache.put(urlString, image);
                }
                if (progress != null) {
                    progress.set(1);
                }
                return image;
            }

            // 3.2. Image in local file
            image = getFromFileCache(urlString, defaultImage);
            if (image != null) {
                if (DevoxxLogging.LOGGING_ENABLED) {
                    LOG.log(Level.FINE, "Image loaded from the local storage: " + urlString);
                }
                if (memoryImageCache != null) {
                    memoryImageCache.put(urlString, image);
                }
                if (progress != null) {
                    progress.set(1);
                }
                return image;
            }
            
            if (allowDownload) {
                // 3.3. Start downloading image if allowed, background thread
                String validUrlString = urlString.replaceAll("\\s","%20");
                try {
                    image = new Image(validUrlString, true);
                    image.progressProperty().addListener(new ImageProgressListener(image, validUrlString, downloadFinished, progress));
                } catch (Throwable ex) {
                    LOG.log(Level.SEVERE, "Issues retrieving image for " + validUrlString, ex);
                }
                // if allowPlaceholder, returns image or its placeholder, as the download may have not completed yet
                if (allowPlaceholder) {
                    if (downloadFinished == null) {
                        return image;
                    } else {
                        return defaultImage.get();
                    }
                }
                // otherwise return null (image is not null)
            }
            
            return defaultImage.get();

        }).orElseGet(defaultImage); // 4. default image

    }

    private static String url2id( String urlString ) {
        return urlString.replace("://", "/")
                .replaceFirst("file:/[A-Z]:/", "file/")
                .replaceFirst("\\?.*", "");
    }

    private static Image getFromClasspath(String urlString, Supplier<Image> defaultImage) {

        InputStream classpathInputStream = ImageCache.class.getResourceAsStream("/" + url2id(urlString));
        if (classpathInputStream != null) {
            Image image = new Image(classpathInputStream);
            return image.isError() ? defaultImage.get() : image;
        }
        return defaultImage.get();
    }

    private static Image getFromFileCache(String urlString, Supplier<Image> defaultImage) {

        return imageStore.map(store -> {

            File cached = new File(store, url2id(urlString));
            if (cached.exists()) {
                if (cached.length() > MAX_FILE_LENGTH) {
                    if (DevoxxLogging.LOGGING_ENABLED) {
                        LOG.log(Level.WARNING, "Not loading image: " + urlString + "with size: " + cached.length());
                    }
                    return defaultImage.get();
                }
                try (FileInputStream fileInputStream = new FileInputStream(cached)) {
                    Image image = new Image(fileInputStream);//, imageScaleWidth, imageScaleHeight, true, false);
                    return image.isError() ? null : image;
                } catch (Throwable ex) {
                    LOG.log(Level.SEVERE, null, ex);

                }
            }
            // don't return a default image, otherwise it will always default to that instead of trying to download it
            return null;

        }).orElseGet(() -> null);
    }

    private static void storeInFileCache(String urlString) {
        new Thread(() -> 
            imageStore.ifPresent(store -> {
                if (DevoxxLogging.LOGGING_ENABLED) {
                    LOG.log(Level.INFO, "String image " + urlString);
                }
                String id = url2id(urlString);
                try (InputStream is = new URL(urlString).openStream()) {
                    final File imageFile = new File(store, id);
                    imageFile.getParentFile().mkdirs();
                    try (OutputStream os = new FileOutputStream(imageFile)) {
                        byte[] b = new byte[BUFFER_SIZE];
                        int l = is.read(b);
                        while (l > 0) {
                            os.write(b, 0, l);
                            l = is.read(b);
                        }
                    }

                } catch (IOException ex) {
                    LOG.log(Level.SEVERE, "Wrong URL for " + id, ex);
                }
            })
        ).start();
    }

}
