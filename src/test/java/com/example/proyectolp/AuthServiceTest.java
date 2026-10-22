package com.example.proyectolp;

import com.example.proyectolp.config.AppConfig;
import com.example.proyectolp.model.Session;
import com.example.proyectolp.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.net.URI;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class AuthServiceTest {
    @TempDir Path temp;
    private static final Clock CLOCK=Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"),ZoneOffset.UTC);
    @Test void parseaSesionYCalculaExpiry(){var n=new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode();n.put("access_token","a");n.put("refresh_token","r");n.put("expires_in",300);n.putObject("user").put("id","u").put("email","x@y.co").putObject("user_metadata").put("display_name","Ana");Session s=Session.fromAuthResponse(n,CLOCK.instant());assertEquals("u",s.userId());assertEquals("Ana",s.displayName());assertEquals(CLOCK.instant().plusSeconds(300),s.expiresAt());}
    @Test void margenDeRenovacionEsSesentaSegundos(){SessionManager manager=new SessionManager(CLOCK);Session s=new Session("a","r",CLOCK.instant().plusSeconds(60),"u","e","");assertTrue(manager.needsRefresh(s));assertFalse(manager.needsRefresh(new Session("a","r",CLOCK.instant().plusSeconds(61),"u","e","")));}
    @Test void erroresAuthSeTraducenYValidadoresFuncionan(){assertEquals("Correo o contraseña incorrectos.",AuthException.from(400,"{\"code\":\"invalid_credentials\"}").getMessage());assertTrue(AuthException.from(400,"email_not_confirmed").getMessage().contains("confirmado"));assertTrue(AuthException.from(429,"{}").getMessage().contains("Demasiados"));assertTrue(AuthService.validEmail("ana@example.com"));assertFalse(AuthService.validEmail("no-correo"));assertTrue(AuthService.validPassword("12345678"));assertFalse(AuthService.validPassword("123"));}
    @Test void usaHeadersSegunSesion(){SessionManager manager=new SessionManager(CLOCK);AppConfig cfg=new AppConfig("https://example.supabase.co","anon");SupabaseService db=new SupabaseService(cfg,(m,u,h,b)->new HttpTransport.Response(200,"[]"),manager,null);assertFalse(db.headersFor(null,"",false).containsKey("Authorization"));Session s=new Session("token","refresh",CLOCK.instant().plusSeconds(900),"uid","e","");assertEquals("Bearer token",db.headersFor(s,"",false).get("Authorization"));assertEquals("anon",db.headersFor(s,"",false).get("apikey"));}
    @Test void renuevaYReintentaSoloUnaVezYLuegoExpira(){
        SessionManager manager=new SessionManager(CLOCK);Session old=new Session("old","refresh-old",CLOCK.instant().plusSeconds(900),"uid","e","");manager.set(old);
        Queue<HttpTransport.Response> responses=new ArrayDeque<>(List.of(new HttpTransport.Response(401,"{}"),new HttpTransport.Response(200,"{\"access_token\":\"new\",\"refresh_token\":\"refresh-new\",\"expires_in\":3600,\"user\":{\"id\":\"uid\",\"email\":\"e\"}}"),new HttpTransport.Response(200,"[]")));
        List<Map<String,String>> sent=new ArrayList<>();HttpTransport transport=(m,u,h,b)->{sent.add(h);return responses.remove();};
        AuthService auth=new AuthService(new AppConfig("https://example.supabase.co","anon"),transport,manager,new SessionStorage(temp.resolve("refresh")),CLOCK);
        SupabaseService db=new SupabaseService(new AppConfig("https://example.supabase.co","anon"),transport,manager,auth);
        assertDoesNotThrow(()->db.getList("tasks?select=*",Object.class));assertEquals("Bearer new",sent.get(2).get("Authorization"));
        Queue<HttpTransport.Response> second=new ArrayDeque<>(List.of(new HttpTransport.Response(401,"{}"),new HttpTransport.Response(200,"{\"access_token\":\"new2\",\"refresh_token\":\"refresh2\",\"expires_in\":3600,\"user\":{\"id\":\"uid\",\"email\":\"e\"}}"),new HttpTransport.Response(401,"{}")));
        HttpTransport transport2=(m,u,h,b)->second.remove();AuthService auth2=new AuthService(new AppConfig("https://example.supabase.co","anon"),transport2,manager,new SessionStorage(temp.resolve("refresh2")),CLOCK);SupabaseService db2=new SupabaseService(new AppConfig("https://example.supabase.co","anon"),transport2,manager,auth2);List<String> expired=new ArrayList<>();manager.onExpired(expired::add);
        assertThrows(SupabaseException.class,()->db2.getList("tasks?select=*",Object.class));assertEquals(1,expired.size());assertNull(manager.current());
    }
}
