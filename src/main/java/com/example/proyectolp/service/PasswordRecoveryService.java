package com.example.proyectolp.service;

import com.example.proyectolp.model.Session;
import com.example.proyectolp.model.enums.AccountStatus;
import java.util.Objects;

/** Orquesta el envío, la verificación y el cambio de clave. */
public final class PasswordRecoveryService {
    public enum StartStatus { NOT_FOUND, UNVERIFIED, CODE_SENT, GENERIC_SENT, THROTTLED, RESEND_WAIT, EMAIL_LIMIT }
    public enum CompleteStatus { SUCCESS, INVALID_CODE, LOCKED }
    public record StartResult(StartStatus status, long waitSeconds) {}

    private final AuthService auth;
    private final RecoveryThrottle throttle;
    private final boolean revealAccountExistence;

    public PasswordRecoveryService(AuthService auth, RecoveryThrottle throttle) {
        this(auth, throttle, RecoveryPolicy.REVEAL_ACCOUNT_EXISTENCE);
    }
    public PasswordRecoveryService(AuthService auth, RecoveryThrottle throttle, boolean revealAccountExistence) {
        this.auth = Objects.requireNonNull(auth);
        this.throttle = Objects.requireNonNull(throttle);
        this.revealAccountExistence = revealAccountExistence;
    }

    public StartResult start(String email) throws AuthException {
        String normalized = normalize(email);
        if (revealAccountExistence) {
            if (!throttle.tryAcquireExistenceCheck()) return new StartResult(StartStatus.THROTTLED, 0);
            AccountStatus status = auth.accountStatus(normalized);
            if (status == AccountStatus.NOT_FOUND) return new StartResult(StartStatus.NOT_FOUND, 0);
            if (status == AccountStatus.UNVERIFIED) return new StartResult(StartStatus.UNVERIFIED, 0);
        }
        long emailLimitWait = throttle.recoveryEmailLimitWaitSeconds(normalized);
        if (emailLimitWait > 0) return new StartResult(StartStatus.EMAIL_LIMIT, emailLimitWait);
        long wait = throttle.resendSeconds(normalized);
        if (wait > 0) return new StartResult(StartStatus.RESEND_WAIT, wait);
        if (!throttle.tryAcquireSend(normalized)) return new StartResult(StartStatus.RESEND_WAIT, throttle.resendSeconds(normalized));
        auth.sendRecoveryCode(normalized);
        return new StartResult(revealAccountExistence ? StartStatus.CODE_SENT : StartStatus.GENERIC_SENT, 0);
    }

    public CompleteStatus complete(String email, String code, String newPassword) throws AuthException {
        String normalized = normalize(email);
        if (throttle.isCodeLocked(normalized)) return CompleteStatus.LOCKED;
        Session temporary;
        try {
            temporary = auth.verifyRecoveryCode(normalized, code);
        } catch (AuthException error) {
            if (!error.isInvalidRecoveryCode()) throw error;
            int attempts = throttle.recordInvalidCode(normalized);
            return attempts >= RecoveryPolicy.MAX_CODE_ATTEMPTS ? CompleteStatus.LOCKED : CompleteStatus.INVALID_CODE;
        }
        try {
            auth.updatePassword(temporary, newPassword);
            throttle.clearCodeAttempts(normalized);
            return CompleteStatus.SUCCESS;
        } finally {
            auth.revoke(temporary);
        }
    }

    public long resendSeconds(String email) { return throttle.resendSeconds(email); }
    public int codeAttempts(String email) { return throttle.codeAttempts(email); }
    private static String normalize(String email) { return email.trim().toLowerCase(java.util.Locale.ROOT); }
}
