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
    void mergeImported(List<FlightLeg> imported){ mergeReplacing(imported, null); }
    void mergeReplacing(List<FlightLeg> imported, String replacedPairing){
        if(database!=null){
            database.saveFlights(imported, replacedPairing);
            schedule.clear();
            mergeInMemory(database.loadFlights());
        } else {
            if (replacedPairing != null) for (List<FlightLeg> existing : schedule.values()) existing.removeIf(l -> replacedPairing.equals(l.pairing()));
            mergeInMemory(imported);
        }
    }
    // Start replacement at the old departure from the same airport, even when
    // the revised departure moves to the following day. Keep completed legs.
    Instant revisionCutoff(List<FlightLeg> incoming, String pairing) {
        if (incoming.isEmpty()) throw new IllegalArgumentException("No revised flights");
        List<FlightLeg> old = flightsForPairing(pairing);
        Instant first = incoming.stream().map(l -> l.departure().toInstant())
            .min(Instant::compareTo).orElseThrow();
        FlightLeg anchor = null;
        long best = Long.MAX_VALUE;
        for (FlightLeg fresh : incoming) for (FlightLeg previous : old) {
            long delta = Math.abs(Duration.between(previous.departure().toInstant(),
                fresh.departure().toInstant()).toHours());
            if (previous.origin().equals(fresh.origin()) && delta <= 72 && delta < best
) {
                best = delta;
                anchor = previous;
            }
        }
        if (anchor == null) throw new IllegalArgumentException(
            "No future departure from the same airport within 72 hours. Review manually.");
        return anchor.departure().toInstant();
    }
    List<FlightLeg> supersededLegs(String pairing, Instant cutoff) {
        List<FlightLeg> out = new ArrayList<>();
        for (FlightLeg old : flightsForPairing(pairing))
            if (!old.departure().toInstant().isBefore(cutoff)) out.add(old);
        return out;
    }
    void replaceRemainingTrip(List<FlightLeg> incoming, String pairing, Instant cutoff) {
        if (database != null) {
            database.replaceRemainingTrip(incoming, pairing, cutoff);
            schedule.clear();
            mergeInMemory(database.loadFlights());
        } else {
            for (List<FlightLeg> day : schedule.values())
                day.removeIf(l -> pairing.equals(l.pairing()) &&
                    !l.departure().toInstant().isBefore(cutoff));
            mergeInMemory(incoming);
        }
    }
    List<String> allPairings() {
        Set<String> names = new TreeSet<>();
        for (List<FlightLeg> day : schedule.values()) for (FlightLeg leg : day)
            if (leg.pairing() != null && !leg.pairing().isBlank()) names.add(leg.pairing());
        return new ArrayList<>(names);
    }
    List<FlightLeg> flightsForPairing(String pairing) {
        List<FlightLeg> legs = new ArrayList<>();
        for (List<FlightLeg> day : schedule.values()) for (FlightLeg leg : day)
            if (pairing.equals(leg.pairing())) legs.add(leg);
        legs.sort(Comparator.comparing(l -> l.departure().toInstant()));
        return legs;
    }
    void removePairing(String pairing) {
        if (database != null) {
            database.removePairing(pairing);
            schedule.clear();
            mergeInMemory(database.loadFlights());
        } else for (List<FlightLeg> day : schedule.values()) day.removeIf(l -> pairing.equals(l.pairing()));
    }
    List<String> candidatePairings(List<FlightLeg> incoming) {
        if (incoming.isEmpty()) return List.of();
        Instant first = incoming.stream().map(l -> l.departure().toInstant()).min(Instant::compareTo).orElseThrow();
        Instant last = incoming.stream().map(l -> l.arrival().toInstant()).max(Instant::compareTo).orElseThrow();
        Set<String> matches = new TreeSet<>();
        for (List<FlightLeg> day : schedule.values()) for (FlightLeg old : day) {
            if (old.pairing() != null && !old.pairing().isBlank()
                && !old.departure().toInstant().isAfter(last.plus(4, ChronoUnit.DAYS))
                && !old.arrival().toInstant().isBefore(first.minus(4, ChronoUnit.DAYS))) matches.add(old.pairing());
        }
        return new ArrayList<>(matches);
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
