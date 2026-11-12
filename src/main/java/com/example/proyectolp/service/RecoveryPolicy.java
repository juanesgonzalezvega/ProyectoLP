package com.example.proyectolp.service;

/** Límites locales para mitigar abusos del flujo de recuperación. */
public final class RecoveryPolicy {
    /** true muestra el estado de existencia; puede revelar correos registrados. */
    public static final boolean REVEAL_ACCOUNT_EXISTENCE = true;
    public static final int MAX_EXISTENCE_CHECKS = 5;
    public static final long EXISTENCE_WINDOW_SECONDS = 600;
    public static final long RESEND_INTERVAL_SECONDS = 60;
    /** Máximo local de correos por cuenta en una hora. */
    public static final int MAX_RECOVERY_EMAILS_PER_HOUR = 5;
    public static final long RECOVERY_EMAIL_WINDOW_SECONDS = 3600;
    public static final int MAX_CODE_ATTEMPTS = 5;
    private RecoveryPolicy() {}
}
