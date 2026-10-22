package com.example.proyectolp.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

/** Guarda únicamente el refresh token fuera del repositorio. */
public class SessionStorage {
    private final Path path;
    public SessionStorage() { this(Path.of(System.getProperty("user.home"), ".proyectolp", "session.refresh")); }
    public SessionStorage(Path path) { this.path = path; }
    public synchronized void save(String refreshToken) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, refreshToken, StandardCharsets.UTF_8);
        try { Files.setPosixFilePermissions(path, Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE)); }
        catch (UnsupportedOperationException | IOException ignored) { path.toFile().setReadable(false, false); path.toFile().setWritable(false, false); path.toFile().setReadable(true, true); path.toFile().setWritable(true, true); }
    }
    public synchronized String load() throws IOException { return Files.isRegularFile(path) ? Files.readString(path, StandardCharsets.UTF_8).trim() : null; }
    public synchronized void clear() throws IOException { Files.deleteIfExists(path); }
    public synchronized void clearQuietly(){try{clear();}catch(IOException ignored){}}
}
