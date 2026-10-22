package com.example.proyectolp.service;

import com.example.proyectolp.model.UserStats;
import com.example.proyectolp.repository.CreditLedgerRepository;
import com.example.proyectolp.repository.UserStatsRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.text.NumberFormat;
import java.util.Locale;

public class StreakService {
    private final UserStatsRepository stats;
    private final CreditLedgerRepository ledger;
    private final Clock clock;
    public StreakService(SupabaseService db, Clock clock) { stats=new UserStatsRepository(db); ledger=new CreditLedgerRepository(db); this.clock=clock; }
    private LocalDate today() { return LocalDate.now(clock.withZone(ZoneId.of("America/Bogota"))); }
    public synchronized void beforeAction() throws Exception {
        UserStats s=stats.get(); LocalDate now=today();
        if(!StreakRules.lost(s.last_activity_date,s.last_penalty_for,now))return;
        int balance=ledger.balance(), divisor=CreditRules.penaltyDivisor(s.current_streak);
        int delta=CreditRules.penaltyDelta(balance,divisor);
        if(balance>0&&ledger.insert("penalizacion",delta,"Penalización por racha perdida (÷"+divisor+")","penalty:"+s.last_activity_date)) {
            String money=NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO")).format(delta*CreditRules.COP_PER_CREDIT);
            NotificationService.getInstance().show(delta+" créditos ("+money+") por racha perdida");
        }
        s.current_streak=0; s.last_penalty_for=s.last_activity_date; stats.save(s);
    }
    public synchronized void recordAction() throws Exception {
        UserStats s=stats.get(); StreakRules.Transition next=StreakRules.action(s.current_streak,s.best_streak,s.last_activity_date,today());
        s.current_streak=next.current(); s.best_streak=next.best(); s.last_activity_date=next.last(); stats.save(s);
        GamificationRuntime.service().refreshStreakBadges();
    }
    public void checkPenalty() throws Exception { beforeAction(); }
    public UserStats getStats() throws Exception { return stats.get(); }
}
