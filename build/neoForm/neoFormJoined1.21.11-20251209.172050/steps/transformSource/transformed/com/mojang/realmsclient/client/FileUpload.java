package com.mojang.realmsclient.client;

import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.dto.UploadInfo;
import com.mojang.realmsclient.gui.screens.UploadResult;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.minecraft.client.User;
import net.minecraft.util.LenientJsonParser;
import net.minecraft.util.Util;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.commons.io.input.CountingInputStream;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class FileUpload implements AutoCloseable {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAX_RETRIES = 5;
    private static final String UPLOAD_PATH = "/upload";
    private final File file;
    private final long realmId;
    private final int slotId;
    private final UploadInfo uploadInfo;
    private final String sessionId;
    private final String username;
    private final String clientVersion;
    private final String worldVersion;
    private final UploadStatus uploadStatus;
    private final HttpClient client;

    public FileUpload(File file, long realmId, int slotId, UploadInfo uploadInfo, User user, String clientVersion, String worldVersion, UploadStatus uploadStatus) {
        this.file = file;
        this.realmId = realmId;
        this.slotId = slotId;
        this.uploadInfo = uploadInfo;
        this.sessionId = user.getSessionId();
        this.username = user.getName();
        this.clientVersion = clientVersion;
        this.worldVersion = worldVersion;
        this.uploadStatus = uploadStatus;
        this.client = HttpClient.newBuilder().executor(Util.nonCriticalIoPool()).connectTimeout(Duration.ofSeconds(15L)).build();
    }

    @Override
    public void close() {
        this.client.close();
    }

    public CompletableFuture<UploadResult> startUpload() {
        long i = this.file.length();
        this.uploadStatus.setTotalBytes(i);
        return this.requestUpload(0, i);
    }

    private CompletableFuture<UploadResult> requestUpload(int retries, long size) {
        BodyPublisher bodypublisher = inputStreamPublisherWithSize(() -> {
            try {
                return new FileUpload.UploadCountingInputStream(new FileInputStream(this.file), this.uploadStatus);
            } catch (IOException ioexception) {
                LOGGER.warn("Failed to open file {}", this.file, ioexception);
                return null;
            }
        }, size);
        HttpRequest httprequest = HttpRequest.newBuilder(this.uploadInfo.uploadEndpoint().resolve("/upload/" + this.realmId + "/" + this.slotId))
            .timeout(Duration.ofMinutes(10L))
            .setHeader("Cookie", this.uploadCookie())
            .setHeader("Content-Type", "application/octet-stream")
            .POST(bodypublisher)
            .build();
        return this.client.sendAsync(httprequest, BodyHandlers.ofString(StandardCharsets.UTF_8)).thenCompose(p_460220_ -> {
            long i = this.getRetryDelaySeconds((HttpResponse<?>)p_460220_);
            if (this.shouldRetry(i, retries)) {
                this.uploadStatus.restart();

                try {
                    Thread.sleep(Duration.ofSeconds(i));
                } catch (InterruptedException interruptedexception) {
                }

                return this.requestUpload(retries + 1, size);
            } else {
                return CompletableFuture.completedFuture(this.handleResponse((HttpResponse<String>)p_460220_));
            }
        });
    }

    private static BodyPublisher inputStreamPublisherWithSize(Supplier<@Nullable InputStream> streamSupplier, long size) {
        return BodyPublishers.fromPublisher(BodyPublishers.ofInputStream(streamSupplier), size);
    }

    private String uploadCookie() {
        return "sid="
            + this.sessionId
            + ";token="
            + this.uploadInfo.token()
            + ";user="
            + this.username
            + ";version="
            + this.clientVersion
            + ";worldVersion="
            + this.worldVersion;
    }

    private UploadResult handleResponse(HttpResponse<String> response) {
        int i = response.statusCode();
        if (i == 401) {
            LOGGER.debug("Realms server returned 401: {}", response.headers().firstValue("WWW-Authenticate"));
        }

        String s = null;
        String s1 = response.body();
        if (s1 != null && !s1.isBlank()) {
            try {
                JsonElement jsonelement = LenientJsonParser.parse(s1).getAsJsonObject().get("errorMsg");
                if (jsonelement != null) {
                    s = jsonelement.getAsString();
                }
            } catch (Exception exception) {
                LOGGER.warn("Failed to parse response {}", s1, exception);
            }
        }

        return new UploadResult(i, s);
    }

    private boolean shouldRetry(long retryDelaySeconds, int retries) {
        return retryDelaySeconds > 0L && retries + 1 < 5;
    }

    private long getRetryDelaySeconds(HttpResponse<?> response) {
        return response.headers().firstValueAsLong("Retry-After").orElse(0L);
    }

    @OnlyIn(Dist.CLIENT)
    static class UploadCountingInputStream extends CountingInputStream {
        private final UploadStatus uploadStatus;

        UploadCountingInputStream(InputStream parent, UploadStatus uploadStatus) {
            super(parent);
            this.uploadStatus = uploadStatus;
        }

        @Override
        protected void afterRead(int bytesRead) throws IOException {
            super.afterRead(bytesRead);
            this.uploadStatus.onWrite(this.getByteCount());
        }
    }
}
