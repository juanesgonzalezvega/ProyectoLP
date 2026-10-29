package com.example.proyectolp.service;

import com.example.proyectolp.model.DailyQuote;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Locale;

public final class ZenQuotesClient {
    public static final URI TODAY_URI=URI.create("https://zenquotes.io/api/today");
    private static final ObjectMapper JSON=new ObjectMapper();
    private final HttpTransport transport;private final Clock clock;
    public ZenQuotesClient(){this(new JdkHttpTransport(),Clock.system(java.time.ZoneId.of("America/Bogota")));}
    public ZenQuotesClient(HttpTransport transport){this(transport,Clock.system(java.time.ZoneId.of("America/Bogota")));}
    public ZenQuotesClient(HttpTransport transport,Clock clock){this.transport=transport;this.clock=clock.withZone(java.time.ZoneId.of("America/Bogota"));}

    public DailyQuote today() throws ZenQuotesException {
        try{
            HttpTransport.Response response=transport.send("GET",TODAY_URI,Map.of("Accept","application/json"),null);
            if(response.statusCode()==429)throw new ZenQuotesException(ZenQuotesException.Kind.RATE_LIMIT,"ZenQuotes alcanzó el límite de consultas. Inténtalo más tarde.");
            if(response.statusCode()<200||response.statusCode()>=300)throw new ZenQuotesException(ZenQuotesException.Kind.HTTP,"ZenQuotes respondió con un error HTTP "+response.statusCode()+".");
            QuoteDto[] values=JSON.readValue(response.body(),QuoteDto[].class);
            if(values.length==0)throw new ZenQuotesException(ZenQuotesException.Kind.NO_DATA,"ZenQuotes no devolvió una frase.");
            QuoteDto dto=values[0];String text=sanitize(dto.q);String author=dto.a==null?"":dto.a.trim();
            if(isLimitResponse(text,author))throw new ZenQuotesException(ZenQuotesException.Kind.RATE_LIMIT,"ZenQuotes alcanzó el límite de consultas. Inténtalo más tarde.");
            if(text.isBlank())throw new ZenQuotesException(ZenQuotesException.Kind.NO_DATA,"ZenQuotes no devolvió una frase válida.");
            LocalDate date;try{date=dto.date==null?LocalDate.now(clock):LocalDate.parse(dto.date);}catch(Exception ex){date=LocalDate.now(clock);}
            return new DailyQuote(text,author.isBlank()?"Unknown":author,date,DailyQuote.Origin.FRESH);
        }catch(ZenQuotesException ex){throw ex;}
        catch(java.net.http.HttpTimeoutException|SocketTimeoutException ex){throw new ZenQuotesException(ZenQuotesException.Kind.TIMEOUT,"Se agotó el tiempo de espera al consultar la frase.",ex);}
        catch(InterruptedException ex){Thread.currentThread().interrupt();throw new ZenQuotesException(ZenQuotesException.Kind.NETWORK,"Se interrumpió la consulta de la frase.",ex);}
        catch(JsonProcessingException ex){throw new ZenQuotesException(ZenQuotesException.Kind.INVALID_JSON,"ZenQuotes devolvió JSON inválido.",ex);}
        catch(IOException ex){throw new ZenQuotesException(ZenQuotesException.Kind.NETWORK,"No hay conexión con ZenQuotes.",ex);}
        catch(Exception ex){throw new ZenQuotesException(ZenQuotesException.Kind.INVALID_JSON,"No se pudo interpretar la respuesta de ZenQuotes.",ex);}
    }
    public static String sanitize(String value){if(value==null)return "";String text=value.trim().replaceAll("\\s+"," ");if(text.length()>400)text=text.substring(0,399)+"…";return text;}
    private static boolean isLimitResponse(String text,String author){String lower=(text+" "+author).toLowerCase(Locale.ROOT);return (author.toLowerCase(Locale.ROOT).contains("zenquotes")&&(lower.contains("too many")||lower.contains("limit")||lower.contains("request")))||lower.contains("too many requests")||lower.contains("rate limit");}
    @JsonIgnoreProperties(ignoreUnknown=true) public static final class QuoteDto{public String q,a,h,date;}
}
