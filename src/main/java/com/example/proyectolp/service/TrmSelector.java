package com.example.proyectolp.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;

/** Selecciona la TRM cuyo rango cubre hoy o la última publicada. */
public final class TrmSelector {
    public static final ZoneId ZONE = ZoneId.of("America/Bogota");
    private final Clock clock;
    public TrmSelector() { this(Clock.system(ZONE)); }
    public TrmSelector(Clock clock) { this.clock = clock.withZone(ZONE); }

    public TrmClient.TrmRecord select(List<TrmClient.TrmRecord> records) throws TrmException {
        if (records == null || records.isEmpty()) throw new TrmException("La fuente de TRM no devolvió datos.");
        LocalDate today = LocalDate.now(clock);
        TrmClient.TrmRecord current = records.stream().filter(r -> r.validFrom() != null && r.validTo() != null)
                .filter(r -> !today.isBefore(r.validFrom()) && !today.isAfter(r.validTo()))
                .max(Comparator.comparing(TrmClient.TrmRecord::validFrom))
                .orElse(null);
        if (current != null) return current;
        return records.stream().filter(r -> r.validFrom() != null)
                .max(Comparator.comparing(TrmClient.TrmRecord::validFrom))
                .orElseThrow(() -> new TrmException("La fuente de TRM no devolvió registros con fecha válida."));
    }
}
