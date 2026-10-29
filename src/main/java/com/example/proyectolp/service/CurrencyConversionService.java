package com.example.proyectolp.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class CurrencyConversionService {
    private CurrencyConversionService() { }
    public static BigDecimal copToUsd(BigDecimal cop, BigDecimal rate) { validate(rate); return cop.divide(rate, 2, RoundingMode.HALF_UP); }
    public static BigDecimal usdToCop(BigDecimal usd, BigDecimal rate) { validate(rate); return usd.multiply(rate).setScale(2, RoundingMode.HALF_UP); }
    private static void validate(BigDecimal rate) { if (rate == null || rate.signum() <= 0) throw new IllegalArgumentException("La TRM debe ser mayor que cero."); }
}
