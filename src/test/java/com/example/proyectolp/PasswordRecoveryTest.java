package com.example.proyectolp;

import com.example.proyectolp.config.AppConfig;
import com.example.proyectolp.model.Session;
import com.example.proyectolp.model.enums.AccountStatus;
import com.example.proyectolp.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Files;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PasswordRecoveryTest {
    @TempDir Path temp;
    private final Instant initial = Instant.parse("2026-02-01T12:00:00Z");
    private final MutableClock clock = new MutableClock(initial);
    private SessionManager manager() { return new SessionManager(clock); }
    private AuthService auth(HttpTransport transport, SessionManager manager) {
        return new AuthService(new AppConfig("https://unit.test", "anon-key"), transport, manager,
                new SessionStorage(temp.resolve(UUID.randomUUID().toString())), clock);
    }
    private static String sessionJson() {
        return "{\"access_token\":\"temporary-access\",\"refresh_token\":\"temporary-refresh\",\"expires_in\":3600,\"user\":{\"id\":\"user-1\",\"email\":\"user@example.com\"}}";
    }

    @Test void accountStatusNormalizaCorreoYNoEnviaAuthorization() throws Exception {
        for (var pair : List.of(Map.entry("\"verified\"", AccountStatus.VERIFIED), Map.entry("\"unverified\"", AccountStatus.UNVERIFIED), Map.entry("\"not_found\"", AccountStatus.NOT_FOUND))) {
            List<HttpTransport.Response> responses = new ArrayList<>(List.of(new HttpTransport.Response(200, pair.getKey())));
            List<Map<String,String>> headers = new ArrayList<>(); List<String> bodies = new ArrayList<>(); List<URI> uris = new ArrayList<>();
            HttpTransport fake = (method, uri, h, body) -> { headers.add(h); bodies.add(body); uris.add(uri); return responses.remove(0); };
            assertEquals(pair.getValue(), auth(fake, manager()).accountStatus("  User@Example.COM  "));
            assertTrue(uris.get(0).toString().endsWith("/rest/v1/rpc/get_account_status"));
            assertTrue(headers.get(0).containsKey("apikey")); assertFalse(headers.get(0).containsKey("Authorization"));
            assertEquals("{\"p_email\":\"user@example.com\"}", bodies.get(0));
        }
    }

    @Test void accountStatusDesconocidoYErrorTienenMensajeClaro() {
        AuthService unknown = auth((m,u,h,b)->new HttpTransport.Response(200,"\"unexpected\""), manager());
        assertEquals("No se pudo interpretar el estado de la cuenta.", assertThrows(AuthException.class,()->unknown.accountStatus("a@b.co")).getMessage());
        AuthService failure = auth((m,u,h,b)->new HttpTransport.Response(500,"{}"), manager());
        assertTrue(assertThrows(AuthException.class,()->failure.accountStatus("a@b.co")).getMessage().contains("estado de la cuenta"));
        AuthService missingRpc = auth((m,u,h,b)->new HttpTransport.Response(404,"{\"code\":\"PGRST202\"}"), manager());
        assertTrue(assertThrows(AuthException.class,()->missingRpc.accountStatus("a@b.co")).getMessage().contains("11_recuperacion.sql"));
        AuthService denied = auth((m,u,h,b)->new HttpTransport.Response(403,"{\"code\":\"42501\"}"), manager());
        assertTrue(assertThrows(AuthException.class,()->denied.accountStatus("a@b.co")).getMessage().contains("GRANT EXECUTE"));
    }

    @Test void recoveryRequestsUsanCuerposCorrectosYSesionTemporalNoSeGuarda() throws Exception {
        SessionManager manager = manager(); Session previous = new Session("existing","refresh",initial.plusSeconds(5000),"other","other@example.com",""); manager.set(previous);
        List<String> methods=new ArrayList<>(), paths=new ArrayList<>(), bodies=new ArrayList<>(); List<Map<String,String>> headers=new ArrayList<>();
        Queue<HttpTransport.Response> responses = new ArrayDeque<>(List.of(
                new HttpTransport.Response(200,"{}"), new HttpTransport.Response(200,sessionJson()),
                new HttpTransport.Response(200,"{}"), new HttpTransport.Response(204,"")));
        HttpTransport fake=(method,uri,h,body)->{methods.add(method);paths.add(uri.getPath());bodies.add(body);headers.add(h);return responses.remove();};
        Path stored=temp.resolve("must-not-exist"); AuthService auth=new AuthService(new AppConfig("https://unit.test","anon-key"),fake,manager,new SessionStorage(stored),clock);
        auth.sendRecoveryCode(" User@Example.COM "); Session recovery=auth.verifyRecoveryCode(" User@Example.COM ","123456"); auth.updatePassword(recovery,"new-secret"); auth.revoke(recovery);
        assertEquals(List.of("POST","POST","PUT","POST"),methods);
        assertTrue(paths.get(0).endsWith("/auth/v1/recover")); assertEquals("{\"email\":\"user@example.com\"}",bodies.get(0));
        assertTrue(paths.get(1).endsWith("/auth/v1/verify"));
        assertFalse(headers.get(0).containsKey("Authorization")); assertFalse(headers.get(1).containsKey("Authorization"));
        var verify=new ObjectMapper().readTree(bodies.get(1)); assertEquals("recovery",verify.path("type").asText()); assertEquals("user@example.com",verify.path("email").asText()); assertEquals("123456",verify.path("token").asText());
        assertTrue(paths.get(2).endsWith("/auth/v1/user")); assertEquals("{\"password\":\"new-secret\"}",bodies.get(2)); assertEquals("Bearer temporary-access",headers.get(2).get("Authorization"));
        assertEquals(previous,manager.current()); assertFalse(Files.exists(stored)); assertEquals("Bearer temporary-access",headers.get(3).get("Authorization"));
    }

    @Test void traduceCodigosDeRecuperacionYConservaMensajeAuthExistente() {
        assertEquals("El c\u00f3digo es incorrecto o ya venci\u00f3. Solicita uno nuevo.", AuthException.from(403,"{\"code\":\"otp_expired\"}").getMessage());
        assertEquals("La nueva contrase\u00f1a debe ser distinta de la anterior.", AuthException.from(422,"{\"code\":\"same_password\"}").getMessage());
        assertEquals("Por seguridad, solicita un nuevo c\u00f3digo e int\u00e9ntalo de nuevo.", AuthException.from(400,"{\"code\":\"reauthentication_needed\"}").getMessage());
        assertTrue(AuthException.from(429,"{\"code\":\"over_email_send_rate_limit\"}").getMessage().startsWith("Se alcanzó el límite de correos."));
        assertTrue(AuthException.from(401,"{\"code\":\"email_address_not_authorized\"}").getMessage().contains("SMTP personalizado"));
        assertTrue(AuthException.from(400,"{\"code\":\"invalid_credentials\"}").getMessage().contains("incorrectos"));
        assertTrue(AuthException.from(429,"{}").getMessage().contains("Demasiados"));
    }

    @Test void throttleAplicaVentanaCooldownIntentosYLiberacion() {
        RecoveryThrottle throttle=new RecoveryThrottle(clock);
        for(int i=0;i<5;i++) assertTrue(throttle.tryAcquireExistenceCheck());
        assertFalse(throttle.tryAcquireExistenceCheck());
        assertTrue(throttle.tryAcquireSend(" User@Example.COM ")); assertEquals(60,throttle.resendSeconds("user@example.com"));
        assertFalse(throttle.tryAcquireSend("user@example.com"));
        for(int i=1;i<=5;i++) assertEquals(i,throttle.recordInvalidCode("user@example.com"));
        assertTrue(throttle.isCodeLocked("user@example.com"));
        clock.advance(Duration.ofSeconds(60)); assertEquals(0,throttle.resendSeconds("user@example.com")); assertTrue(throttle.tryAcquireSend("user@example.com"));
        assertFalse(throttle.isCodeLocked("user@example.com"));
        clock.advance(Duration.ofSeconds(540)); assertTrue(throttle.tryAcquireExistenceCheck());
    }

    @Test void throttlePermiteCincoCorreosPorHoraYLiberaElMasAntiguo() {
        RecoveryThrottle throttle=new RecoveryThrottle(clock);
        for (int i=0; i<5; i++) {
            assertTrue(throttle.tryAcquireSend("user@example.com"));
            if (i<4) clock.advance(Duration.ofSeconds(60));
        }
        assertEquals(5, RecoveryPolicy.MAX_RECOVERY_EMAILS_PER_HOUR);
        assertEquals(RecoveryPolicy.RECOVERY_EMAIL_WINDOW_SECONDS-240, throttle.resendSeconds("user@example.com"));
        assertFalse(throttle.tryAcquireSend("user@example.com"));
        clock.advance(Duration.ofSeconds(RecoveryPolicy.RECOVERY_EMAIL_WINDOW_SECONDS-240));
        assertEquals(0, throttle.resendSeconds("user@example.com"));
        assertTrue(throttle.tryAcquireSend("user@example.com"));
    }

    @Test void recoveryServiceEvitaEnviarSiNoExisteONoEstaVerificada() throws Exception {
        for (var pair : List.of(Map.entry("\"not_found\"", PasswordRecoveryService.StartStatus.NOT_FOUND), Map.entry("\"unverified\"", PasswordRecoveryService.StartStatus.UNVERIFIED))) {
            List<String> paths=new ArrayList<>(); Queue<HttpTransport.Response> q=new ArrayDeque<>(List.of(new HttpTransport.Response(200,pair.getKey())));
            PasswordRecoveryService service=new PasswordRecoveryService(auth((m,u,h,b)->{paths.add(u.getPath());return q.remove();},manager()),new RecoveryThrottle(clock));
            assertEquals(pair.getValue(),service.start("a@example.com").status()); assertEquals(1,paths.size()); assertFalse(paths.get(0).endsWith("/auth/v1/recover"));
        }
    }

    @Test void verifiedSendsCodeAndGenericPolicySkipsExistenceLookup() throws Exception {
        List<String> revealedPaths=new ArrayList<>(); Queue<HttpTransport.Response> revealed=new ArrayDeque<>(List.of(new HttpTransport.Response(200,"\"verified\""),new HttpTransport.Response(200,"{}")));
        PasswordRecoveryService service=new PasswordRecoveryService(auth((m,u,h,b)->{revealedPaths.add(u.getPath());return revealed.remove();},manager()),new RecoveryThrottle(clock));
        assertEquals(PasswordRecoveryService.StartStatus.CODE_SENT,service.start("a@example.com").status()); assertTrue(revealedPaths.get(1).endsWith("/auth/v1/recover"));
        List<String> genericPaths=new ArrayList<>(); PasswordRecoveryService generic=new PasswordRecoveryService(auth((m,u,h,b)->{genericPaths.add(u.getPath());return new HttpTransport.Response(200,"{}");},manager()),new RecoveryThrottle(clock),false);
        assertEquals(PasswordRecoveryService.StartStatus.GENERIC_SENT,generic.start("a@example.com").status()); assertEquals(List.of("/auth/v1/recover"),genericPaths);
    }

    @Test void quintoCodigoInvalidoBloqueaYSesionExitosaActualizaYRevoca() throws Exception {
        List<String> paths=new ArrayList<>(); Queue<HttpTransport.Response> failed=new ArrayDeque<>(); for(int i=0;i<5;i++)failed.add(new HttpTransport.Response(403,"{\"code\":\"otp_expired\"}"));
        PasswordRecoveryService service=new PasswordRecoveryService(auth((m,u,h,b)->{paths.add(u.getPath());return failed.remove();},manager()),new RecoveryThrottle(clock));
        for(int i=1;i<5;i++) assertEquals(PasswordRecoveryService.CompleteStatus.INVALID_CODE,service.complete("a@example.com","111111","password-1"));
        assertEquals(PasswordRecoveryService.CompleteStatus.LOCKED,service.complete("a@example.com","111111","password-1")); assertEquals(5,paths.size());

        SessionManager manager=manager(); Session existing=new Session("existing","existing-refresh",initial.plusSeconds(4000),"existing-user","existing@example.com",""); manager.set(existing);
        List<String> successPaths=new ArrayList<>(); List<URI> successUris=new ArrayList<>(); Queue<HttpTransport.Response> q=new ArrayDeque<>(List.of(new HttpTransport.Response(200,sessionJson()),new HttpTransport.Response(200,"{}"),new HttpTransport.Response(204,""),new HttpTransport.Response(204,"")));
        PasswordRecoveryService successful=new PasswordRecoveryService(auth((m,u,h,b)->{successPaths.add(u.getPath());successUris.add(u);return q.remove();},manager),new RecoveryThrottle(clock));
        assertEquals(PasswordRecoveryService.CompleteStatus.SUCCESS,successful.complete("a@example.com","123456","password-2"));
        assertEquals(List.of("/auth/v1/verify","/auth/v1/user","/rest/v1/rpc/log_client_event","/auth/v1/logout"),successPaths); assertEquals(existing,manager.current()); assertTrue(successUris.get(3).toString().endsWith("/auth/v1/logout?scope=local"));
    }

    private static final class MutableClock extends Clock {
        private Instant now; MutableClock(Instant now){this.now=now;} void advance(Duration duration){now=now.plus(duration);}
        @Override public ZoneId getZone(){return ZoneOffset.UTC;} @Override public Clock withZone(ZoneId zone){return this;} @Override public Instant instant(){return now;}
    }
}
