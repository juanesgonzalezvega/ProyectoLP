package com.example.proyectolp;

import com.example.proyectolp.service.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Clock;
import java.time.ZoneId;
import static org.junit.jupiter.api.Assertions.*;

class GamificationRulesTest {
 @Test void goalCreditTotalsAndCrossedMilestonesFollowFixedThresholds(){
  assertEquals(0,CreditRules.goalTotal(new BigDecimal("59999")));
  assertEquals(3,CreditRules.goalTotal(new BigDecimal("60000")));
  assertEquals(4,CreditRules.goalTotal(new BigDecimal("61000")));
  assertEquals(4,CreditRules.goalTotal(new BigDecimal("70000")));
  assertEquals(4,CreditRules.goalTotal(new BigDecimal("78000")));
  assertEquals(4,CreditRules.crossedGoalMilestones(new BigDecimal("78000"),BigDecimal.ZERO,new BigDecimal("78000")));
  assertEquals(2,CreditRules.crossedGoalMilestones(new BigDecimal("100000"),new BigDecimal("19000"),new BigDecimal("41000")));
 }
 @Test void actionCreditsAndHabitDateAreExact(){
  LocalDate today=LocalDate.of(2026,10,21);
  assertEquals(3,CreditRules.task(false));assertEquals(1,CreditRules.task(true));assertEquals(1,CreditRules.stage());
  assertEquals(1,CreditRules.habit(true,today,today));assertEquals(0,CreditRules.habit(true,today.minusDays(1),today));assertEquals(0,CreditRules.habit(false,today,today));
 }
 @Test void penaltyDivisorsFloorAndLostPenaltyIsIdempotent(){
  assertEquals(2,CreditRules.penaltyDivisor(5));assertEquals(2,CreditRules.penaltyDivisor(10));assertEquals(3,CreditRules.penaltyDivisor(11));assertEquals(3,CreditRules.penaltyDivisor(30));assertEquals(4,CreditRules.penaltyDivisor(31));
  assertEquals(3,CreditRules.penalizedBalance(7,2));assertEquals(-4,CreditRules.penaltyDelta(7,2));assertEquals(0,CreditRules.penaltyDelta(0,2));
  LocalDate old=LocalDate.of(2026,10,18),today=LocalDate.of(2026,10,21);assertTrue(StreakRules.lost(old,null,today));assertFalse(StreakRules.lost(old,old,today));
 }
 @Test void streakTransitionsRespectTodayYesterdayAndGaps(){
  LocalDate today=LocalDate.of(2026,10,21);Clock fixed=Clock.fixed(today.atStartOfDay(ZoneId.of("America/Bogota")).toInstant(),ZoneId.of("America/Bogota"));today=LocalDate.now(fixed);var same=StreakRules.action(4,8,today,today);assertEquals(4,same.current());
  var next=StreakRules.action(4,8,today.minusDays(1),today);assertEquals(5,next.current());assertEquals(8,next.best());
  var gap=StreakRules.action(4,8,today.minusDays(3),today);assertEquals(1,gap.current());
 }
 @Test void levelsRanksProgressAndDedupeKeysAreStable(){
  assertEquals(100,LevelCurve.threshold(2));assertEquals(300,LevelCurve.threshold(3));assertEquals(1,LevelCurve.level(99));assertEquals(2,LevelCurve.level(100));
  assertEquals("Novato",LevelCurve.rank(4));assertEquals("Aprendiz",LevelCurve.rank(5));assertEquals("Constante",LevelCurve.rank(10));assertEquals("Experto",LevelCurve.rank(20));assertEquals("Maestro",LevelCurve.rank(35));
  assertEquals(.5,LevelCurve.progress(200),0.0001);assertEquals(CreditRules.taskKey(9),CreditRules.taskKey(9));assertEquals(CreditRules.goalKey(3,2),CreditRules.goalKey(3,2));assertEquals(CreditRules.habitKey(4,LocalDate.of(2026,1,2)),CreditRules.habitKey(4,LocalDate.of(2026,1,2)));
 }
}
