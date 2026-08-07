package com.pilotcompanion.app;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public record FlightLeg(String flightNumber, String origin, String destination,
        ZonedDateTime departure, ZonedDateTime arrival,
        ZonedDateTime scheduledDeparture, ZonedDateTime scheduledArrival,
        String assignment, String source, String hotel) {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.US);

    public String seatPosition() {
        if (assignment == null) return "FO";
        String normalized = assignment.trim().toUpperCase(Locale.US).replace("/", "");
        if (normalized.contains("DEADHEAD") || normalized.equals("DH")) return "DH";
        if (normalized.equals("IRO") || normalized.equals("RO")) return "RO";
        if (normalized.equals("FO2")) return "FO2";
        return "FO";
    }

    public String localTimes() {
        return departure.format(TIME) + "-" + arrival.format(TIME);
    }

    public boolean isRevised() {
        return !departure.toInstant().equals(scheduledDeparture.toInstant())
                || !arrival.toInstant().equals(scheduledArrival.toInstant());
    }

    public String scheduledLocalTimes() {
        return scheduledDeparture.format(TIME) + "-" + scheduledArrival.format(TIME);
    }

    public String flightTime() {
        Duration duration = Duration.between(departure.toInstant(), arrival.toInstant());
        return String.format(Locale.US, "%dh %02dm", duration.toHours(), duration.toMinutesPart());
    }
}
