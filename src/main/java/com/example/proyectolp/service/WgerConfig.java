package com.example.proyectolp.service;

import java.io.InputStream;
import java.util.Properties;

public final class WgerConfig {
    public static final String DEFAULT_BASE_URL = "https://wger.de/api/v2";
    private WgerConfig() { }
    public static String baseUrl() {
        Properties properties = new Properties();
        try (InputStream in = WgerConfig.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (in != null) properties.load(in);
        } catch (Exception ignored) { }
        return properties.getProperty("wger.base.url", DEFAULT_BASE_URL).trim().replaceAll("/+$", "");
    }
}
