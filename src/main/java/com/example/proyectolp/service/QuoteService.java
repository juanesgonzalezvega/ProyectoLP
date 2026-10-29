package com.example.proyectolp.service;

import com.example.proyectolp.model.DailyQuote;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** Orquesta caché, ZenQuotes y traducción con estado compartido. */
public final class QuoteService {
    private static final ZoneId ZONE=ZoneId.of("America/Bogota");
    private static final QuoteService INSTANCE=new QuoteService(new ZenQuotesClient(),new MyMemoryClient(),new ZenQuotesCache(),Clock.system(ZONE));
    private final ZenQuotesClient zen;private final QuoteTranslator translator;private final ZenQuotesCache cache;private final Clock clock;private final Duration translationTimeout;
    private final Deque<Instant> requests=new ArrayDeque<>();private CompletableFuture<Optional<DailyQuote>> inFlight;private Instant retryAfter;
    public static QuoteService getInstance(){return INSTANCE;}
    /** Compatibilidad para las pruebas y clientes anteriores al paso de traducción. */
    public QuoteService(ZenQuotesClient zen,ZenQuotesCache cache,Clock clock){this(zen,text->{throw new TranslationException(TranslationException.Kind.OTHER,"Traductor no configurado.");},cache,clock);}
    public QuoteService(ZenQuotesClient zen,QuoteTranslator translator,ZenQuotesCache cache,Clock clock){this(zen,translator,cache,clock,Duration.ofSeconds(8));}
    public QuoteService(ZenQuotesClient zen,QuoteTranslator translator,ZenQuotesCache cache,Clock clock,Duration translationTimeout){this.zen=zen;this.translator=translator;this.cache=cache;this.clock=clock.withZone(ZONE);this.translationTimeout=translationTimeout;}
    /** Devuelve la operación compartida para que los refrescos repetidos no dupliquen llamadas. */
    public CompletableFuture<Optional<DailyQuote>> todayAsync(Executor executor){
        LocalDate today=LocalDate.now(clock);Optional<ZenQuotesCache.CachedQuote> stored=cache.read();
        if(stored.isPresent()&&stored.get().savedOn().equals(today)){
            ZenQuotesCache.CachedQuote q=stored.get();
            if(q.translationStatus()==DailyQuote.TranslationStatus.TRANSLATED&&q.translatedText()!=null)return CompletableFuture.completedFuture(Optional.of(q.quoteCached()));
            if(cache.quotaExhausted(today))return CompletableFuture.completedFuture(Optional.of(originalOnly(q.quoteCached())));
        }
        synchronized(this){
            if(inFlight!=null)return inFlight;
            Instant now=clock.instant();if(retryAfter!=null&&now.isBefore(retryAfter))return CompletableFuture.completedFuture(fallback(today));
            while(!requests.isEmpty()&&Duration.between(requests.peekFirst(),now).compareTo(Duration.ofSeconds(30))>=0)requests.removeFirst();
            if(requests.size()>=5){retryAfter=requests.peekFirst().plusSeconds(30);return CompletableFuture.completedFuture(fallback(today));}
            requests.addLast(now);inFlight=new CompletableFuture<>();CompletableFuture<Optional<DailyQuote>> result=inFlight;
            try{executor.execute(()->runFlow(result,today,stored,executor));}catch(RuntimeException ex){retryAfter=now.plus(Duration.ofMinutes(5));result.complete(fallback(today));inFlight=null;}
            return result;
        }
    }
    private void runFlow(CompletableFuture<Optional<DailyQuote>> result,LocalDate today,Optional<ZenQuotesCache.CachedQuote> stored,Executor executor){
        try{
            DailyQuote source;
            if(stored.isPresent()&&stored.get().savedOn().equals(today))source=stored.get().quoteCached();
            else{
                try{source=zen.today();cache.write(source,today);}
                catch(Exception ex){synchronized(this){retryAfter=clock.instant().plus(Duration.ofMinutes(5));}result.complete(fallback(today));return;}
            }
            if(cache.quotaExhausted(today)){result.complete(Optional.of(originalOnly(source)));return;}
            int remaining=Math.max(0,translator.dailyCharacterLimit()-cache.translationCharacters(today));
            CompletableFuture<String> translation=CompletableFuture.supplyAsync(()->{
                try{return translator.translate(source.text(),remaining,count->{try{cache.addTranslationCharacters(today,count);}catch(java.io.IOException e){throw new UncheckedIOException(e);}});}
                catch(TranslationException e){throw new java.util.concurrent.CompletionException(e);}
            },executor);
            String translated;
            try{translated=translation.get(translationTimeout.toMillis(),TimeUnit.MILLISECONDS);}catch(TimeoutException ex){translation.cancel(true);throw new TranslationException(TranslationException.Kind.TIMEOUT,"La traducción tardó demasiado.",ex);}catch(ExecutionException ex){Throwable cause=ex.getCause();if(cause instanceof java.util.concurrent.CompletionException ce&&ce.getCause()!=null)cause=ce.getCause();if(cause instanceof TranslationException te)throw te;if(cause instanceof UncheckedIOException ioe)throw new TranslationException(TranslationException.Kind.OTHER,"No se pudo guardar el consumo de traducción.",ioe);throw new TranslationException(TranslationException.Kind.OTHER,"No se pudo traducir la frase.",cause);}
            if(translated==null||translated.isBlank()||translated.trim().equalsIgnoreCase(source.text().trim()))throw new TranslationException(TranslationException.Kind.OTHER,"MyMemory no devolvió una traducción válida.");
            cache.saveTranslation(translated,today);retryAfter=null;DailyQuote done=new DailyQuote(source.text(),source.author(),source.date(),source.origin(),translated,DailyQuote.TranslationStatus.TRANSLATED);result.complete(Optional.of(done));
        }catch(TranslationException ex){
            try{if(ex.kind()==TranslationException.Kind.QUOTA)cache.markQuotaExhausted(today);cache.saveOriginalOnly(today);}catch(Exception ignored){}
            synchronized(this){retryAfter=ex.kind()==TranslationException.Kind.QUOTA?null:clock.instant().plus(Duration.ofMinutes(5));}
            result.complete(fallback(today));
        }catch(Exception ex){
            try{cache.saveOriginalOnly(today);}catch(Exception ignored){}
            synchronized(this){retryAfter=clock.instant().plus(Duration.ofMinutes(5));}
            result.complete(fallback(today));
        }finally{synchronized(this){if(inFlight==result)inFlight=null;}}
    }
    private Optional<DailyQuote> fallback(LocalDate today){return cache.read().map(q->q.savedOn().equals(today)?originalOnly(q.quoteCached()):q.asStale());}
    private static DailyQuote originalOnly(DailyQuote q){return new DailyQuote(q.text(),q.author(),q.date(),q.origin(),null,DailyQuote.TranslationStatus.ORIGINAL_ONLY);}
}
