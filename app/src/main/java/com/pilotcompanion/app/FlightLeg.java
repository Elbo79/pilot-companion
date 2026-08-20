package com.pilotcompanion.app;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public record FlightLeg(String flightNumber, String origin, String destination,
        ZonedDateTime departure, ZonedDateTime arrival,
        ZonedDateTime scheduledDeparture, ZonedDateTime scheduledArrival,
        String assignment, String source, String hotel, ChangeType changeType, String pairing) {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.US);
    private static final DateTimeFormatter ZULU = DateTimeFormatter.ofPattern("MMM d HH:mm'Z'", Locale.US);
    private static final DateTimeFormatter CALIFORNIA = DateTimeFormatter.ofPattern("MMM d HH:mm z", Locale.US);
    private static final ZoneId PACIFIC = ZoneId.of("America/Los_Angeles");

    public enum ChangeType { ORIGINAL, TRADED, REVISED }

    public String seatPosition() {
        if (assignment == null) return "FO";
        String normalized = assignment.trim().toUpperCase(Locale.US).replace("/", "");
        if (normalized.contains("DEADHEAD") || normalized.equals("DH")) return "DH";
        if (normalized.equals("IRO") || normalized.equals("RO")) return "RO";
        if (normalized.equals("FO2")) return "FO2";
        return "FO";
    }

    public String localTimes() { return departure.format(TIME) + "-" + arrival.format(TIME); }
    public String departureZulu() { return departure.withZoneSameInstant(ZoneOffset.UTC).format(ZULU); }
    public String arrivalZulu() { return arrival.withZoneSameInstant(ZoneOffset.UTC).format(ZULU); }
    public String zuluTimes() { return departureZulu() + " - " + arrivalZulu(); }
    public String departureCalifornia() { return departure.withZoneSameInstant(PACIFIC).format(CALIFORNIA); }
    public String arrivalCalifornia() { return arrival.withZoneSameInstant(PACIFIC).format(CALIFORNIA); }
    public String californiaTimes() { return departureCalifornia() + " - " + arrivalCalifornia(); }
    public boolean isRevised() { return changeType == ChangeType.REVISED; }
    public boolean isTraded() { return changeType == ChangeType.TRADED; }
    public String scheduledLocalTimes() { return scheduledDeparture.format(TIME) + "-" + scheduledArrival.format(TIME); }

    public String changeLabel() {
        return switch (changeType) {
            case TRADED -> "TRADED";
            case REVISED -> "REVISED";
            default -> "SCHEDULED";
        };
    }

    public String flightTime() {
        Duration duration = Duration.between(departure.toInstant(), arrival.toInstant());
        return String.format(Locale.US, "%dh %02dm", duration.toHours(), duration.toMinutesPart());
    }
}
