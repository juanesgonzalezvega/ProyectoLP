package com.example.proyectolp.service;

import java.time.Clock;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

/** Límites en memoria por proceso; no sustituye los límites de Supabase. */
public final class RecoveryThrottle {
    public static final RecoveryThrottle SHARED = new RecoveryThrottle();
    private final Clock clock;
    private final ArrayDeque<Long> existenceChecks = new ArrayDeque<>();
    private final Map<String, Long> lastSend = new HashMap<>();
    private final Map<String, ArrayDeque<Long>> recoverySends = new HashMap<>();
    private final Map<String, Integer> codeAttempts = new HashMap<>();

    public RecoveryThrottle() { this(Clock.systemUTC()); }
    public RecoveryThrottle(Clock clock) { this.clock = clock; }

    public synchronized boolean tryAcquireExistenceCheck() {
        long now = clock.instant().getEpochSecond();
        purgeExistence(now);
        if (existenceChecks.size() >= RecoveryPolicy.MAX_EXISTENCE_CHECKS) return false;
        existenceChecks.addLast(now);
        return true;
    }

    public synchronized boolean tryAcquireSend(String email) {
        String key = normalize(email);
        long now = clock.instant().getEpochSecond();
        ArrayDeque<Long> sends = recoverySends.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        purgeSends(sends, now);
        if (sends.size() >= RecoveryPolicy.MAX_RECOVERY_EMAILS_PER_HOUR) return false;
        Long previous = lastSend.get(key);
        if (previous != null && now - previous < RecoveryPolicy.RESEND_INTERVAL_SECONDS) return false;
        lastSend.put(key, now);
        sends.addLast(now);
        codeAttempts.remove(key);
        return true;
    }

    public synchronized long resendSeconds(String email) {
        String key = normalize(email);
        Long previous = lastSend.get(key);
        ArrayDeque<Long> sends = recoverySends.get(key);
        if (sends != null) {
            long now = clock.instant().getEpochSecond();
            purgeSends(sends, now);
            if (sends.size() >= RecoveryPolicy.MAX_RECOVERY_EMAILS_PER_HOUR)
                return Math.max(0, RecoveryPolicy.RECOVERY_EMAIL_WINDOW_SECONDS - (now - sends.peekFirst()));
        }
        if (previous == null) return 0;
        return Math.max(0, RecoveryPolicy.RESEND_INTERVAL_SECONDS - (clock.instant().getEpochSecond() - previous));
    }

    public synchronized long recoveryEmailLimitWaitSeconds(String email) {
        ArrayDeque<Long> sends = recoverySends.get(normalize(email));
        if (sends == null) return 0;
        long now = clock.instant().getEpochSecond();
        purgeSends(sends, now);
        return sends.size() < RecoveryPolicy.MAX_RECOVERY_EMAILS_PER_HOUR
                ? 0
                : Math.max(0, RecoveryPolicy.RECOVERY_EMAIL_WINDOW_SECONDS - (now - sends.peekFirst()));
    }

    public synchronized int recordInvalidCode(String email) {
        String key = normalize(email);
        int attempts = codeAttempts.getOrDefault(key, 0) + 1;
        codeAttempts.put(key, attempts);
        return attempts;
    }

    public synchronized boolean isCodeLocked(String email) {
        return codeAttempts.getOrDefault(normalize(email), 0) >= RecoveryPolicy.MAX_CODE_ATTEMPTS;
    }

    public synchronized int codeAttempts(String email) { return codeAttempts.getOrDefault(normalize(email), 0); }
    public synchronized void clearCodeAttempts(String email) { codeAttempts.remove(normalize(email)); }

    private void purgeExistence(long now) {
        while (!existenceChecks.isEmpty() && now - existenceChecks.peekFirst() >= RecoveryPolicy.EXISTENCE_WINDOW_SECONDS) existenceChecks.removeFirst();
    }
    private static void purgeSends(ArrayDeque<Long> sends, long now) {
        while (!sends.isEmpty() && now - sends.peekFirst() >= RecoveryPolicy.RECOVERY_EMAIL_WINDOW_SECONDS) sends.removeFirst();
    }
    private static String normalize(String email) { return email == null ? "" : email.trim().toLowerCase(java.util.Locale.ROOT); }
}
