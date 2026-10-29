package com.example.proyectolp.service;

import com.example.proyectolp.model.CatalogExercise;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
import java.util.concurrent.TimeoutException;

/** Cliente público de wger. */
public final class WgerClient {
    private static final ObjectMapper JSON = new ObjectMapper();
    private final HttpTransport transport;
    private final String baseUrl;
    public WgerClient() { this(new JdkHttpTransport(), WgerConfig.baseUrl()); }
    public WgerClient(HttpTransport transport) { this(transport, WgerConfig.DEFAULT_BASE_URL); }
    public WgerClient(HttpTransport transport, String baseUrl) { this.transport = transport; this.baseUrl = baseUrl.replaceAll("/+$", ""); }

    public List<CatalogExercise> search(String term) throws WgerException {
        String cleaned = term == null ? "" : term.trim();
        if (cleaned.length() < 3) return List.of();
        String query = URLEncoder.encode(cleaned, StandardCharsets.UTF_8);
        // Exerciseinfo expone datos completos, pero no tiene filtro textual; se descargan páginas y se filtra localmente.
        List<CatalogExercise> results = new ArrayList<>();
        int offset = 0;
        try {
            while (offset < 999 && results.size() < 20) {
                String url = baseUrl + "/exerciseinfo/?limit=100&offset=" + offset;
                HttpTransport.Response response = transport.send("GET", URI.create(url), Map.of("Accept", "application/json"), null);
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    if (response.statusCode() == 401 || response.statusCode() == 403) throw new WgerException("El catálogo rechazó la consulta (HTTP " + response.statusCode() + ").");
                    throw new WgerException("El catálogo respondió con un error (HTTP " + response.statusCode() + ").");
                }
                Page page = JSON.readValue(response.body(), Page.class);
                if (page.results == null) break;
                for (ExerciseRow row : page.results) {
                    if (row.translations == null) continue;
                    Translation es = null, en = null;
                    for (Translation tr : row.translations) {
                        if (tr.name == null || tr.name.isBlank()) continue;
                        if (tr.language == 4) es = tr;
                        if (tr.language == 2) en = tr;
                    }
                    Translation selected = es != null ? es : en;
                    if (selected == null || !selected.name.toLowerCase(Locale.ROOT).contains(cleaned.toLowerCase(Locale.ROOT))) continue;
                    List<String> muscles = row.muscles == null ? List.of() : row.muscles.stream().map(m -> m.name).toList();
                    String group = WgerMuscleMapper.map(muscles, row.category == null ? null : row.category.name);
                    results.add(new CatalogExercise(selected.name, group, es != null ? "español" : "inglés"));
                    if (results.size() == 20) break;
                }
                if (page.next == null || page.next.isBlank()) break;
                offset += page.results.size();
            }
            return List.copyOf(results);
        } catch (WgerException ex) { throw ex; }
        catch (SocketTimeoutException | java.net.http.HttpTimeoutException ex) { throw new WgerException("Se agotó el tiempo de espera al consultar el catálogo.", ex); }
        catch (InterruptedException ex) { Thread.currentThread().interrupt(); throw new WgerException("Se interrumpió la consulta del catálogo.", ex); }
        catch (IOException ex) { throw new WgerException("No hay conexión con el catálogo de ejercicios.", ex); }
        catch (Exception ex) { throw new WgerException("El catálogo devolvió datos JSON inválidos.", ex); }
    }
    @JsonIgnoreProperties(ignoreUnknown = true) public static final class Page { public List<ExerciseRow> results; public String next; }
    @JsonIgnoreProperties(ignoreUnknown = true) public static final class ExerciseRow { public Category category; public List<Muscle> muscles; public List<Translation> translations; }
    @JsonIgnoreProperties(ignoreUnknown = true) public static final class Category { public String name; }
    @JsonIgnoreProperties(ignoreUnknown = true) public static final class Muscle { public String name; }
    @JsonIgnoreProperties(ignoreUnknown = true) public static final class Translation { public String name; public int language; }
}
