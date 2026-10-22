package com.example.proyectolp.service;

public class SupabaseException extends Exception {
    private final int statusCode;
    private final String body;
    public SupabaseException(int statusCode, String body) {
        super(statusCode == 401 ? "La sesión expiró o no es válida. Inicia sesión de nuevo."
                : statusCode == 403 ? "Acceso denegado por las políticas de seguridad (RLS)."
                : statusCode == 404 ? "No se encontró el recurso solicitado. Revisa el esquema y sus políticas."
                : statusCode >= 500 ? "Supabase no está disponible en este momento."
                : "Supabase rechazó la operación (HTTP " + statusCode + "). Revisa permisos y datos.");
        this.statusCode = statusCode;
        this.body = body;
    }
    public int getStatusCode() { return statusCode; }
    public String getBody() { return body; }
}
