package com.example.proyectolp.service;

public class SupabaseException extends Exception {
    private final int statusCode;
    private final String body;
    public SupabaseException(int statusCode, String body) {
        super("Supabase respondió HTTP " + statusCode + (body == null || body.isBlank() ? "" : ": " + body));
        this.statusCode = statusCode;
        this.body = body;
    }
    public int getStatusCode() { return statusCode; }
    public String getBody() { return body; }
}
