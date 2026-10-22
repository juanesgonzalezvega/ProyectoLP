package com.example.proyectolp.service;

public class AuthException extends Exception {
    public AuthException(String message) { super(message); }
    public static AuthException from(int status, String response) {
        String text = response == null ? "" : response.toLowerCase(java.util.Locale.ROOT);
        if (status == 429 || text.contains("rate limit") || text.contains("too many")) return new AuthException("Demasiados intentos. Espera un momento e inténtalo de nuevo.");
        if (text.contains("email_not_confirmed") || text.contains("email not confirmed")) return new AuthException("Tu correo aún no está confirmado. Revisa tu bandeja de entrada.");
        if (text.contains("already registered") || text.contains("user already registered") || text.contains("already been registered") || text.contains("email_exists") || text.contains("user_already_exists")) return new AuthException("Ese correo ya está registrado.");
        if (text.contains("weak_password") || text.contains("password should") || text.contains("password is too short")) return new AuthException("La contraseña es débil. Usa al menos 8 caracteres.");
        if (text.contains("invalid_login_credentials") || text.contains("invalid_credentials") || text.contains("invalid credentials") || text.contains("invalid login credentials")) return new AuthException("Correo o contraseña incorrectos.");
        if (status == 401) return new AuthException("Correo o contraseña incorrectos.");
        return new AuthException("No se pudo completar la autenticación. Verifica tus datos e inténtalo de nuevo.");
    }
    public static AuthException offline() { return new AuthException("No hay conexión con el servicio. Revisa internet e inténtalo de nuevo."); }
}
