package com.pilotcompanion.app;

import android.content.Context;
import org.json.JSONObject;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

final class ScheduleRepository {
    private final Map<LocalDate,List<FlightLeg>> schedule=new HashMap<>();
    private PilotDatabase database;
    ScheduleRepository() {}
    ScheduleRepository(Context context){ attach(context); }
    void attach(Context context){
        database=new PilotDatabase(context);
        schedule.clear();
        mergeInMemory(database.loadFlights());
    }
    void mergeImported(List<FlightLeg> imported){
        if(database!=null){
            database.saveFlights(imported);
            schedule.clear();
            mergeInMemory(database.loadFlights());
        } else mergeInMemory(imported);
    }
    private void mergeInMemory(List<FlightLeg> imported){
        for(FlightLeg leg:imported){
            for(List<FlightLeg> list:schedule.values())list.removeIf(old->PilotDatabase.identity(old).equals(PilotDatabase.identity(leg)));
            schedule.computeIfAbsent(leg.departure().toLocalDate(),ignored->new ArrayList<>()).add(leg);
        }
    }
    static ZonedDateTime zoned(String instant,String airport){return Instant.parse(instant).atZone(airportZone(airport));}
    static FlightLeg fromSeed(JSONObject o) throws Exception {
        String from=o.getString("from"),to=o.getString("to");
        ZonedDateTime departure=LocalDateTime.parse(o.getString("out")).atZone(ZoneOffset.UTC).withZoneSameInstant(airportZone(from));
        ZonedDateTime arrival=LocalDateTime.parse(o.getString("in")).atZone(ZoneOffset.UTC).withZoneSameInstant(airportZone(to));
        ZonedDateTime scheduledDeparture=o.isNull("scheduledOut")?departure:LocalDateTime.parse(o.getString("scheduledOut")).atZone(ZoneOffset.UTC).withZoneSameInstant(airportZone(from));
        ZonedDateTime scheduledArrival=o.isNull("scheduledIn")?arrival:LocalDateTime.parse(o.getString("scheduledIn")).atZone(ZoneOffset.UTC).withZoneSameInstant(airportZone(to));
        return new FlightLeg(o.getString("flight"),from,to,departure,arrival,scheduledDeparture,scheduledArrival,o.getString("assignment"),o.getString("source"),o.isNull("hotel")?null:o.getString("hotel"),FlightLeg.ChangeType.valueOf(o.getString("change")),o.getString("pairing"));
    }
    List<FlightLeg> forDate(LocalDate date){return Collections.unmodifiableList(schedule.getOrDefault(date,List.of()));}
    FlightLeg nextAssignmentAfter(Instant instant){return schedule.values().stream().flatMap(List::stream).filter(l->l.departure().toInstant().isAfter(instant)).min(Comparator.comparing(l->l.departure().toInstant())).orElse(null);}
    LocalDate firstScheduledDate(){return schedule.keySet().stream().min(LocalDate::compareTo).orElse(LocalDate.now());}
    LocalDate nearestScheduledDate(LocalDate target){return schedule.keySet().stream().min(Comparator.comparingLong(d->Math.abs(ChronoUnit.DAYS.between(target,d)))).orElse(target);}
    private static ZoneId airportZone(String airport){return switch(airport){
        case "ANC"->ZoneId.of("America/Anchorage");case "SDF"->ZoneId.of("America/Kentucky/Louisville");
        case "HNL"->ZoneId.of("Pacific/Honolulu");case "HKG"->ZoneId.of("Asia/Hong_Kong");
        case "ICN"->ZoneId.of("Asia/Seoul");case "SZX","PVG"->ZoneId.of("Asia/Shanghai");
        case "DEL"->ZoneId.of("Asia/Kolkata");case "CGN"->ZoneId.of("Europe/Berlin");
        case "CRK"->ZoneId.of("Asia/Manila");case "TPE"->ZoneId.of("Asia/Taipei");
        default->ZoneOffset.UTC;};}
}
