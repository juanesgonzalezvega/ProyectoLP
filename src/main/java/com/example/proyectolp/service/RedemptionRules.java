package com.example.proyectolp.service;

import java.net.URI;

public final class RedemptionRules {
    private RedemptionRules() {}
    public static int copValue(int credits) { return Math.multiplyExact(credits, CreditRules.COP_PER_CREDIT); }
    public static int missing(int balance, int cost) { return Math.max(0, cost - balance); }
    public static int balanceAfter(int balance, int cost) { return Math.subtractExact(balance, cost); }
    public static boolean canPay(int balance, int cost) { return cost > 0 && balance >= cost; }
    public static boolean validWebsite(String value) {
        if (value == null || value.isBlank()) return false;
        try {
            URI uri = URI.create(value);
            return "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null
                    && !uri.getHost().isBlank() && uri.getRawUserInfo() == null;
        } catch (IllegalArgumentException e) { return false; }
    }
}
