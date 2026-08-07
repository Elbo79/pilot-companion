package com.pilotcompanion.app;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class ScheduleRepository {
    private static final ZoneId UTC = ZoneId.of("UTC");
    private final Map<LocalDate, List<FlightLeg>> schedule = new HashMap<>();

    ScheduleRepository() {
        addUtc("UPS52", "ANC", "HKG", "2026-08-03T18:47", "2026-08-04T04:44", "2026-08-03T18:08", "2026-08-04T05:10", "F/O", "Crew Access revision");
        addUtc("UPS67", "HKG", "ICN", "2026-08-06T02:37", "2026-08-06T06:18", "2026-08-06T02:15", "2026-08-06T05:45", "F/O", "Crew Access revision");
        addUtc("UPS6011", "ICN", "DEL", "2026-08-08T01:19", "2026-08-08T08:46", null, null, "F/O", "Crew Access");
        addUtc("UPS15", "DEL", "CGN", "2026-08-09T08:10", "2026-08-09T17:06", null, null, "F/O", "Crew Access");
        addUtc("UPS224", "CGN", "SDF", "2026-08-10T17:25", "2026-08-11T02:17", null, null, "R/O", "Crew Access");
        addUtc("UPS5074", "SDF", "ANC", "2026-08-11T19:50", "2026-08-12T02:44", null, null, "DH", "Crew Access");

        addUtc("UPS64", "ANC", "ICN", "2026-08-19T17:37", "2026-08-20T02:25", null, null, "F/O", "Crew Access");
        addUtc("UPS11", "ICN", "DEL", "2026-08-21T02:33", "2026-08-21T10:00", null, null, "R/O", "Crew Access");
        addUtc("UPS17", "DEL", "CGN", "2026-08-23T13:00", "2026-08-23T21:56", null, null, "R/O", "Crew Access");
        addUtc("UPS14", "CGN", "SZX", "2026-08-25T03:47", "2026-08-25T15:30", null, null, "F/O", "Crew Access");
        addUtc("UPS77", "SZX", "ANC", "2026-08-26T11:10", "2026-08-26T21:13", null, null, "F/O", "Crew Access");

        addUtc("UPS197", "ANC", "SDF", "2026-09-04T07:13", "2026-09-04T13:26", null, null, "F/O", "Crew Access");
        addUtc("UPS36", "SDF", "HNL", "2026-09-05T11:26", "2026-09-05T20:31", null, null, "F/O", "Crew Access");
        addUtc("UPS35", "HNL", "ICN", "2026-09-07T06:45", "2026-09-07T16:20", null, null, "DH", "Crew Access revision");
        addUtc("UPS99", "ICN", "ANC", "2026-09-08T12:50", "2026-09-08T20:51", null, null, "F/O", "Crew Access revision");
    }

    private void addUtc(String number, String from, String to, String actualOut, String actualIn,
                        String scheduledOut, String scheduledIn, String assignment, String source) {
        ZonedDateTime departure = ZonedDateTime.parse(actualOut + "Z").withZoneSameInstant(airportZone(from));
        ZonedDateTime arrival = ZonedDateTime.parse(actualIn + "Z").withZoneSameInstant(airportZone(to));
        ZonedDateTime scheduledDeparture = scheduledOut == null ? departure
                : ZonedDateTime.parse(scheduledOut + "Z").withZoneSameInstant(airportZone(from));
        ZonedDateTime scheduledArrival = scheduledIn == null ? arrival
                : ZonedDateTime.parse(scheduledIn + "Z").withZoneSameInstant(airportZone(to));
        FlightLeg leg = new FlightLeg(number, from, to, departure, arrival,
                scheduledDeparture, scheduledArrival, assignment, source);
        schedule.computeIfAbsent(departure.toLocalDate(), ignored -> new ArrayList<>()).add(leg);
    }

    List<FlightLeg> forDate(LocalDate date) {
        return Collections.unmodifiableList(schedule.getOrDefault(date, List.of()));
    }

    LocalDate firstScheduledDate() {
        return schedule.keySet().stream().min(LocalDate::compareTo).orElse(LocalDate.now());
    }

    private static ZoneId airportZone(String airport) {
        return switch (airport) {
            case "ANC" -> ZoneId.of("America/Anchorage");
            case "SDF" -> ZoneId.of("America/Kentucky/Louisville");
            case "HNL" -> ZoneId.of("Pacific/Honolulu");
            case "HKG" -> ZoneId.of("Asia/Hong_Kong");
            case "ICN" -> ZoneId.of("Asia/Seoul");
            case "SZX" -> ZoneId.of("Asia/Shanghai");
            case "DEL" -> ZoneId.of("Asia/Kolkata");
            case "CGN" -> ZoneId.of("Europe/Berlin");
            default -> UTC;
        };
    }
}
