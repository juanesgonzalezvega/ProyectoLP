package com.example.proyectolp;

import com.example.proyectolp.model.Attachment;
import com.example.proyectolp.service.AttachmentCache;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class AttachmentCacheAndJsonTest {
    @TempDir Path temp;

    @Test void cacheReusesDownloadsAndClearRemovesPrivateFiles() throws Exception {
        AttachmentCache cache = new AttachmentCache(temp);
        AtomicInteger downloads = new AtomicInteger();
        Path first = cache.getOrDownload("u/task/1/a.png", "png", path -> {
            downloads.incrementAndGet();
            Files.write(path, new byte[]{1, 2, 3});
        });
        Path second = cache.getOrDownload("u/task/1/a.png", "png", path -> downloads.incrementAndGet());
        assertEquals(first, second);
        assertEquals(1, downloads.get());
        cache.clear();
        assertFalse(Files.exists(first));
        cache.dispose();
    }

    @Test void attachmentUsesSnakeCaseAndIgnoresUnknownFields() throws Exception {
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        String json = "{\"id\":7,\"task_id\":3,\"project_id\":null,\"session_id\":null,\"file_name\":\"foto.png\",\"mime_type\":\"image/png\",\"kind\":\"imagen\",\"size_bytes\":12,\"storage_path\":\"private/path\",\"user_id\":\"private-user\",\"future_field\":true}";
        Attachment value = mapper.readValue(json, Attachment.class);
        assertEquals(3L, value.getTaskId());
        assertEquals("foto.png", value.getFileName());
        String serialized = mapper.writeValueAsString(value);
        assertTrue(serialized.contains("task_id"));
        assertFalse(serialized.contains("user_id"));
    }
}
