package com.example.proyectolp.service;

import com.example.proyectolp.model.TrainingModels.*;
import java.math.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

public class TrainingStatsService {
    public record Progress(LocalDate date, BigDecimal maxWeight, int maxReps, BigDecimal volume) {}
    public record Comparison(Progress previous, Progress current, BigDecimal weightDelta, int repsDelta, BigDecimal volumeDelta) {}
    public record Summary(int sessions, BigDecimal volume) {}
    public BigDecimal volume(Collection<SetEntry> sets) { return sets.stream().map(s->s.weight().multiply(BigDecimal.valueOf(s.reps()))).reduce(BigDecimal.ZERO,BigDecimal::add); }
    public BigDecimal volumeForSessions(Collection<Session> sessions,Collection<SetEntry> sets,LocalDate from,LocalDate to) {
        Set<Long> ids=sessions.stream().filter(s->!s.sessionDate().isBefore(from)&&!s.sessionDate().isAfter(to)).map(Session::id).collect(Collectors.toSet());return volume(sets.stream().filter(s->ids.contains(s.sessionId())).toList());
    }
    public Map<String,BigDecimal> muscleDistribution(Collection<Session> sessions,Collection<SetEntry> sets,LocalDate from,LocalDate to) {
        Set<Long> ids=sessions.stream().filter(s->!s.sessionDate().isBefore(from)&&!s.sessionDate().isAfter(to)).map(Session::id).collect(Collectors.toSet());
        Map<String,BigDecimal> totals=sets.stream().filter(s->ids.contains(s.sessionId())).collect(Collectors.groupingBy(SetEntry::muscleGroup,TreeMap::new,Collectors.reducing(BigDecimal.ZERO,s->s.weight().multiply(BigDecimal.valueOf(s.reps())),BigDecimal::add)));
        BigDecimal total=totals.values().stream().reduce(BigDecimal.ZERO,BigDecimal::add); if(total.signum()==0)return Map.of();
        Map<String,BigDecimal> result=new LinkedHashMap<>();BigDecimal assigned=BigDecimal.ZERO;int i=0;
        for(var e:totals.entrySet()){i++;BigDecimal p=i==totals.size()?BigDecimal.valueOf(100).subtract(assigned):e.getValue().multiply(BigDecimal.valueOf(100)).divide(total,2,RoundingMode.HALF_UP);result.put(e.getKey(),p);assigned=assigned.add(p);}return result;
    }
    public List<Progress> progress(String exercise,Collection<Session> sessions,Collection<SetEntry> sets) {
        Map<Long,Session> byId=sessions.stream().collect(Collectors.toMap(Session::id,s->s));Map<LocalDate,List<SetEntry>> groups=sets.stream().filter(s->s.exerciseName().equalsIgnoreCase(exercise)&&byId.containsKey(s.sessionId())).collect(Collectors.groupingBy(s->byId.get(s.sessionId()).sessionDate(),TreeMap::new,Collectors.toList()));
        return groups.entrySet().stream().map(e->new Progress(e.getKey(),e.getValue().stream().map(SetEntry::weight).max(BigDecimal::compareTo).orElse(BigDecimal.ZERO),e.getValue().stream().mapToInt(SetEntry::reps).max().orElse(0),volume(e.getValue()))).toList();
    }
    public Optional<Comparison> compareLastTwo(String exercise,Collection<Session> sessions,Collection<SetEntry> sets) { List<Progress> p=progress(exercise,sessions,sets);if(p.size()<2)return Optional.empty();Progress a=p.get(p.size()-2),b=p.get(p.size()-1);return Optional.of(new Comparison(a,b,b.maxWeight().subtract(a.maxWeight()),b.maxReps()-a.maxReps(),b.volume().subtract(a.volume()))); }
    public Summary summary(Collection<Session> sessions,Collection<SetEntry> sets,LocalDate from,LocalDate to) { List<Session> period=sessions.stream().filter(s->!s.sessionDate().isBefore(from)&&!s.sessionDate().isAfter(to)).toList();return new Summary(period.size(),volumeForSessions(period,sets,from,to)); }
    public double sessionsPerWeek(Collection<Session> sessions,LocalDate from,LocalDate to) { long weeks=Math.max(1, ChronoUnit.WEEKS.between(from,to)+1);long n=sessions.stream().filter(s->!s.sessionDate().isBefore(from)&&!s.sessionDate().isAfter(to)).count();return (double)n/weeks; }
}
