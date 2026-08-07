package com.pilotcompanion.app;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public record FlightLeg(
        String flightNumber,
        String origin,
        String destination,
        ZonedDateTime departure,
        ZonedDateTime arrival,
        String assignment
) {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.US);

    public String seatPosition() {
        if (assignment == null) return "FO";
        String normalized = assignment.trim().toUpperCase(Locale.US);
        if (normalized.contains("DEADHEAD") || normalized.equals("DH")) return "DH";
        if (normalized.equals("IRO")) return "RO";
        if (normalized.equals("FO2")) return "FO2";
        return "FO";
    }

    public String localTimes() {
        return departure.format(TIME) + "–" + arrival.format(TIME);
    }

    public String flightTime() {
        Duration duration = Duration.between(departure.toInstant(), arrival.toInstant());
        return String.format(Locale.US, "%dh %02dm", duration.toHours(), duration.toMinutesPart());
    }
}
