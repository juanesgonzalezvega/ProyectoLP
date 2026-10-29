package com.example.proyectolp.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public final class TrmClient {
    public enum Origin { FRESH, CACHED }
    public record TrmQuote(BigDecimal rate, LocalDate validFrom, LocalDate validTo, Instant fetchedAt, Origin origen) { }
    public record TrmRecord(BigDecimal rate, LocalDate validFrom, LocalDate validTo) { }
    private static final ObjectMapper JSON=new ObjectMapper().registerModule(new JavaTimeModule());
    private final HttpTransport transport; private final TrmCache cache; private final Clock clock; private final String endpoint; private final String token;
    public TrmClient(){this(new JdkHttpTransport(),new TrmCache(),Clock.system(TrmSelector.ZONE),TrmConfig.URL,TrmConfig.appToken());}
    public TrmClient(HttpTransport transport,TrmCache cache,Clock clock){this(transport,cache,clock,TrmConfig.URL,"");}
    public TrmClient(HttpTransport transport,TrmCache cache,Clock clock,String endpoint,String token){this.transport=transport;this.cache=cache;this.clock=clock.withZone(TrmSelector.ZONE);this.endpoint=endpoint;this.token=token==null?"":token;}
    public TrmQuote current() throws TrmException { return current(false); }
    public TrmQuote current(boolean force) throws TrmException {
        if(!force){TrmQuote cached=cache.readValidToday();if(cached!=null)return cached;}
        TrmQuote previous=cache.read();
        String query="%24limit=20&%24order="+URLEncoder.encode("vigenciadesde DESC",StandardCharsets.UTF_8);
        URI uri=URI.create(endpoint+(endpoint.contains("?")?"&":"?")+query);
        try{
            Map<String,String> headers=token.isBlank()?Map.of("Accept","application/json"):Map.of("Accept","application/json","X-App-Token",token);
            HttpTransport.Response response=transport.send("GET",uri,headers,null);
            if(response.statusCode()<200||response.statusCode()>=300)throw new TrmException("La fuente de TRM respondió con un error (HTTP "+response.statusCode()+").");
            TrmRecordDto[] dtos=JSON.readValue(response.body(),TrmRecordDto[].class);
            if(dtos.length==0)throw new TrmException("La fuente de TRM no devolvió datos.");
            List<TrmRecord> records=Arrays.stream(dtos).map(TrmClient::convert).filter(r->r!=null).toList();
            TrmRecord selected=new TrmSelector(clock).select(records);
            TrmQuote quote=new TrmQuote(selected.rate(),selected.validFrom(),selected.validTo(),clock.instant(),Origin.FRESH);cache.write(quote);return quote;
        }catch(TrmException e){if(previous!=null)return previous;throw e;}
        catch(java.net.http.HttpTimeoutException|SocketTimeoutException e){if(previous!=null)return previous;throw new TrmException("Se agotó el tiempo de espera al consultar la TRM.",e);}
        catch(InterruptedException e){Thread.currentThread().interrupt();if(previous!=null)return previous;throw new TrmException("Se interrumpió la consulta de la TRM.",e);}
        catch(JsonProcessingException e){if(previous!=null)return previous;throw new TrmException("La fuente de TRM devolvió datos JSON inválidos.",e);}
        catch(IOException e){if(previous!=null)return previous;throw new TrmException("No hay conexión con la fuente de TRM.",e);}
        catch(Exception e){if(previous!=null&&e instanceof java.net.ConnectException)return previous;throw new TrmException("La fuente de TRM devolvió datos JSON inválidos.",e);}
    }
    public URI requestUri(){String query="%24limit=20&%24order="+URLEncoder.encode("vigenciadesde DESC",StandardCharsets.UTF_8);return URI.create(endpoint+(endpoint.contains("?")?"&":"?")+query);}
    private static TrmRecord convert(TrmRecordDto dto){try{BigDecimal rate=new BigDecimal(dto.valor.replace("$","").replace(",",".").trim());return new TrmRecord(rate,date(dto.vigenciadesde),date(dto.vigenciahasta));}catch(Exception ex){return null;}}
    private static LocalDate date(String value){if(value==null)return null;try{return LocalDate.parse(value.substring(0,10));}catch(Exception ex){return LocalDateTime.parse(value).toLocalDate();}}
    @JsonIgnoreProperties(ignoreUnknown=true) public static final class TrmRecordDto { public String valor,unidad,vigenciadesde,vigenciahasta; }
}
