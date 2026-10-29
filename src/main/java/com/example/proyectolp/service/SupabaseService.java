package com.example.proyectolp.service;

import com.example.proyectolp.config.AppConfig;
import com.example.proyectolp.model.Session;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SupabaseService {
    public interface WriteListener { void beforeBusinessWrite(String table) throws Exception; void afterBusinessWrite(String table) throws Exception; }
    private static volatile WriteListener writeListener;
    private static final java.util.Set<String> BUSINESS_TABLES = java.util.Set.of("projects","stages","tasks","habits","habit_logs","income","expenses","financial_goals","workout_routines","exercises","workout_sessions","workout_sets","attachments");
    public static void setWriteListener(WriteListener listener) { writeListener = listener; }
    private final AppConfig config;
    private final HttpTransport transport;
    private final SessionManager sessions;
    private final AuthService auth;
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    private final long generation;
    public SupabaseService(AppConfig config) { this(config,new JdkHttpTransport(),SessionManager.getInstance()); }
    public SupabaseService(AppConfig config,HttpTransport transport,SessionManager sessions) {
        this(config,transport,sessions,new AuthService(config,transport,sessions,new SessionStorage(),java.time.Clock.systemUTC()));
    }
    public SupabaseService(AppConfig config,HttpTransport transport,SessionManager sessions,AuthService auth) {
        this.config=config;this.transport=transport;this.sessions=sessions;this.auth=auth;Long taskGeneration=SessionTaskExecutor.submittedGeneration();this.generation=taskGeneration==null?sessions.generation():taskGeneration;
    }
    public <T> T get(String path, Class<T> type) throws IOException, InterruptedException, SupabaseException { return mapper.readValue(send("GET",path,null,"").body(),type); }
    public <T> List<T> getList(String path, Class<T> type) throws IOException, InterruptedException, SupabaseException { return mapper.readValue(send("GET",path,null,"").body(),listType(type)); }
    public <T> T get(String path,TypeReference<T> type)throws IOException,InterruptedException,SupabaseException{return mapper.readValue(send("GET",path,null,"").body(),type);}
    public String post(String path,Object value,String prefer)throws IOException,InterruptedException,SupabaseException{return send("POST",path,json(value),prefer).body();}
    public <T> List<T> postList(String path,Object value,String prefer,Class<T> type)throws IOException,InterruptedException,SupabaseException{return mapper.readValue(send("POST",path,json(value),prefer).body(),listType(type));}
    public <T> List<T> insertIgnore(String path,Object value,Class<T> type)throws IOException,InterruptedException,SupabaseException {
        String body=send("POST",path,json(value),"resolution=ignore-duplicates,return=representation").body();
        return body==null||body.isBlank()?List.of():mapper.readValue(body,listType(type));
    }
    public String patch(String path,Object value,String prefer)throws IOException,InterruptedException,SupabaseException{return send("PATCH",path,json(value),prefer).body();}
    public String delete(String path)throws IOException,InterruptedException,SupabaseException{return send("DELETE",path,null,"").body();}
    public Map<String,String> headersFor(Session session,String prefer,boolean body) {
        Map<String,String> headers=new LinkedHashMap<>();headers.put("apikey",config.key());headers.put("Accept","application/json");
        if(session!=null)headers.put("Authorization","Bearer "+session.accessToken());
        if(body)headers.put("Content-Type","application/json");if(prefer!=null&&!prefer.isBlank())headers.put("Prefer",prefer);return headers;
    }
    private HttpTransport.Response send(String method,String path,String body,String prefer)throws IOException,InterruptedException,SupabaseException {
        String route=path.startsWith("/")?path.substring(1):path;
        String table=route.split("[?/ ]",2)[0];
        return execute(method,route,body,prefer,config.url()+"/rest/v1/",false,false,table,
                session->transport.send(method,URI.create(config.url()+"/rest/v1/"+route),headersFor(session,prefer,body!=null),body));
    }

    public void storageUpload(String storagePath,Path file,String contentType)throws IOException,InterruptedException,SupabaseException {
        String route="object/attachments/"+encodeStoragePath(storagePath);
        execute("POST",route,null,null,config.url()+"/storage/v1/",true,false,null,session->{
            Map<String,String> headers=headersFor(session,null,false);headers.put("Content-Type",contentType);headers.put("x-upsert","false");
            return transport.sendFile("POST",URI.create(config.url()+"/storage/v1/"+route),headers,file,Duration.ofMinutes(10));
        });
    }

    public void storageDownload(String storagePath,Path destination)throws IOException,InterruptedException,SupabaseException {
        String route="object/authenticated/attachments/"+encodeStoragePath(storagePath);
        execute("GET",route,null,null,config.url()+"/storage/v1/",true,false,null,session->transport.downloadFile("GET",URI.create(config.url()+"/storage/v1/"+route),headersFor(session,null,false),destination,Duration.ofMinutes(10)));
    }

    public void storageDelete(String storagePath)throws IOException,InterruptedException,SupabaseException {
        String route="object/attachments/"+encodeStoragePath(storagePath);
        execute("DELETE",route,null,null,config.url()+"/storage/v1/",true,true,null,session->transport.send("DELETE",URI.create(config.url()+"/storage/v1/"+route),headersFor(session,null,false),null));
    }

    public void storageDeleteMany(List<String> storagePaths)throws IOException,InterruptedException,SupabaseException {
        if(storagePaths==null||storagePaths.isEmpty())return;
        String route="object/attachments";String body=json(Map.of("prefixes",storagePaths));
        execute("DELETE",route,body,null,config.url()+"/storage/v1/",true,true,null,session->{Map<String,String> headers=headersFor(session,null,true);return transport.send("DELETE",URI.create(config.url()+"/storage/v1/"+route),headers,body);});
    }

    private HttpTransport.Response execute(String method,String route,String body,String prefer,String baseUrl,boolean storage,boolean ignoreNotFound,String actionTable,RequestCall requestCall)throws IOException,InterruptedException,SupabaseException {
        var lock=SessionManager.requestLock().readLock();lock.lock();
        try {
            boolean write=method.equals("POST")||method.equals("PATCH")||method.equals("DELETE");
            String table=actionTable==null?route.split("[?/ ]",2)[0]:actionTable;
            boolean business=write&&BUSINESS_TABLES.contains(table);
            Session initial=sessions.current();long requestGeneration=sessions.generation();
            if(requestGeneration!=generation)throw new SupabaseException(401,"La sesión cambió; vuelve a cargar la pantalla.");
            if(business&&initial!=null){WriteListener l=writeListener;if(l!=null)try{l.beforeBusinessWrite(table);}catch(Exception e){throw new IOException("No se pudo comprobar la racha antes de guardar.",e);}}
            Session current=sessions.current();
            if(current!=null&&sessions.needsRefresh(current)) current=renew(current,false);
            HttpTransport.Response response;
            try{response=requestCall.send(current);}catch(java.net.http.HttpTimeoutException e){if(storage)throw new SupabaseException(0,"", "La transferencia tardó demasiado. Inténtalo de nuevo.");throw e;}catch(java.net.ConnectException e){if(storage)throw new SupabaseException(0,"", "No hay conexión con Supabase Storage.");throw e;}
            if(response.statusCode()==401&&current!=null){
                current=renew(current,true);
                try{response=requestCall.send(current);}catch(java.net.http.HttpTimeoutException e){if(storage)throw new SupabaseException(0,"", "La transferencia tardó demasiado. Inténtalo de nuevo.");throw e;}catch(java.net.ConnectException e){if(storage)throw new SupabaseException(0,"", "No hay conexión con Supabase Storage.");throw e;}
                if(response.statusCode()==401){sessions.emitExpired("La sesión expiró. Inicia sesión de nuevo.");throw new SupabaseException(401,"La sesión expiró. Inicia sesión de nuevo.");}
            }
            if(ignoreNotFound&&response.statusCode()==404)return response;
            if(response.statusCode()<200||response.statusCode()>=300)throw storage?new SupabaseException(response.statusCode(),response.body(),storageMessage(response.statusCode())):new SupabaseException(response.statusCode(),response.body());
            if(business&&current!=null){WriteListener l=writeListener;if(l!=null)try{l.afterBusinessWrite(table);}catch(Exception e){
                // La escritura principal ya quedó confirmada por Supabase; un fallo auxiliar no debe cortar operaciones compuestas.
                NotificationService.getInstance().show("Se guardó el cambio, pero no se pudo actualizar la racha. Vuelve a intentarlo o revisa user_stats y sus políticas RLS.");
            }}
            return response;
        } finally {lock.unlock();}
    }

    private static String storageMessage(int status){return status==413?"El archivo supera el tamaño permitido.":status==401||status==403?"Supabase Storage rechazó el acceso. Revisa la sesión y las políticas.":status==400?"Storage rechazó el archivo o la ruta. Revisa el bucket y las políticas.":status==404?"No se encontró el bucket u objeto de Storage.":status>=500?"Supabase Storage no está disponible en este momento.":"Supabase Storage rechazó la operación (HTTP "+status+").";}
    private static String encodeStoragePath(String path){return java.util.Arrays.stream(path.split("/",-1)).map(s->java.net.URLEncoder.encode(s,java.nio.charset.StandardCharsets.UTF_8).replace("+","%20")).collect(java.util.stream.Collectors.joining("/"));}
    @FunctionalInterface private interface RequestCall{HttpTransport.Response send(Session session)throws IOException,InterruptedException;}
    private Session renew(Session old,boolean forced)throws SupabaseException {
        if(auth==null){sessions.emitExpired("La sesión expiró. Inicia sesión de nuevo.");throw new SupabaseException(401,"No se pudo renovar la sesión.");}
        synchronized(sessions){
            Session now=sessions.current();if(now==null)throw new SupabaseException(401,"La sesión expiró. Inicia sesión de nuevo.");
            if(!now.refreshToken().equals(old.refreshToken()))return now;
            if(!forced&&!sessions.needsRefresh(now))return now;
            try{return auth.refresh(now.refreshToken(),sessions.rememberSession());}catch(AuthException e){auth.clearStoredRefresh();sessions.emitExpired("La sesión expiró. Inicia sesión de nuevo.");throw new SupabaseException(401,"La sesión expiró. Inicia sesión de nuevo.");}
        }
    }
    private JavaType listType(Class<?> type){return mapper.getTypeFactory().constructCollectionType(List.class,type);}
    private String json(Object value)throws IOException{return mapper.writeValueAsString(value);}
}
