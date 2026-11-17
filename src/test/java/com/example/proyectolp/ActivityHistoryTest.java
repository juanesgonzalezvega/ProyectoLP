package com.example.proyectolp;

import com.example.proyectolp.config.AppConfig;
import com.example.proyectolp.model.ActivityEntry;
import com.example.proyectolp.model.Session;
import com.example.proyectolp.model.enums.ActivityCategory;
import com.example.proyectolp.repository.ActivityRepository;
import com.example.proyectolp.service.ActivityEventLogger;
import com.example.proyectolp.service.ActivityFormatter;
import com.example.proyectolp.service.ActivityHistoryService;
import com.example.proyectolp.service.ActivityQueryBuilder;
import com.example.proyectolp.service.HttpTransport;
import com.example.proyectolp.service.SessionManager;
import com.example.proyectolp.service.SupabaseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.net.URI;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class ActivityHistoryTest {
    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-11-17T20:20:00Z"), ZoneId.of("America/Bogota"));

    @Test
    void buildsEncodedCategoryDateSearchAndPaginationFilters() {
        String query = ActivityQueryBuilder.build(ActivityCategory.TASK,
                LocalDate.of(2026, 11, 16), LocalDate.of(2026, 11, 17),
                "50%, * (tarea), acción", 50, 100);
        assertTrue(query.contains("category=eq.tarea"));
        assertTrue(query.contains("occurred_at=gte.2026-11-16T05%3A00%3A00Z"));
        assertTrue(query.contains("occurred_at=lt.2026-11-18T05%3A00%3A00Z"));
        assertTrue(query.contains("summary=ilike.*"));
        assertTrue(query.contains("%25"));
        assertTrue(query.contains("%2C"));
        assertTrue(query.contains("%28"));
        assertTrue(query.contains("%C3%B3"));
        assertTrue(query.contains("limit=50"));
        assertTrue(query.contains("offset=100"));
    }

    @Test
    void finalDayIsInclusiveAcrossBogotaMidnight() {
        String query = ActivityQueryBuilder.build(ActivityCategory.ALL, null,
                LocalDate.of(2026, 1, 1), "", 50, 0);
        assertTrue(query.contains("occurred_at=lt.2026-01-02T05%3A00%3A00Z"));
    }

    @Test
    void formatsDatesRelativeTimeCategoriesAndChangesSafely() throws Exception {
        ActivityFormatter formatter = new ActivityFormatter(FIXED_CLOCK);
        Instant event = Instant.parse("2026-11-17T20:15:00Z");
        assertTrue(formatter.formatDate(event).contains("17 nov 2026"));
        assertTrue(formatter.relativeTime(event).toLowerCase().contains("5 min"));
        assertTrue(formatter.categoryLabel("habito").equals("Hábitos"));
        assertEquals("Todas", ActivityCategory.ALL.toString());
        assertEquals(List.of("estado: pendiente → completada"),
                formatter.changeDetails(new ObjectMapper().readTree(
                        "{\"estado\":{\"antes\":\"pendiente\",\"despues\":\"completada\"}}")));
        assertEquals(List.of("estado: pendiente → completada"),
                formatter.changeDetails(new ObjectMapper().readTree(
                        "{\"cambios\":{\"estado\":{\"antes\":\"pendiente\",\"despues\":\"completada\"}}}")));
        assertTrue(formatter.changeDetails(null).isEmpty());
        assertTrue(formatter.changeDetails(new ObjectMapper().readTree("[]")).isEmpty());
        assertEquals("", formatter.detailText(null));
        assertEquals(200, formatter.truncate("x".repeat(220)).length());
    }

    @Test
    void mapsSnakeCaseAndIgnoresUnknownJsonFields() throws Exception {
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        ActivityEntry entry = mapper.readValue("""
                {"id":4,"user_id":"user","occurred_at":"2026-11-17T20:20:00Z",
                 "category":"tarea","action":"completar","entity_type":"tarea","entity_id":"7",
                 "summary":"Completó la tarea","metadata":{"estado":{"antes":"pendiente","despues":"completada"}},
                 "source":"trigger","unexpected":"ignored"}
                """, ActivityEntry.class);
        assertEquals(4, entry.getId());
        assertEquals("user", entry.userId);
        assertEquals("2026-11-17T20:20:00Z", entry.getOccurredAt().toString());
        assertEquals("completar", entry.getAction());
        assertTrue(entry.getMetadata().has("estado"));
    }

    @Test
    void serviceDeduplicatesAndPreservesPreviousPageOnError() throws Exception {
        List<ActivityHistoryService.Filter> filters = new ArrayList<>();
        AtomicBoolean fail = new AtomicBoolean();
        ActivityHistoryService service = new ActivityHistoryService((filter, limit, offset) -> {
            filters.add(filter);
            if (fail.get()) throw new IllegalStateException("offline");
            if (offset > 0) return List.of(entry(49), entry(51));
            List<ActivityEntry> page = new ArrayList<>();
            for (int i = 0; i < 50; i++) page.add(entry(i));
            return page;
        });
        var firstFilter = new ActivityHistoryService.Filter(ActivityCategory.ALL, null, null, "");
        assertEquals(50, service.loadFirst(firstFilter).entries().size());
        assertEquals(51, service.loadMore().entries().size());
        var newFilter = new ActivityHistoryService.Filter(ActivityCategory.TASK, null, null, "tarea");
        assertEquals(50, service.loadFirst(newFilter).entries().size());
        assertEquals(newFilter, filters.get(filters.size() - 1));
        fail.set(true);
        assertThrows(IllegalStateException.class, () -> service.loadFirst(
                new ActivityHistoryService.Filter(ActivityCategory.HABIT, null, null, "")));
        assertEquals(50, service.snapshot().entries().size());
        assertEquals(0, new ActivityHistoryService((filter, limit, offset) -> List.of())
                .loadFirst(firstFilter).entries().size());
    }

    @Test
    void eventLoggerUsesWhitelistIgnoresFailuresAndClosesBeforeRevoke() {
        List<String> calls = new ArrayList<>();
        ActivityEventLogger logger = new ActivityEventLogger((action, metadata) -> {
            calls.add(action);
            if ("inicio_sesion".equals(action)) throw new IllegalStateException("offline");
        }, Runnable::run);
        assertFalse(logger.logBestEffort("arbitrary"));
        assertTrue(logger.logBestEffort("inicio_sesion"));
        AtomicBoolean revoked = new AtomicBoolean();
        logger = new ActivityEventLogger((action, metadata) -> calls.add(action), Runnable::run);
        ActivityEventLogger finalLogger = logger;
        finalLogger.logCloseBefore(() -> {
            assertEquals("cierre_sesion", calls.get(calls.size() - 1));
            revoked.set(true);
        });
        assertTrue(revoked.get());
        assertTrue(calls.contains("inicio_sesion"));
        assertTrue(calls.contains("cierre_sesion"));
    }

    @Test
    void repositoryUsesPostgrestAndTheEventRpcThroughFakeTransport() throws Exception {
        SessionManager sessions = new SessionManager(Clock.systemUTC());
        sessions.set(new Session("jwt", "refresh", Instant.now().plusSeconds(3600), "user-id", "", ""));
        AtomicReference<URI> lastUri = new AtomicReference<>();
        AtomicReference<String> requestBody = new AtomicReference<>();
        HttpTransport transport = (method, uri, headers, body) -> {
            lastUri.set(uri);
            requestBody.set(body);
            assertEquals("Bearer jwt", headers.get("Authorization"));
            if (method.equals("GET")) {
                return new HttpTransport.Response(200, """
                        [{"id":8,"user_id":"user-id","occurred_at":"2026-11-17T20:20:00Z",
                         "category":"tarea","action":"crear","summary":"Creó la tarea","metadata":{},"source":"trigger"}]
                        """);
            }
            return new HttpTransport.Response(204, "");
        };
        SupabaseService db = new SupabaseService(new AppConfig("https://example.supabase.co", "public-key"),
                transport, sessions, null);
        ActivityRepository repository = new ActivityRepository(db);
        assertEquals(8, repository.latest(5).getFirst().getId());
        assertTrue(lastUri.get().toString().contains("/rest/v1/activity_log?"));
        repository.logClientEvent("inicio_sesion", Map.of());
        assertTrue(lastUri.get().toString().endsWith("/rest/v1/rpc/log_client_event"));
        assertTrue(requestBody.get().contains("\"p_action\":\"inicio_sesion\""));
    }

    @Test
    void sqlEnforcesReadOnlyHistoryWhitelistAndConditionalTriggers() throws Exception {
        String sql = Files.readString(Path.of("supabase/13_historial_actividad.sql")).toLowerCase();
        assertTrue(sql.contains("create table if not exists public.activity_log"));
        assertTrue(sql.contains("enable row level security"));
        assertTrue(sql.contains("for select to authenticated using (user_id = (select auth.uid()))"));
        assertTrue(sql.contains("revoke all on table public.activity_log from public, anon, authenticated"));
        java.util.regex.Matcher policies = java.util.regex.Pattern
                .compile("create policy [^;]+ on public\\.activity_log").matcher(sql);
        int policyCount = 0;
        while (policies.find()) policyCount++;
        assertEquals(1, policyCount);
        assertFalse(sql.contains("grant insert on table public.activity_log"));
        assertFalse(sql.contains("grant update on table public.activity_log"));
        assertFalse(sql.contains("grant delete on table public.activity_log"));
        String clientEventFunction = sql.substring(sql.indexOf("create or replace function public.log_client_event"));
        java.util.regex.Matcher actions = java.util.regex.Pattern
                .compile("when '([a-z_]+)' then").matcher(clientEventFunction);
        java.util.Set<String> allowedActions = new java.util.HashSet<>();
        while (actions.find()) allowedActions.add(actions.group(1));
        assertEquals(java.util.Set.of("inicio_sesion", "cierre_sesion", "contrasena_actualizada",
                "entrenamiento_finalizado"), allowedActions);
        assertTrue(sql.contains("to_regclass('public.' || v_table)"));
        assertTrue(sql.contains("'tasks', 'projects', 'stages', 'habits', 'habit_logs'"));
        assertTrue(sql.contains("'credit_ledger', 'redemptions', 'user_badges', 'daily_missions', 'attachments'"));
        assertFalse(sql.contains("'user_stats'"));
        assertFalse(sql.contains("'xp_log'"));
        assertFalse(sql.contains("'exercises'"));
        assertFalse(sql.contains("'merchants'"));
        assertTrue(sql.contains("pg_catalog.pg_trigger_depth() > 1"));
        assertTrue(sql.contains("create trigger activity_log_project_delete before delete on public.projects"));
    }

    @Test
    void historyFxmlHasNoInlineStylesAndThemeClassesExist() throws Exception {
        String fxml = Files.readString(Path.of("src/main/resources/com/example/proyectolp/ui/activity-history.fxml"));
        String theme = Files.readString(Path.of("src/main/resources/com/example/proyectolp/css/theme.css"));
        assertFalse(fxml.matches("(?s).*\\sstyle\\s*=.*"));
        for (String styleClass : List.of("app-bg", "kicker", "h2", "btn-ghost", "btn-primary",
                "text-error", "card")) {
            assertTrue(theme.contains("." + styleClass), "Falta la clase " + styleClass);
            assertTrue(fxml.contains("styleClass=\"" + styleClass + "\""));
        }
    }

    private static ActivityEntry entry(long id) {
        ActivityEntry entry = new ActivityEntry();
        entry.id = id;
        entry.summary = "Actividad " + id;
        return entry;
    }
}
