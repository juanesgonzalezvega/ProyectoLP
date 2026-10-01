package com.example.proyectolp.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public record AppConfig(String url, String key) {
    public static AppConfig load() throws IOException {
        Path file = Path.of("config.properties");
        if (!Files.isRegularFile(file)) throw new IOException("No existe config.properties en la raíz del proyecto. Cópialo desde config.example.properties y agrega tus datos.");
        Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(file)) { properties.load(in); }
        String url = properties.getProperty("supabase.url", "").trim();
        String key = properties.getProperty("supabase.key", "").trim();
        if (url.isEmpty() || key.isEmpty()) throw new IOException("Completa supabase.url y supabase.key en config.properties.");
        return new AppConfig(url.replaceAll("/+$", ""), key);
    }
}
