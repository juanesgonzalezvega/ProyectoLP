package com.example.proyectolp;

import com.example.proyectolp.config.AppConfig;
import com.example.proyectolp.model.Session;
import com.example.proyectolp.service.HttpTransport;
import com.example.proyectolp.service.SessionManager;
import com.example.proyectolp.service.SupabaseException;
import com.example.proyectolp.service.SupabaseService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class SupabaseStorageTransportTest {
    @TempDir Path temp;

    @Test void uploadStreamsPrivateObjectWithSessionHeadersAndNoUpsert() throws Exception {
        SessionManager sessions = new SessionManager(Clock.systemUTC());
        sessions.set(new Session("jwt", "refresh", Instant.now().plusSeconds(3600), "12345678-1234-1234-1234-123456789012", "", ""));
        AtomicReference<URI> uri = new AtomicReference<>();
        AtomicReference<Map<String, String>> headers = new AtomicReference<>();
        HttpTransport transport = new HttpTransport() {
            @Override public Response send(String m, URI u, Map<String,String> h, String b) { return new Response(200, "{}"); }
            @Override public Response sendFile(String m, URI u, Map<String,String> h, Path file, Duration timeout) {
                uri.set(u); headers.set(h); assertEquals("POST", m); assertEquals(Duration.ofMinutes(10), timeout); assertTrue(Files.isRegularFile(file)); return new Response(200, "{}");
            }
        };
        Path file = temp.resolve("f.png"); Files.write(file, new byte[]{1});
        new SupabaseService(new AppConfig("https://example.supabase.co", "anon"), transport, sessions, null)
                .storageUpload("12345678-1234-1234-1234-123456789012/task/1/uuid.png", file, "image/png");
        assertEquals("https://example.supabase.co/storage/v1/object/attachments/12345678-1234-1234-1234-123456789012/task/1/uuid.png", uri.get().toString());
        assertEquals("anon", headers.get().get("apikey"));
        assertEquals("Bearer jwt", headers.get().get("Authorization"));
        assertEquals("image/png", headers.get().get("Content-Type"));
        assertEquals("false", headers.get().get("x-upsert"));
    }

    @Test void mapsTooLargeAndTreatsMissingDeleteAsSuccess() throws Exception {
        SessionManager sessions = new SessionManager(Clock.systemUTC());
        sessions.set(new Session("jwt", "refresh", Instant.now().plusSeconds(3600), "uid", "", ""));
        HttpTransport transport = new HttpTransport() {
            @Override public Response send(String m, URI u, Map<String,String> h, String b) { return new Response(m.equals("DELETE") ? 404 : 413, "{}"); }
            @Override public Response sendFile(String m, URI u, Map<String,String> h, Path file, Duration timeout) { return new Response(413, "{}"); }
        };
        SupabaseService db = new SupabaseService(new AppConfig("https://example.supabase.co", "anon"), transport, sessions, null);
        Path file = temp.resolve("file.png"); Files.write(file, new byte[]{1});
        SupabaseException error = assertThrows(SupabaseException.class, () -> db.storageUpload("uid/task/1/a.png", file, "image/png"));
        assertTrue(error.getMessage().contains("supera el tamaño"));
        assertDoesNotThrow(() -> db.storageDelete("uid/task/1/missing.png"));
    }
}
