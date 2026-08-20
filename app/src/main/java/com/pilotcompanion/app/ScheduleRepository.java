package com.pilotcompanion.app;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class ScheduleRepository {
    private static final ZoneId UTC = ZoneId.of("UTC");
    private final Map<LocalDate, List<FlightLeg>> schedule = new HashMap<>();

    ScheduleRepository() {
        addUtc("UPS52", "ANC", "HKG", "2026-08-03T18:47", "2026-08-04T04:44", "2026-08-03T18:08", "2026-08-04T05:10", "F/O", "Crew Access revision", "New World Millennium Hong Kong", FlightLeg.ChangeType.REVISED, "");
        addUtc("UPS67", "HKG", "ICN", "2026-08-06T02:37", "2026-08-06T06:18", "2026-08-06T02:15", "2026-08-06T05:45", "F/O", "Crew Access revision", "Sheraton Incheon", FlightLeg.ChangeType.REVISED, "");
        addUtc("UPS6011", "ICN", "DEL", "2026-08-08T01:19", "2026-08-08T08:46", null, null, "F/O", "Crew Access", "Andaz Delhi", FlightLeg.ChangeType.REVISED, "");
        addUtc("UPS15", "DEL", "CGN", "2026-08-09T08:10", "2026-08-09T17:06", null, null, "F/O", "Crew Access", "Cologne Marriott Hotel", FlightLeg.ChangeType.ORIGINAL, "");

        addUtc("UPS008", "CGN", "SZX", "2026-08-11T05:27", "2026-08-11T17:10", "2026-08-10T17:25", "2026-08-11T02:17", "F/O", "Unacknowledged Roster Changes", "JW Marriott Shenzhen", FlightLeg.ChangeType.REVISED, "");
        addUtc("UPS071", "SZX", "ANC", "2026-08-12T21:05", "2026-08-13T07:08", "2026-08-11T19:50", "2026-08-12T02:44", "F/O", "Unacknowledged Roster Changes", null, FlightLeg.ChangeType.REVISED, "");

        // Crew Access trip A70327R is published in Zulu. Store Zulu as authoritative and derive local time by airport.
        addUtc("UPS71", "ANC", "SDF", "2026-08-20T09:13", "2026-08-20T15:20", null, null, "F/O", "Trade confirmation + Crew Access", "Galt House", FlightLeg.ChangeType.TRADED, "A70327R");
        addUtc("UPS213", "SDF", "CGN", "2026-08-21T08:42", "2026-08-21T16:40", null, null, "F/O", "Trade confirmation + Crew Access", "Cologne Marriott Hotel", FlightLeg.ChangeType.TRADED, "A70327R");
        addUtc("UPS14", "CGN", "SZX", "2026-08-25T03:47", "2026-08-25T15:30", null, null, "R/O", "Trade confirmation + Crew Access", "JW Marriott Shenzhen", FlightLeg.ChangeType.TRADED, "A70327R");
        addUtc("UPS77", "SZX", "ANC", "2026-08-26T11:10", "2026-08-26T21:13", null, null, "R/O", "Trade confirmation + Crew Access", null, FlightLeg.ChangeType.TRADED, "A70327R");

        addUtc("UPS197", "ANC", "SDF", "2026-09-04T07:13", "2026-09-04T13:26", null, null, "F/O", "Crew Access", "Galt House", FlightLeg.ChangeType.ORIGINAL, "");
        addUtc("UPS36", "SDF", "HNL", "2026-09-05T11:26", "2026-09-05T20:31", null, null, "F/O", "Crew Access", "Ala Moana Honolulu by Mantra", FlightLeg.ChangeType.ORIGINAL, "");
        addUtc("UPS35", "HNL", "ICN", "2026-09-07T06:45", "2026-09-07T16:20", null, null, "DH", "Crew Access revision", "Sheraton Incheon", FlightLeg.ChangeType.REVISED, "");
        addUtc("UPS99", "ICN", "ANC", "2026-09-08T12:50", "2026-09-08T20:51", null, null, "F/O", "Crew Access revision", null, FlightLeg.ChangeType.REVISED, "");
    }

    void mergeImported(List<FlightLeg> imported) {
        for (FlightLeg leg : imported) {
            for (List<FlightLeg> existing : schedule.values()) {
                existing.removeIf(old -> (!leg.pairing().isBlank() && leg.pairing().equals(old.pairing()) && leg.flightNumber().equals(old.flightNumber()))
                        || (leg.flightNumber().equals(old.flightNumber()) && leg.origin().equals(old.origin()) && leg.destination().equals(old.destination())));
            }
            schedule.computeIfAbsent(leg.departure().toLocalDate(), ignored -> new ArrayList<>()).add(leg);
        }
    }

    private void addUtc(String number, String from, String to, String actualOut, String actualIn,
                        String scheduledOut, String scheduledIn, String assignment, String source,
                        String hotel, FlightLeg.ChangeType changeType, String pairing) {
        ZonedDateTime departure = ZonedDateTime.parse(actualOut + "Z").withZoneSameInstant(airportZone(from));
        ZonedDateTime arrival = ZonedDateTime.parse(actualIn + "Z").withZoneSameInstant(airportZone(to));
        ZonedDateTime scheduledDeparture = scheduledOut == null ? departure : ZonedDateTime.parse(scheduledOut + "Z").withZoneSameInstant(airportZone(from));
        ZonedDateTime scheduledArrival = scheduledIn == null ? arrival : ZonedDateTime.parse(scheduledIn + "Z").withZoneSameInstant(airportZone(to));
        FlightLeg leg = new FlightLeg(number, from, to, departure, arrival,
                scheduledDeparture, scheduledArrival, assignment, source, hotel, changeType, pairing);
        schedule.computeIfAbsent(departure.toLocalDate(), ignored -> new ArrayList<>()).add(leg);
    }

    List<FlightLeg> forDate(LocalDate date) { return Collections.unmodifiableList(schedule.getOrDefault(date, List.of())); }
    LocalDate firstScheduledDate() { return schedule.keySet().stream().min(LocalDate::compareTo).orElse(LocalDate.now()); }
    LocalDate nearestScheduledDate(LocalDate target) {
        if (schedule.containsKey(target)) return target;
        return schedule.keySet().stream().min((a,b) -> Long.compare(Math.abs(ChronoUnit.DAYS.between(target,a)), Math.abs(ChronoUnit.DAYS.between(target,b)))).orElse(target);
    }

    private static ZoneId airportZone(String airport) {
        return switch (airport) {
            case "ANC" -> ZoneId.of("America/Anchorage"); case "SDF" -> ZoneId.of("America/Kentucky/Louisville");
            case "HNL" -> ZoneId.of("Pacific/Honolulu"); case "HKG" -> ZoneId.of("Asia/Hong_Kong");
            case "ICN" -> ZoneId.of("Asia/Seoul"); case "SZX" -> ZoneId.of("Asia/Shanghai");
            case "DEL" -> ZoneId.of("Asia/Kolkata"); case "CGN" -> ZoneId.of("Europe/Berlin");
            default -> UTC;
        };
    }
}
