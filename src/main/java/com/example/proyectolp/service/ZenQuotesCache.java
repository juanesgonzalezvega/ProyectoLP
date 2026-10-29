package com.example.proyectolp.service;

import com.example.proyectolp.model.DailyQuote;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Optional;

/** Conserva la frase pública en disco, sin asociarla a la sesión. */
public final class ZenQuotesCache {
    private static final ObjectMapper JSON=new ObjectMapper().registerModule(new JavaTimeModule());
    private final Path file;
    public ZenQuotesCache(){this(Path.of(System.getProperty("user.home"),".proyectolp"));}
    public ZenQuotesCache(Path path){this.file=path.getFileName()!=null&&path.getFileName().toString().endsWith(".json")?path:path.resolve("zenquotes-cache.json");}
    public synchronized Optional<CachedQuote> read(){try{Entry e=JSON.readValue(file.toFile(),Entry.class);if(e.text==null||e.text.isBlank()||e.date==null)return Optional.empty();return Optional.of(new CachedQuote(e.text,e.author==null?"":e.author,e.date,e.savedOn==null?e.date:e.savedOn,e.translatedText,e.translationStatus,e.quotaExhaustedDate,e.translationCharsDate,e.translationCharacters==null?0:e.translationCharacters));}catch(Exception ignored){return Optional.empty();}}
    public synchronized Optional<DailyQuote> readToday(LocalDate today){return read().filter(q->q.savedOn().equals(today)).map(CachedQuote::quoteCached);}
    public synchronized void write(DailyQuote quote,LocalDate savedOn)throws java.io.IOException{Entry e=readEntry();e.text=quote.text();e.author=quote.author();e.date=quote.date();e.savedOn=savedOn;e.translatedText=quote.translatedText();e.translationStatus=quote.translationStatus();save(e);}
    public synchronized void saveTranslation(String translated,LocalDate today)throws java.io.IOException{Entry e=readEntry();e.translatedText=translated;e.translationStatus=DailyQuote.TranslationStatus.TRANSLATED;save(e);}
    public synchronized void saveOriginalOnly(LocalDate today)throws java.io.IOException{Entry e=readEntry();e.translationStatus=DailyQuote.TranslationStatus.ORIGINAL_ONLY;save(e);}
    public synchronized void markQuotaExhausted(LocalDate today)throws java.io.IOException{Entry e=readEntry();e.quotaExhaustedDate=today;save(e);}
    public synchronized boolean quotaExhausted(LocalDate today){return read().map(q->today.equals(q.quotaExhaustedDate())).orElse(false);}
    public synchronized int translationCharacters(LocalDate today){return read().filter(q->today.equals(q.translationCharsDate())).map(CachedQuote::translationCharacters).orElse(0);}
    public synchronized void addTranslationCharacters(LocalDate today,int amount)throws java.io.IOException{Entry e=readEntry();if(!today.equals(e.translationCharsDate)){e.translationCharsDate=today;e.translationCharacters=0;}e.translationCharacters+=amount;save(e);}
    private Entry readEntry(){try{return JSON.readValue(file.toFile(),Entry.class);}catch(Exception ex){return new Entry();}}
    private void save(Entry entry)throws java.io.IOException{Files.createDirectories(file.getParent());Path tmp=Files.createTempFile(file.getParent(),"zenquotes-",".tmp");boolean moved=false;try{JSON.writeValue(tmp.toFile(),entry);try{Files.move(tmp,file,java.nio.file.StandardCopyOption.ATOMIC_MOVE,java.nio.file.StandardCopyOption.REPLACE_EXISTING);}catch(java.nio.file.AtomicMoveNotSupportedException ex){Files.move(tmp,file,java.nio.file.StandardCopyOption.REPLACE_EXISTING);}moved=true;}finally{if(!moved)Files.deleteIfExists(tmp);}}
    public record CachedQuote(String text,String author,LocalDate date,LocalDate savedOn,String translatedText,DailyQuote.TranslationStatus translationStatus,LocalDate quotaExhaustedDate,LocalDate translationCharsDate,int translationCharacters){
        public DailyQuote quoteCached(){return new DailyQuote(text,author,date,DailyQuote.Origin.CACHED,translatedText,translationStatus);}
        public DailyQuote asStale(){return new DailyQuote(text,author,date,DailyQuote.Origin.STALE,translatedText,translationStatus);}
    }
    @JsonIgnoreProperties(ignoreUnknown=true) public static final class Entry{public String text,author,translatedText;public LocalDate date,savedOn,quotaExhaustedDate,translationCharsDate;public DailyQuote.TranslationStatus translationStatus;public Integer translationCharacters;}
}
