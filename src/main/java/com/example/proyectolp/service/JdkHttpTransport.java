package com.example.proyectolp.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

public final class JdkHttpTransport implements HttpTransport {
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    @Override public Response send(String method, URI uri, Map<String, String> headers, String body) throws IOException, InterruptedException {
        HttpRequest.Builder request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(20));
        headers.forEach(request::header);
        request.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        HttpResponse<String> response = client.send(request.build(), HttpResponse.BodyHandlers.ofString());
        return new Response(response.statusCode(), response.body());
    }

    @Override public Response sendFile(String method, URI uri, Map<String, String> headers, Path file, Duration timeout) throws IOException, InterruptedException {
        HttpRequest.Builder request = HttpRequest.newBuilder(uri).timeout(timeout);
        headers.forEach(request::header);
        request.method(method, HttpRequest.BodyPublishers.ofFile(file));
        HttpResponse<String> response = client.send(request.build(), HttpResponse.BodyHandlers.ofString());
        return new Response(response.statusCode(), response.body());
    }

    @Override public Response downloadFile(String method, URI uri, Map<String, String> headers, Path destination, Duration timeout) throws IOException, InterruptedException {
        HttpRequest.Builder request = HttpRequest.newBuilder(uri).timeout(timeout);
        headers.forEach(request::header);
        request.method(method, HttpRequest.BodyPublishers.noBody());
        HttpResponse<Path> response = client.send(request.build(), HttpResponse.BodyHandlers.ofFile(destination));
        if (response.statusCode() < 200 || response.statusCode() >= 300) java.nio.file.Files.deleteIfExists(destination);
        return new Response(response.statusCode(), "");
    }
}
