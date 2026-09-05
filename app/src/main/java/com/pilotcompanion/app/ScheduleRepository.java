package com.pilotcompanion.app;

import java.time.Duration;
import java.time.Instant;
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
        // Earlier retained schedule
        addUtc("UPS52", "ANC", "HKG", "2026-08-03T18:47", "2026-08-04T04:44", "2026-08-03T18:08", "2026-08-04T05:10", "F/O", "Crew Access revision", "New World Millennium Hong Kong", FlightLeg.ChangeType.REVISED, "");
        addUtc("UPS67", "HKG", "ICN", "2026-08-06T02:37", "2026-08-06T06:18", "2026-08-06T02:15", "2026-08-06T05:45", "F/O", "Crew Access revision", "Sheraton Incheon", FlightLeg.ChangeType.REVISED, "");
        addUtc("UPS6011", "ICN", "DEL", "2026-08-08T01:19", "2026-08-08T08:46", null, null, "F/O", "Crew Access", "Andaz Delhi", FlightLeg.ChangeType.REVISED, "");
        addUtc("UPS15", "DEL", "CGN", "2026-08-09T08:10", "2026-08-09T17:06", null, null, "F/O", "Crew Access", "Cologne Marriott Hotel", FlightLeg.ChangeType.ORIGINAL, "");
        addUtc("UPS008", "CGN", "SZX", "2026-08-11T05:27", "2026-08-11T17:10", "2026-08-10T17:25", "2026-08-11T02:17", "F/O", "Unacknowledged Roster Changes", "JW Marriott Shenzhen", FlightLeg.ChangeType.REVISED, "");
        addUtc("UPS071", "SZX", "ANC", "2026-08-12T21:05", "2026-08-13T07:08", "2026-08-11T19:50", "2026-08-12T02:44", "F/O", "Unacknowledged Roster Changes", null, FlightLeg.ChangeType.REVISED, "");
        addUtc("UPS71", "ANC", "SDF", "2026-08-20T09:13", "2026-08-20T15:20", null, null, "F/O", "Trade confirmation + Crew Access", "Galt House", FlightLeg.ChangeType.TRADED, "A70327R");
        addUtc("UPS213", "SDF", "CGN", "2026-08-21T08:42", "2026-08-21T16:40", null, null, "F/O", "Trade confirmation + Crew Access", "Cologne Marriott Hotel", FlightLeg.ChangeType.TRADED, "A70327R");
        addUtc("UPS14", "CGN", "SZX", "2026-08-25T03:47", "2026-08-25T15:30", null, null, "R/O", "Trade confirmation + Crew Access", "JW Marriott Shenzhen", FlightLeg.ChangeType.TRADED, "A70327R");
        addUtc("UPS77", "SZX", "ANC", "2026-08-26T11:10", "2026-08-26T21:13", null, null, "R/O", "Trade confirmation + Crew Access", null, FlightLeg.ChangeType.TRADED, "A70327R");

        // Current Crew Access schedule from 04 Sep 2026 screenshots. Zulu is authoritative.
        addUtc("DH 071", "ANC", "SDF", "2026-09-04T09:13", "2026-09-04T15:20", null, null, "DH", "Crew Access Trip Information screenshot", "Galt House", FlightLeg.ChangeType.ORIGINAL, "A70659");
        addUtc("UPS036", "SDF", "HNL", "2026-09-05T11:26", "2026-09-05T20:31", null, null, "F/O", "Crew Access Trip Information screenshot", "Ala Moana Honolulu by Mantra", FlightLeg.ChangeType.ORIGINAL, "A70659");
        addUtc("DH 035", "HNL", "ICN", "2026-09-07T06:45", "2026-09-07T16:20", null, null, "DH", "Crew Access Trip Information screenshot", "Sheraton Incheon", FlightLeg.ChangeType.ORIGINAL, "A70659");
        addUtc("UPS099", "ICN", "ANC", "2026-09-08T12:50", "2026-09-08T20:51", null, null, "F/O", "Crew Access Trip Information screenshot", null, FlightLeg.ChangeType.ORIGINAL, "A70659");

        addUtc("UPS099", "ANC", "SDF", "2026-09-09T22:46", "2026-09-10T04:53", null, null, "F/O", "Crew Access Trip Information screenshot", "Galt House", FlightLeg.ChangeType.ORIGINAL, "16168");
        addUtc("DH 094", "SDF", "ANC", "2026-09-10T19:50", "2026-09-11T02:31", null, null, "DH", "Crew Access Trip Information screenshot", null, FlightLeg.ChangeType.ORIGINAL, "16168");

        addUtc("UPS093", "ANC", "SDF", "2026-09-20T08:53", "2026-09-20T15:00", null, null, "F/O", "Crew Access Trip Information screenshot", "Galt House", FlightLeg.ChangeType.ORIGINAL, "A70040R");
        addUtc("UPS094", "SDF", "ANC", "2026-09-21T19:50", "2026-09-22T02:31", null, null, "F/O", "Crew Access Trip Information screenshot", null, FlightLeg.ChangeType.ORIGINAL, "A70040R");

        addUtc("UPS099", "ANC", "SDF", "2026-09-28T22:46", "2026-09-29T04:53", null, null, "F/O", "Crew Access Trip Information screenshot", "Galt House", FlightLeg.ChangeType.ORIGINAL, "A70315");
        addUtc("UPS223", "SDF", "CGN", "2026-09-30T06:57", "2026-09-30T14:55", null, null, "F/O", "Crew Access Trip Information screenshot", "Cologne Marriott Hotel", FlightLeg.ChangeType.ORIGINAL, "A70315");
        addUtc("UPS010", "CGN", "PVG", "2026-10-01T08:16", "2026-10-01T19:50", null, null, "F/O", "Crew Access Trip Information screenshot", "Conrad Shanghai", FlightLeg.ChangeType.ORIGINAL, "A70315");
        addUtc("UPS081", "PVG", "ANC", "2026-10-03T19:20", "2026-10-04T04:04", null, null, "F/O", "Crew Access Trip Information screenshot", null, FlightLeg.ChangeType.ORIGINAL, "A70315");

        addUtc("UPS064", "ANC", "ICN", "2026-10-15T17:37", "2026-10-16T02:25", null, null, "F/O", "Crew Access Trip Information screenshot", "Sheraton Incheon", FlightLeg.ChangeType.ORIGINAL, "A70569");
        addUtc("UPS099", "ICN", "ANC", "2026-10-17T22:05", "2026-10-18T06:06", null, null, "F/O", "Crew Access Trip Information screenshot", null, FlightLeg.ChangeType.ORIGINAL, "A70569");

        addUtc("UPS096", "ANC", "CRK", "2026-10-26T19:10", "2026-10-27T06:20", null, null, "F/O", "Crew Access Trip Information screenshot", "Clark Marriott Hotel", FlightLeg.ChangeType.ORIGINAL, "A70233R");
        addUtc("UPS095", "CRK", "TPE", "2026-10-28T08:20", "2026-10-28T10:06", null, null, "F/O", "Crew Access Trip Information screenshot", "Sheraton Taipei", FlightLeg.ChangeType.ORIGINAL, "A70233R");
        addUtc("UPS095", "TPE", "ANC", "2026-10-29T12:30", "2026-10-29T21:10", null, null, "F/O", "Crew Access Trip Information screenshot", null, FlightLeg.ChangeType.ORIGINAL, "A70233R");
    }

    void mergeImported(List<FlightLeg> imported) {
        for (FlightLeg leg : imported) {
            for (List<FlightLeg> existing : schedule.values()) {
                existing.removeIf(old -> sameLegIdentity(old, leg));
            }
            schedule.computeIfAbsent(leg.departure().toLocalDate(), ignored -> new ArrayList<>()).add(leg);
        }
    }

    private static boolean sameLegIdentity(FlightLeg old, FlightLeg incoming) {
        // Pairing + flight number is the strongest identity. Repeated UPS flight numbers on later pairings
        // must never delete an earlier trip (e.g. UPS099 appears several times in Sep/Oct 2026).
        if (!old.pairing().isBlank() && !incoming.pairing().isBlank()) {
            return old.pairing().equals(incoming.pairing())
                    && old.flightNumber().equals(incoming.flightNumber())
                    && old.origin().equals(incoming.origin())
                    && old.destination().equals(incoming.destination());
        }

        // Legacy rows without pairing IDs may be replaced only when they are clearly the same occurrence:
        // same flight/route and departure instants within 12 hours.
        if (!old.flightNumber().equals(incoming.flightNumber())
                || !old.origin().equals(incoming.origin())
                || !old.destination().equals(incoming.destination())) return false;
        long hours = Math.abs(Duration.between(old.departure().toInstant(), incoming.departure().toInstant()).toHours());
        return hours <= 12;
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

    FlightLeg nextAssignmentAfter(Instant instant) {
        return schedule.values().stream()
                .flatMap(List::stream)
                .filter(leg -> leg.departure().toInstant().isAfter(instant))
                .min((a, b) -> a.departure().toInstant().compareTo(b.departure().toInstant()))
                .orElse(null);
    }

    LocalDate firstScheduledDate() { return schedule.keySet().stream().min(LocalDate::compareTo).orElse(LocalDate.now()); }
    LocalDate nearestScheduledDate(LocalDate target) {
        if (schedule.containsKey(target)) return target;
        return schedule.keySet().stream().min((a,b) -> Long.compare(Math.abs(ChronoUnit.DAYS.between(target,a)), Math.abs(ChronoUnit.DAYS.between(target,b)))).orElse(target);
    }

    private static ZoneId airportZone(String airport) {
        return switch (airport) {
            case "ANC" -> ZoneId.of("America/Anchorage"); case "SDF" -> ZoneId.of("America/Kentucky/Louisville");
            case "HNL" -> ZoneId.of("Pacific/Honolulu"); case "HKG" -> ZoneId.of("Asia/Hong_Kong");
            case "ICN" -> ZoneId.of("Asia/Seoul"); case "SZX", "PVG" -> ZoneId.of("Asia/Shanghai");
            case "DEL" -> ZoneId.of("Asia/Kolkata"); case "CGN" -> ZoneId.of("Europe/Berlin");
            case "CRK" -> ZoneId.of("Asia/Manila"); case "TPE" -> ZoneId.of("Asia/Taipei");
            default -> UTC;
        };
    }
}
