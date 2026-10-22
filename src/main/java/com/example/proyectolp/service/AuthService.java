package com.example.proyectolp.service;

import com.example.proyectolp.config.AppConfig;
import com.example.proyectolp.model.Session;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;

/** Cliente REST de Supabase Auth, separado de PostgREST. */
public final class AuthService {
    private final AppConfig config;
    private final HttpTransport transport;
    private final SessionManager sessions;
    private final SessionStorage storage;
    private final Clock clock;
    private final ObjectMapper mapper = new ObjectMapper();
    public AuthService(AppConfig config, SessionManager sessions, SessionStorage storage) { this(config, new JdkHttpTransport(), sessions, storage, Clock.systemUTC()); }
    public AuthService(AppConfig config, HttpTransport transport, SessionManager sessions, SessionStorage storage, Clock clock) {
        this.config=config; this.transport=transport; this.sessions=sessions; this.storage=storage; this.clock=clock;
    }
    public Session login(String email, String password, boolean remember) throws AuthException {
        JsonNode result = request("POST", "/auth/v1/token?grant_type=password", Map.of("email",email,"password",password), null);
        Session session = Session.fromAuthResponse(result, clock.instant());
        if (session == null) throw new AuthException("Supabase no devolvió una sesión válida.");
        accept(session, remember); return session;
    }
    public Session register(String email, String password, String displayName, boolean remember) throws AuthException {
        Map<String,Object> body = new LinkedHashMap<>(); body.put("email",email); body.put("password",password); body.put("data",Map.of("display_name",displayName));
        JsonNode result = request("POST", "/auth/v1/signup", body, null);
        Session session = Session.fromAuthResponse(result, clock.instant());
        if (session != null) accept(session, remember);
        return session;
    }
    public Session refresh(String refreshToken, boolean remember) throws AuthException {
        long sessionGeneration=sessions.generation();
        JsonNode result = request("POST", "/auth/v1/token?grant_type=refresh_token", Map.of("refresh_token",refreshToken), null);
        Session renewed=Session.fromAuthResponse(result,clock.instant());
        if(renewed==null)throw new AuthException("La sesión expiró. Inicia sesión de nuevo.");
        if(sessionGeneration!=sessions.generation())throw new AuthException("La sesión cambió mientras se renovaba. Inicia sesión de nuevo.");
        Session current=sessions.current();
        if(current!=null&&!current.refreshToken().equals(refreshToken))throw new AuthException("La sesión ya fue renovada en otra solicitud.");
        accept(renewed,remember); return renewed;
    }
    public Session restore() throws AuthException {
        try { String token=storage.load(); return token==null||token.isBlank()?null:refresh(token,true); }
        catch(IOException e){ return null; }
    }
    public void clearStoredRefresh(){try{storage.clear();}catch(IOException ignored){}}
    public void logout() throws AuthException {
        var lock=SessionManager.requestLock().writeLock(); lock.lock();
        try {
            Session current=sessions.current();
            try { if(current!=null) request("POST","/auth/v1/logout?scope=local",Map.of(),current.accessToken()); }
            finally { sessions.clear(); try { storage.clear(); } catch(IOException ignored) {} }
        } finally { lock.unlock(); }
    }
    public void revokeAndClear(Session captured) {
        var lock=SessionManager.requestLock().writeLock();lock.lock();
        try { try { if(captured!=null)request("POST","/auth/v1/logout?scope=local",Map.of(),captured.accessToken()); } catch(AuthException ignored) {}
            sessions.clear();try{storage.clear();}catch(IOException ignored){}
        } finally { lock.unlock(); }
    }
    public void revoke(Session captured) {
        if(captured==null)return;
        try{request("POST","/auth/v1/logout?scope=local",Map.of(),captured.accessToken());}catch(AuthException ignored){}
    }
    private void accept(Session session, boolean remember) throws AuthException {
        sessions.set(session);sessions.setRemembered(remember);
        sessions.scheduleRefresh(this,session);
        try { if(remember) storage.save(session.refreshToken()); else storage.clear(); }
        catch(IOException e){ sessions.clear(); throw new AuthException("No se pudo guardar la preferencia Recordarme en este equipo."); }
    }
    private JsonNode request(String method,String path,Object payload,String bearer) throws AuthException {
        try {
            Map<String,String> headers=new LinkedHashMap<>(); headers.put("apikey",config.key()); headers.put("Accept","application/json"); headers.put("Content-Type","application/json");
            if(bearer!=null)headers.put("Authorization","Bearer "+bearer);
            String body=payload==null?"":mapper.writeValueAsString(payload);
            HttpTransport.Response response=transport.send(method,URI.create(config.url()+path),headers,body);
            if(response.statusCode()<200||response.statusCode()>=300)throw AuthException.from(response.statusCode(),response.body());
            return response.body()==null||response.body().isBlank()?mapper.createObjectNode():mapper.readTree(response.body());
        } catch(AuthException e){throw e;} catch(InterruptedException e){Thread.currentThread().interrupt();throw AuthException.offline();} catch(IOException|RuntimeException e){throw AuthException.offline();}
    }
    public static boolean validEmail(String email){return email!=null&&email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");}
    public static boolean validPassword(String password){return password!=null&&password.length()>=8;}
}
