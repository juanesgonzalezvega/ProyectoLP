package com.example.proyectolp.model;

import java.time.LocalDate;

public record DailyQuote(String text, String author, LocalDate date, Origin origin,
                         String translatedText, TranslationStatus translationStatus) {
    public enum Origin { FRESH, CACHED, STALE }
    public enum TranslationStatus { TRANSLATED, ORIGINAL_ONLY }
    public DailyQuote(String text,String author,LocalDate date,Origin origin){this(text,author,date,origin,null,null);}
    public String displayText(){return translationStatus==TranslationStatus.TRANSLATED&&translatedText!=null?translatedText:text;}
}
