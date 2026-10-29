package com.example.proyectolp.service;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

/** Transporte mínimo intercambiable para pruebas sin red. */
public interface HttpTransport {
    record Response(int statusCode, String body) {}
    Response send(String method, URI uri, Map<String, String> headers, String body) throws IOException, InterruptedException;

    default Response sendFile(String method, URI uri, Map<String, String> headers, Path file, Duration timeout) throws IOException, InterruptedException {
        throw new IOException("Este transporte no admite carga streaming de archivos.");
    }

    default Response downloadFile(String method, URI uri, Map<String, String> headers, Path destination, Duration timeout) throws IOException, InterruptedException {
        throw new IOException("Este transporte no admite descarga streaming de archivos.");
    }
}
