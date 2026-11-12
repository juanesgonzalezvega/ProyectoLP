package com.example.proyectolp.service;

public class AuthException extends Exception {
    private final String errorCode;
    private final int statusCode;
    public AuthException(String message) { this(message, "", -1); }
    private AuthException(String message, String errorCode) { this(message, errorCode, -1); }
    private AuthException(String message, String errorCode, int statusCode) { super(message); this.errorCode = errorCode; this.statusCode = statusCode; }
    public String errorCode() { return errorCode; }
    public int statusCode() { return statusCode; }
    public boolean isInvalidRecoveryCode() { return "otp_expired".equals(errorCode) || "invalid_otp".equals(errorCode) || "token_not_found".equals(errorCode); }
    public static AuthException from(int status, String response) {
        String text = response == null ? "" : response.toLowerCase(java.util.Locale.ROOT);
        String code = extractCode(response);
        if ("otp_expired".equals(code) || "invalid_otp".equals(code) || "token_not_found".equals(code)
                || text.contains("token has expired or is invalid") || text.contains("invalid otp")
                || text.contains("email link is invalid or has expired"))
            return new AuthException("El c\u00f3digo es incorrecto o ya venci\u00f3. Solicita uno nuevo.", code.isBlank() ? "invalid_otp" : code, status);
        if ("same_password".equals(code)) return new AuthException("La nueva contrase\u00f1a debe ser distinta de la anterior.", code, status);
        if ("reauthentication_needed".equals(code)) return new AuthException("Por seguridad, solicita un nuevo c\u00f3digo e int\u00e9ntalo de nuevo.", code, status);
        if ("email_address_not_authorized".equals(code) || text.contains("email address not authorized"))
            return new AuthException("El servicio de correo del proyecto no puede enviar a esta direcci\u00f3n. Configura SMTP personalizado en Supabase.", code, status);
        if ("over_email_send_rate_limit".equals(code) || (status == 429 && !code.isBlank()))
            return new AuthException("Se alcanz\u00f3 el l\u00edmite de correos. Espera unos minutos e int\u00e9ntalo de nuevo.", code, status);
        if (text.contains("rate limit") || text.contains("too many") || (status == 429 && code.isBlank())) return new AuthException("Demasiados intentos. Espera un momento e inténtalo de nuevo.", code, status);
        if (text.contains("email_not_confirmed") || text.contains("email not confirmed")) return new AuthException("Tu correo aún no está confirmado. Revisa tu bandeja de entrada.", code, status);
        if (text.contains("already registered") || text.contains("user already registered") || text.contains("already been registered") || text.contains("email_exists") || text.contains("user_already_exists")) return new AuthException("Ese correo ya está registrado.", code, status);
        if (text.contains("weak_password") || text.contains("password should") || text.contains("password is too short")) return new AuthException("La contraseña es débil. Usa al menos 8 caracteres.", code, status);
        if (text.contains("invalid_login_credentials") || text.contains("invalid_credentials") || text.contains("invalid credentials") || text.contains("invalid login credentials")) return new AuthException("Correo o contraseña incorrectos.", code, status);
        if (status == 401) return new AuthException("Correo o contraseña incorrectos.", code, status);
        return new AuthException("No se pudo completar la autenticación. Verifica tus datos e inténtalo de nuevo.", code, status);
    }
    private static String extractCode(String response) {
        if (response == null || response.isBlank()) return "";
        try {
            com.fasterxml.jackson.databind.JsonNode node = new com.fasterxml.jackson.databind.ObjectMapper().readTree(response);
            String code = node.path("code").asText("");
            if (code.isBlank()) code = node.path("error_code").asText("");
            return code.toLowerCase(java.util.Locale.ROOT);
        } catch (Exception ignored) { return ""; }
    }
    public static AuthException offline() { return new AuthException("No hay conexión con el servicio. Revisa internet e inténtalo de nuevo."); }
}
