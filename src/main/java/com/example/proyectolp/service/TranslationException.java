package com.example.proyectolp.service;

public class TranslationException extends Exception {
    public enum Kind { QUOTA, NETWORK, TIMEOUT, INVALID_RESPONSE, OTHER }
    private final Kind kind;
    public TranslationException(Kind kind,String message){super(message);this.kind=kind;}
    public TranslationException(Kind kind,String message,Throwable cause){super(message,cause);this.kind=kind;}
    public Kind kind(){return kind;}
}
