package com.example.proyectolp.service;

import com.example.proyectolp.config.AppConfig;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.List;

public class SupabaseService {
    private final AppConfig config;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    public SupabaseService(AppConfig config) { this.config = config; }

    public <T> T get(String path, Class<T> type) throws IOException, InterruptedException, SupabaseException { return read(send("GET", path, null, ""), type); }
    public <T> List<T> getList(String path, Class<T> type) throws IOException, InterruptedException, SupabaseException {
        JavaType listType = mapper.getTypeFactory().constructCollectionType(List.class, type);
        return mapper.readValue(send("GET", path, null, "").body(), listType);
    }
    public <T> T get(String path, TypeReference<T> type) throws IOException, InterruptedException, SupabaseException { return mapper.readValue(send("GET", path, null, "").body(), type); }
    public String post(String path, Object value, String prefer) throws IOException, InterruptedException, SupabaseException { return send("POST", path, json(value), prefer).body(); }
    public <T> List<T> postList(String path, Object value, String prefer, Class<T> type) throws IOException, InterruptedException, SupabaseException {
        JavaType listType = mapper.getTypeFactory().constructCollectionType(List.class, type);
        return mapper.readValue(send("POST", path, json(value), prefer).body(), listType);
    }
    public String patch(String path, Object value, String prefer) throws IOException, InterruptedException, SupabaseException { return send("PATCH", path, json(value), prefer).body(); }
    public String delete(String path) throws IOException, InterruptedException, SupabaseException { return send("DELETE", path, null, "").body(); }
    private <T> T read(HttpResponse<String> response, Class<T> type) throws IOException { return mapper.readValue(response.body(), type); }
    private String json(Object value) throws IOException { return mapper.writeValueAsString(value); }
    private HttpResponse<String> send(String method, String path, String body, String prefer) throws IOException, InterruptedException, SupabaseException {
        String route = path.startsWith("/") ? path.substring(1) : path;
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(config.url() + "/rest/v1/" + route))
                .timeout(Duration.ofSeconds(15)).header("apikey", config.key()).header("Accept", "application/json");
        if (body != null) builder.header("Content-Type", "application/json");
        if (!prefer.isBlank()) builder.header("Prefer", prefer);
        HttpRequest.BodyPublisher publisher = body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body);
        HttpResponse<String> response = client.send(builder.method(method, publisher).build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) throw new SupabaseException(response.statusCode(), response.body());
        return response;
    }
}
