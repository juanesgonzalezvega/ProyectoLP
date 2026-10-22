package com.example.proyectolp;

import com.example.proyectolp.model.TrainingModels.*;
import com.example.proyectolp.service.TrainingStatsService;
import com.example.proyectolp.service.RestTimer;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class TrainingStatsTest {
    private final TrainingStatsService stats=new TrainingStatsService();
    private SetEntry set(long session,String muscle,String exercise,String weight,int reps){return new SetEntry(null,session,exercise,muscle,1,new BigDecimal(weight),reps,"");}
    @Test void sessionVolumeSumsEveryRegisteredSet(){assertEquals(new BigDecimal("160"),stats.volume(List.of(set(1,"pecho","Press","40",3),set(1,"pecho","Press","40",1))));}
    @Test void periodVolumeFiltersSessionsByDateAndFrequencyIsWeekly(){List<Session>s=List.of(new Session(1L,null,LocalDate.of(2026,10,1),""),new Session(2L,null,LocalDate.of(2026,9,1),""));assertEquals(new BigDecimal("100"),stats.volumeForSessions(s,List.of(set(1,"pecho","Press","25",4),set(2,"pecho","Press","50",2)),LocalDate.of(2026,10,1),LocalDate.of(2026,10,31)));assertEquals(1.0,stats.sessionsPerWeek(s,LocalDate.of(2026,10,1),LocalDate.of(2026,10,7)));}
    @Test void comparesMaximumWeightRepsAndVolumeAcrossSessions(){List<Session>s=List.of(new Session(1L,null,LocalDate.of(2026,10,1),""),new Session(2L,null,LocalDate.of(2026,10,8),""));var c=stats.compareLastTwo("Press",s,List.of(set(1,"pecho","Press","40",8),set(2,"pecho","Press","45",10))).orElseThrow();assertEquals(new BigDecimal("5"),c.weightDelta());assertEquals(2,c.repsDelta());assertEquals(new BigDecimal("130"),c.volumeDelta());}
    @Test void musclePercentagesSumToOneHundredAndEmptyIsEmpty(){List<Session>s=List.of(new Session(1L,null,LocalDate.now(),""));Map<String,BigDecimal> d=stats.muscleDistribution(s,List.of(set(1,"pecho","A","20",5),set(1,"espalda","B","20",5)),LocalDate.now(),LocalDate.now());assertEquals(new BigDecimal("100.00"),d.values().stream().reduce(BigDecimal.ZERO,BigDecimal::add));assertTrue(stats.muscleDistribution(s,List.of(),LocalDate.now(),LocalDate.now()).isEmpty());}
    @Test void restTimerCountsPausesAndResetsWithoutJavaFxScheduler(){RestTimer timer=new RestTimer(3);timer.startClockless();timer.elapseOneSecond();assertEquals(2,timer.getRemainingSeconds());timer.pause();timer.elapseOneSecond();assertEquals(2,timer.getRemainingSeconds());timer.startClockless();timer.elapseOneSecond();timer.elapseOneSecond();assertEquals(RestTimer.State.FINISHED,timer.getState());timer.reset();assertEquals(3,timer.getRemainingSeconds());assertEquals(RestTimer.State.READY,timer.getState());}
}
