package com.example.proyectolp;

import com.example.proyectolp.model.DailyQuote;
import com.example.proyectolp.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.net.http.HttpTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.*;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class ZenQuotesTest {
    @TempDir Path temp;
    private static final ZoneId BOGOTA=ZoneId.of("America/Bogota");
    private static final String BODY="[{\"q\":\"  Keep\\n moving forward.  \",\"a\":\"Ada Lovelace\",\"h\":\"<b>ignored</b>\",\"date\":\"2026-10-29\",\"unknown\":1}]";

    @Test void parsesOnlyRawQuoteAndSanitizes(){ZenQuotesClient client=new ZenQuotesClient((m,u,h,b)->{assertEquals("application/json",h.get("Accept"));return new HttpTransport.Response(200,BODY);},fixed("2026-10-29T12:00:00Z"));DailyQuote q=assertDoesNotThrow(client::today);assertEquals("Keep moving forward.",q.text());assertEquals("Ada Lovelace",q.author());assertEquals(LocalDate.of(2026,10,29),q.date());}
    @Test void clientReportsEmptyMalformedStatusesTimeoutAndLimit(){
        assertKind(200,"[]",null,ZenQuotesException.Kind.NO_DATA);
        assertKind(200,"not json",null,ZenQuotesException.Kind.INVALID_JSON);
        assertKind(429,"",null,ZenQuotesException.Kind.RATE_LIMIT);
        assertKind(500,"",null,ZenQuotesException.Kind.HTTP);
        ZenQuotesClient timeout=new ZenQuotesClient((m,u,h,b)->{throw new HttpTimeoutException("timeout");},fixed("2026-10-29T12:00:00Z"));assertEquals(ZenQuotesException.Kind.TIMEOUT,assertThrows(ZenQuotesException.class,timeout::today).kind());
        ZenQuotesClient limit=new ZenQuotesClient((m,u,h,b)->new HttpTransport.Response(200,"[{\"q\":\"Too many requests. Please wait\",\"a\":\"zenquotes.io\"}]"),fixed("2026-10-29T12:00:00Z"));assertEquals(ZenQuotesException.Kind.RATE_LIMIT,assertThrows(ZenQuotesException.class,limit::today).kind());
    }
    @Test void sanitizesLengthWhitespaceAndEmpty(){assertEquals("line one line two",ZenQuotesClient.sanitize("  line one\n\tline two  "));String longText="x".repeat(450);String shortened=ZenQuotesClient.sanitize(longText);assertEquals(400,shortened.length());assertTrue(shortened.endsWith("…"));assertEquals("",ZenQuotesClient.sanitize(" \n "));}
    @Test void cacheReadsWritesAndIgnoresCorruptFile()throws Exception{ZenQuotesCache cache=new ZenQuotesCache(temp);assertTrue(cache.read().isEmpty());DailyQuote quote=new DailyQuote("A quote","Author",LocalDate.of(2026,10,29),DailyQuote.Origin.FRESH);cache.write(quote,quote.date());assertEquals("A quote",cache.read().orElseThrow().text());Path file=temp.resolve("zenquotes-cache.json");Files.writeString(file,"{bad json");assertTrue(cache.read().isEmpty());cache.write(quote,quote.date());assertEquals("Author",cache.read().orElseThrow().author());}
    @Test void quoteServiceUsesSameDayCacheAndRefetchesNextDay()throws Exception{ZenQuotesCache cache=new ZenQuotesCache(temp);DailyQuote saved=new DailyQuote("Cached","Author",LocalDate.of(2026,10,29),DailyQuote.Origin.FRESH);cache.write(saved,LocalDate.of(2026,10,29));AtomicInteger calls=new AtomicInteger();ZenQuotesClient client=new ZenQuotesClient((m,u,h,b)->{calls.incrementAndGet();return new HttpTransport.Response(200,BODY);},fixed("2026-10-29T12:00:00Z"));QuoteService same=new QuoteService(client,cache,fixed("2026-10-29T12:00:00Z"));assertEquals(DailyQuote.Origin.CACHED,same.todayAsync(Runnable::run).join().orElseThrow().origin());assertEquals(0,calls.get());QuoteService next=new QuoteService(client,cache,fixed("2026-10-30T12:00:00Z"));assertTrue(next.todayAsync(Runnable::run).join().isPresent());assertEquals(1,calls.get());}
    @Test void quoteServiceFallsBackAndWaitsFiveMinutesAfterFailure()throws Exception{ZenQuotesCache cache=new ZenQuotesCache(temp);DailyQuote saved=new DailyQuote("Saved quote","Author",LocalDate.of(2026,10,28),DailyQuote.Origin.FRESH);cache.write(saved,LocalDate.of(2026,10,28));MutableClock clock=new MutableClock(Instant.parse("2026-10-29T12:00:00Z"),BOGOTA);AtomicInteger calls=new AtomicInteger();ZenQuotesClient client=new ZenQuotesClient((m,u,h,b)->{calls.incrementAndGet();throw new IOException("offline");},clock);QuoteService service=new QuoteService(client,cache,clock);assertEquals(DailyQuote.Origin.STALE,service.todayAsync(Runnable::run).join().orElseThrow().origin());service.todayAsync(Runnable::run).join();assertEquals(1,calls.get());clock.advance(Duration.ofMinutes(5));service.todayAsync(Runnable::run).join();assertEquals(2,calls.get());}
    @Test void quoteServiceCoalescesConcurrentRequests()throws Exception{CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);AtomicInteger calls=new AtomicInteger();ZenQuotesCache cache=new ZenQuotesCache(temp);ZenQuotesClient client=new ZenQuotesClient((m,u,h,b)->{calls.incrementAndGet();entered.countDown();try{release.await();}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IOException(e);}return new HttpTransport.Response(200,BODY);},fixed("2026-10-29T12:00:00Z"));QuoteService service=new QuoteService(client,cache,fixed("2026-10-29T12:00:00Z"));ExecutorService pool=Executors.newFixedThreadPool(2);try{CompletableFuture<Optional<DailyQuote>> one=service.todayAsync(pool);assertTrue(entered.await(2,TimeUnit.SECONDS));CompletableFuture<Optional<DailyQuote>> two=service.todayAsync(pool);assertSame(one,two);release.countDown();assertTrue(one.get(2,TimeUnit.SECONDS).isPresent());assertEquals(1,calls.get());}finally{release.countDown();pool.shutdownNow();}}

    private void assertKind(int status,String body,Exception ignored,ZenQuotesException.Kind kind){ZenQuotesClient client=new ZenQuotesClient((m,u,h,b)->new HttpTransport.Response(status,body),fixed("2026-10-29T12:00:00Z"));assertEquals(kind,assertThrows(ZenQuotesException.class,client::today).kind());}
    private static Clock fixed(String time){return Clock.fixed(Instant.parse(time),BOGOTA);}
    private static final class MutableClock extends Clock{private Instant instant;private final ZoneId zone;MutableClock(Instant instant,ZoneId zone){this.instant=instant;this.zone=zone;}void advance(Duration d){instant=instant.plus(d);}@Override public ZoneId getZone(){return zone;}@Override public Clock withZone(ZoneId z){return z.equals(zone)?this:new MutableClock(instant,z);}@Override public Instant instant(){return instant;}}
}
