package com.example.proyectolp.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;

public final class TrmCache {
    private static final ObjectMapper JSON = new ObjectMapper().registerModule(new JavaTimeModule());
    private final Path file;
    private final Clock clock;
    public TrmCache() { this(Path.of(System.getProperty("user.home"), ".proyectolp", "trm-cache.json"), Clock.system(TrmSelector.ZONE)); }
    public TrmCache(Path directory, Clock clock) { this.file=directory.getFileName()!=null&&directory.getFileName().toString().endsWith(".json")?directory:directory.resolve("trm-cache.json");this.clock=clock.withZone(TrmSelector.ZONE); }
    public TrmClient.TrmQuote read() {
        try { Entry e=JSON.readValue(file.toFile(),Entry.class); if(e!=null&&e.rate!=null&&e.validFrom!=null&&e.validTo!=null&&e.fetchedAt!=null)
            return new TrmClient.TrmQuote(e.rate,e.validFrom,e.validTo,e.fetchedAt,TrmClient.Origin.CACHED); }
        catch(Exception ignored) { }
        return null;
    }
    public TrmClient.TrmQuote readValidToday() {
        TrmClient.TrmQuote q=read(); LocalDate today=LocalDate.now(clock);
        return q!=null&&q.fetchedAt().atZone(TrmSelector.ZONE).toLocalDate().equals(today)&&!today.isBefore(q.validFrom())&&!today.isAfter(q.validTo())?q:null;
    }
    public void write(TrmClient.TrmQuote quote) throws IOException {
        Files.createDirectories(file.getParent()); Entry e=new Entry();e.rate=quote.rate();e.validFrom=quote.validFrom();e.validTo=quote.validTo();e.fetchedAt=quote.fetchedAt();JSON.writeValue(file.toFile(),e);
    }
    @JsonIgnoreProperties(ignoreUnknown=true) public static final class Entry { public java.math.BigDecimal rate; public LocalDate validFrom,validTo; public java.time.Instant fetchedAt; }
}
