package com.example.proyectolp.service;

public class ZenQuotesException extends Exception {
    public enum Kind { RATE_LIMIT, NETWORK, TIMEOUT, HTTP, INVALID_JSON, NO_DATA }
    private final Kind kind;
    public ZenQuotesException(Kind kind, String message) { super(message); this.kind=kind; }
    public ZenQuotesException(Kind kind, String message, Throwable cause) { super(message,cause);this.kind=kind; }
    public Kind kind() { return kind; }
}
