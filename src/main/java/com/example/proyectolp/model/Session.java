package com.example.proyectolp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Session(String accessToken, String refreshToken, Instant expiresAt,
                      String userId, String email, String displayName) {
    public static Session fromAuthResponse(JsonNode root, Instant now) {
        JsonNode user = root.path("user");
        if (user.isMissingNode() || user.isNull() || !root.hasNonNull("access_token")
                || !root.hasNonNull("refresh_token") || !user.hasNonNull("id")) return null;
        long expiry = root.path("expires_at").asLong(0);
        if (expiry <= 0) expiry = now.getEpochSecond() + Math.max(1, root.path("expires_in").asLong(3600));
        String name = user.path("user_metadata").path("display_name").asText("");
        if (name.isBlank()) name = user.path("user_metadata").path("name").asText("");
        return new Session(root.path("access_token").asText(), root.path("refresh_token").asText(),
                Instant.ofEpochSecond(expiry), user.path("id").asText(), user.path("email").asText(""), name);
    }
}
