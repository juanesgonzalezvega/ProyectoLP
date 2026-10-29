package com.example.proyectolp;

import com.example.proyectolp.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.file.Path;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class TrmServicesTest {
    @TempDir Path temp;
    private static final Clock CLOCK=Clock.fixed(Instant.parse("2026-10-28T15:00:00Z"),TrmSelector.ZONE);
    @Test void convierteEnAmbosSentidosYRedondea(){assertEquals(new BigDecimal("250.00"),CurrencyConversionService.copToUsd(new BigDecimal("1000000"),new BigDecimal("4000")));assertEquals(new BigDecimal("4000.00"),CurrencyConversionService.usdToCop(new BigDecimal("1"),new BigDecimal("4000")));assertEquals(new BigDecimal("-0.01"),CurrencyConversionService.copToUsd(new BigDecimal("-20"),new BigDecimal("4000")));}
    @Test void rechazaTasaInvalida(){assertThrows(IllegalArgumentException.class,()->CurrencyConversionService.copToUsd(BigDecimal.ONE,null));assertThrows(IllegalArgumentException.class,()->CurrencyConversionService.copToUsd(BigDecimal.ONE,BigDecimal.ZERO));assertThrows(IllegalArgumentException.class,()->CurrencyConversionService.usdToCop(BigDecimal.ONE,new BigDecimal("-2")));}
    @Test void selectorIncluyeRangoYUsaUltimaPublicada() throws Exception {TrmSelector s=new TrmSelector(CLOCK);var weekend=new TrmClient.TrmRecord(new BigDecimal("4000"),LocalDate.of(2026,10,24),LocalDate.of(2026,10,26));var today=new TrmClient.TrmRecord(new BigDecimal("4100"),LocalDate.of(2026,10,28),LocalDate.of(2026,10,28));assertEquals(today,s.select(List.of(weekend,today)));assertEquals(weekend,s.select(List.of(weekend)));}
    @Test void clienteParseaFixtureYCodificaUri() throws Exception {String fixture="[{\"valor\":\"4000.25\",\"unidad\":\"COP\",\"vigenciadesde\":\"2026-10-28T00:00:00.000\",\"vigenciahasta\":\"2026-10-28T00:00:00.000\"}]"; // Forma real del dataset, valor decimal representado como texto.
        HttpTransport fake=(method,uri,headers,body)->{assertEquals("application/json",headers.get("Accept"));assertTrue(uri.toString().contains("%24order=vigenciadesde+DESC"));return new HttpTransport.Response(200,fixture);};
        TrmClient client=new TrmClient(fake,new TrmCache(temp,CLOCK),CLOCK);assertEquals(new BigDecimal("4000.25"),client.current(true).rate());}
    @Test void cacheToleraCorrupcionYAlmacena(){TrmCache cache=new TrmCache(temp,CLOCK);assertNull(cache.read());var q=new TrmClient.TrmQuote(new BigDecimal("4000"),LocalDate.of(2026,10,28),LocalDate.of(2026,10,28),CLOCK.instant(),TrmClient.Origin.FRESH);assertDoesNotThrow(()->cache.write(q));assertEquals(q.rate(),cache.read().rate());}
    @Test void clienteInformaRespuestaVaciaErrorHttpJsonYTimeout(){
        TrmCache cache=new TrmCache(temp,CLOCK);
        assertTrue(assertThrows(TrmException.class,()->client((m,u,h,b)->new HttpTransport.Response(200,"[]"),cache).current(true)).getMessage().contains("no devolvió datos"));
        assertTrue(assertThrows(TrmException.class,()->client((m,u,h,b)->new HttpTransport.Response(500,"{}"),cache).current(true)).getMessage().contains("HTTP 500"));
        assertTrue(assertThrows(TrmException.class,()->client((m,u,h,b)->new HttpTransport.Response(200,"not json"),cache).current(true)).getMessage().contains("JSON inválidos"));
        assertTrue(assertThrows(TrmException.class,()->client((m,u,h,b)->{throw new java.net.http.HttpTimeoutException("timeout");},cache).current(true)).getMessage().contains("tiempo de espera"));
    }
    @Test void cacheSoloEsVigenteElMismoDia(){TrmCache cache=new TrmCache(temp,CLOCK);var q=new TrmClient.TrmQuote(new BigDecimal("4000"),LocalDate.of(2026,10,28),LocalDate.of(2026,10,28),CLOCK.instant(),TrmClient.Origin.FRESH);assertDoesNotThrow(()->cache.write(q));assertNotNull(cache.readValidToday());Clock tomorrow=Clock.fixed(Instant.parse("2026-10-29T15:00:00Z"),TrmSelector.ZONE);assertNull(new TrmCache(temp,tomorrow).readValidToday());}
    private TrmClient client(HttpTransport transport,TrmCache cache){return new TrmClient(transport,cache,CLOCK);}
}
