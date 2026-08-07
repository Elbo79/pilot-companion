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
    private final Map<LocalDate, List<FlightLeg>> schedule = new HashMap<>();

    ScheduleRepository() {
        LocalDate today = LocalDate.now();
        add(today.withDayOfMonth(Math.min(4, today.lengthOfMonth())), "5X120", "SDF", "ANC", 6, 30, 10, 12, "FO");
        add(today.withDayOfMonth(Math.min(6, today.lengthOfMonth())), "5X902", "ANC", "ICN", 13, 5, 18, 40, "IRO");
        add(today.withDayOfMonth(Math.min(12, today.lengthOfMonth())), "5X11", "ICN", "SZX", 9, 20, 12, 45, "FO2");
        add(today.withDayOfMonth(Math.min(18, today.lengthOfMonth())), "5X88", "SZX", "SDF", 22, 10, 4, 55, "Deadhead");
    }

    private void add(LocalDate date, String number, String from, String to,
                     int departureHour, int departureMinute, int arrivalHour, int arrivalMinute,
                     String assignment) {
        ZoneId fromZone = airportZone(from);
        ZoneId toZone = airportZone(to);
        ZonedDateTime departure = date.atTime(departureHour, departureMinute).atZone(fromZone);
        LocalDate arrivalDate = arrivalHour < departureHour ? date.plusDays(1) : date;
        ZonedDateTime arrival = arrivalDate.atTime(arrivalHour, arrivalMinute).atZone(toZone);
        if (!arrival.toInstant().isAfter(departure.toInstant())) arrival = arrival.plusDays(1);
        schedule.computeIfAbsent(date, ignored -> new ArrayList<>())
                .add(new FlightLeg(number, from, to, departure, arrival, assignment));
    }

    List<FlightLeg> forDate(LocalDate date) {
        return Collections.unmodifiableList(schedule.getOrDefault(date, List.of()));
    }

    private static ZoneId airportZone(String airport) {
        return switch (airport) {
            case "SDF" -> ZoneId.of("America/Kentucky/Louisville");
            case "ANC" -> ZoneId.of("America/Anchorage");
            case "ICN" -> ZoneId.of("Asia/Seoul");
            case "SZX" -> ZoneId.of("Asia/Shanghai");
            default -> ZoneId.systemDefault();
        };
    }
}
