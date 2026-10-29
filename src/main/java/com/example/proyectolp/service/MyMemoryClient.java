package com.example.proyectolp.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.function.IntConsumer;

/** Cliente MyMemory con fragmentos UTF-8 limitados a 480 bytes. */
public final class MyMemoryClient implements QuoteTranslator {
    public static final String ENDPOINT="https://api.mymemory.translated.net/get";
    private static final int MAX_BYTES=480;
    private static final ObjectMapper JSON=new ObjectMapper();
    private final HttpTransport transport;private final String email;
    public MyMemoryClient(){this(new JdkHttpTransport(),TranslationConfig.email());}
    public MyMemoryClient(HttpTransport transport){this(transport,"");}
    public MyMemoryClient(HttpTransport transport,String email){this.transport=transport;this.email=email==null?"":email.trim();}
    @Override public int dailyCharacterLimit(){return email.isBlank()?5000:50000;}
    @Override public String translate(String text)throws TranslationException{return translate(text,Integer.MAX_VALUE,ignored->{});}
    @Override public String translate(String text,int remaining,IntConsumer onCharactersSent)throws TranslationException{
        if(text==null||text.isBlank())throw new TranslationException(TranslationException.Kind.INVALID_RESPONSE,"No hay texto para traducir.");
        List<String> chunks=splitText(text,MAX_BYTES);List<String> translations=new ArrayList<>();int total=0;
        for(String chunk:chunks){int chars=chunk.length();if(chars>remaining-total)throw new TranslationException(TranslationException.Kind.QUOTA,"Se agotó la cuota diaria de traducción.");onCharactersSent.accept(chars);total+=chars;translations.add(translateChunk(chunk));}
        String result=String.join(" ",translations).trim();if(result.equalsIgnoreCase(text.trim()))throw new TranslationException(TranslationException.Kind.OTHER,"MyMemory no devolvió una traducción distinta del original.");return result;
    }
    public URI buildUri(String text){StringBuilder query=new StringBuilder("q=").append(encode(text)).append("&langpair=en%7Ces");if(!email.isBlank())query.append("&de=").append(encode(email));return URI.create(ENDPOINT+"?"+query);}
    private String translateChunk(String chunk)throws TranslationException{
        try{
            URI uri=buildUri(chunk);HttpTransport.Response response=transport.send("GET",uri,Map.of("Accept","application/json"),null);
            if(response.statusCode()==429)throw new TranslationException(TranslationException.Kind.QUOTA,"MyMemory agotó la cuota diaria de traducción.");
            if(response.statusCode()<200||response.statusCode()>=300)throw new TranslationException(TranslationException.Kind.OTHER,"MyMemory respondió con un error HTTP "+response.statusCode()+".");
            ResponseDto dto=JSON.readValue(response.body(),ResponseDto.class);String translated=dto.responseData==null?"":decodeEntities(dto.responseData.translatedText==null?"":dto.responseData.translatedText).trim();
            String warning=translated.toLowerCase(Locale.ROOT);
            int status=parseStatus(dto.responseStatus);
            if(Boolean.TRUE.equals(dto.quotaFinished)||warning.startsWith("mymemory warning")||status>0&&status!=200)throw new TranslationException(TranslationException.Kind.QUOTA,"MyMemory agotó la cuota diaria de traducción.");
            if(status!=200)throw new TranslationException(TranslationException.Kind.INVALID_RESPONSE,"MyMemory rechazó la traducción (estado "+status+").");
            if(translated.isBlank()||warning.startsWith("mymemory warning"))throw new TranslationException(TranslationException.Kind.INVALID_RESPONSE,"MyMemory no devolvió una traducción válida.");
            if(translated.equalsIgnoreCase(chunk.trim()))throw new TranslationException(TranslationException.Kind.OTHER,"MyMemory no devolvió una traducción distinta del original.");
            return translated;
        }catch(TranslationException ex){throw ex;}
        catch(java.net.http.HttpTimeoutException|SocketTimeoutException ex){throw new TranslationException(TranslationException.Kind.TIMEOUT,"Se agotó el tiempo de espera al traducir la frase.",ex);}
        catch(InterruptedException ex){Thread.currentThread().interrupt();throw new TranslationException(TranslationException.Kind.NETWORK,"Se interrumpió la traducción.",ex);}
        catch(JsonProcessingException ex){throw new TranslationException(TranslationException.Kind.INVALID_RESPONSE,"MyMemory devolvió JSON inválido.",ex);}
        catch(IOException ex){throw new TranslationException(TranslationException.Kind.NETWORK,"No hay conexión con MyMemory.",ex);}
        catch(Exception ex){throw new TranslationException(TranslationException.Kind.INVALID_RESPONSE,"No se pudo interpretar la respuesta de MyMemory.",ex);}
    }
    private static int parseStatus(JsonNode status){if(status==null)return -1;try{return status.isNumber()?status.asInt():Integer.parseInt(status.asText().trim());}catch(Exception ex){return -1;}}
    private static String encode(String value){return URLEncoder.encode(value,StandardCharsets.UTF_8).replace("+","%20");}
    public static String decodeEntities(String input){if(input==null)return "";StringBuilder out=new StringBuilder();for(int i=0;i<input.length();){if(input.charAt(i)=='&'){int end=input.indexOf(';',i+1);if(end>i&&end-i<=12){String entity=input.substring(i+1,end);String decoded=entity(entity);if(decoded!=null){out.append(decoded);i=end+1;continue;}}}out.append(input.charAt(i++));}return out.toString();}
    private static String entity(String value){return switch(value){case "amp"->"&";case "quot"->"\"";case "apos","#39"->"'";case "lt"->"<";case "gt"->">";default->{try{int cp=value.startsWith("#x")||value.startsWith("#X")?Integer.parseInt(value.substring(2),16):value.startsWith("#")?Integer.parseInt(value.substring(1)): -1;yield cp>=0&&Character.isValidCodePoint(cp)?new String(Character.toChars(cp)):null;}catch(Exception ex){yield null;}}};}
    public static List<String> splitText(String text,int maxBytes){if(maxBytes<4)throw new IllegalArgumentException("El límite debe permitir un carácter UTF-8.");String normalized=text==null?"":text.trim().replaceAll("\\s+"," ");if(normalized.isEmpty())return List.of();List<String> sentences=new ArrayList<>();for(String s:normalized.split("(?<=[.!?])\\s+"))if(!s.isBlank())sentences.add(s);List<String> result=new ArrayList<>();for(String sentence:sentences)splitWords(sentence,maxBytes,result);return List.copyOf(result);}
    private static void splitWords(String text,int maxBytes,List<String> out){StringBuilder part=new StringBuilder();for(String word:text.split(" ")){if(word.getBytes(StandardCharsets.UTF_8).length>maxBytes){if(!part.isEmpty()){out.add(part.toString());part.setLength(0);}splitLongWord(word,maxBytes,out);continue;}int needed=(part.isEmpty()?0:1)+word.getBytes(StandardCharsets.UTF_8).length;if(part.toString().getBytes(StandardCharsets.UTF_8).length+needed>maxBytes){out.add(part.toString());part.setLength(0);}if(!part.isEmpty())part.append(' ');part.append(word);}if(!part.isEmpty())out.add(part.toString());}
    private static void splitLongWord(String word,int maxBytes,List<String> out){StringBuilder part=new StringBuilder();for(int i=0;i<word.length();){int cp=word.codePointAt(i);String chars=new String(Character.toChars(cp));if(part.toString().getBytes(StandardCharsets.UTF_8).length+chars.getBytes(StandardCharsets.UTF_8).length>maxBytes){out.add(part.toString());part.setLength(0);}part.append(chars);i+=Character.charCount(cp);}if(!part.isEmpty())out.add(part.toString());}
    @JsonIgnoreProperties(ignoreUnknown=true) public static final class ResponseDto{public ResponseData responseData;public JsonNode responseStatus;public Boolean quotaFinished;}
    @JsonIgnoreProperties(ignoreUnknown=true) public static final class ResponseData{public String translatedText;}
}
