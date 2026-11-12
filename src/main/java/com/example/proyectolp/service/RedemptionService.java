package com.example.proyectolp.service;

import com.example.proyectolp.model.RedemptionResult;
import com.example.proyectolp.repository.RedemptionRepository;
import java.net.ConnectException;
import java.util.concurrent.atomic.AtomicBoolean;

/** Orquesta el canje; el costo y el saldo definitivos los valida el servidor. */
public final class RedemptionService {
    @FunctionalInterface public interface RedeemOperation { RedemptionResult redeem(long productId,String requestId) throws Exception; }
    private final RedeemOperation operation;
    private final AtomicBoolean inFlight = new AtomicBoolean();

    public RedemptionService(SupabaseService db) {
        RedemptionRepository redemptions = new RedemptionRepository(db);
        operation = redemptions::redeem;
    }
    public RedemptionService(RedeemOperation operation) { this.operation=operation; }

    public RedemptionResult redeem(long productId, int cost, int knownBalance, String requestId) throws Exception {
        if (!inFlight.compareAndSet(false, true)) throw new RedemptionException("El canje ya está en proceso.");
        try {
            if (!RedemptionRules.canPay(knownBalance, cost))
                throw new RedemptionException("No tienes créditos suficientes: te faltan " + RedemptionRules.missing(knownBalance, cost) + ".");
            return operation.redeem(productId, requestId);
        } catch (Exception e) {
            throw translate(e);
        } finally { inFlight.set(false); }
    }

    private RedemptionException translate(Exception e) {
        String details = e.getMessage() == null ? "" : e.getMessage().toLowerCase(java.util.Locale.ROOT);
        if (e instanceof SupabaseException se && se.getBody() != null) details += " " + se.getBody().toLowerCase(java.util.Locale.ROOT);
        if (details.contains("insufficient_credits")) {
            java.util.regex.Matcher missing = java.util.regex.Pattern.compile("\\\"details\\\"\\s*:\\s*\\\"(\\d+)\\\"").matcher(details);
            return missing.find()
                    ? new RedemptionException("No tienes créditos suficientes: te faltan " + missing.group(1) + ".", e)
                    : new RedemptionException("No tienes créditos suficientes. Actualiza tu saldo e inténtalo de nuevo.", e);
        }
        if (details.contains("product_unavailable")) return new RedemptionException("Este producto ya no está disponible.", e);
        if (details.contains("401") || details.contains("sesión expiró") || details.contains("sesión cambió")) return new RedemptionException("Tu sesión expiró. Inicia sesión de nuevo.", e);
        if (e instanceof ConnectException || e instanceof java.io.IOException || e instanceof java.net.http.HttpTimeoutException || details.contains("connection") || details.contains("conexión")) return new RedemptionException("No hay conexión. Puedes reintentar el mismo canje.", e);
        if (e instanceof RedemptionException re) return re;
        return new RedemptionException("No se pudo completar el canje. Inténtalo de nuevo con el mismo código de solicitud.", e);
    }

    public static final class RedemptionException extends Exception {
        public RedemptionException(String message) { super(message); }
        public RedemptionException(String message, Throwable cause) { super(message, cause); }
    }
}
