package com.example.proyectolp.service;

import java.util.function.IntConsumer;

public interface QuoteTranslator {
    String translate(String text) throws TranslationException;
    default int dailyCharacterLimit(){return 5000;}
    default String translate(String text,int remaining,IntConsumer onCharactersSent)throws TranslationException{
        if(text.length()>remaining)throw new TranslationException(TranslationException.Kind.QUOTA,"Se agotó la cuota diaria de traducción.");
        onCharactersSent.accept(text.length());return translate(text);
    }
}
