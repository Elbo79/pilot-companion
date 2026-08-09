package com.pilotcompanion.app;

import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class ScheduleImportParser {
    private static final Pattern CREW_ACCESS = Pattern.compile(
            "(?i)(Aug|Sep|Oct|Nov|Dec|Jan|Feb|Mar|Apr|May|Jun|Jul)\\s+(\\d{1,2})\\s+(\\d{1,2}:\\d{2})\\s*-\\s*" +
            "(Aug|Sep|Oct|Nov|Dec|Jan|Feb|Mar|Apr|May|Jun|Jul)\\s+(\\d{1,2})\\s+(\\d{1,2}:\\d{2}).{0,80}?" +
            "UPS\\s*(\\d+).{0,100}?([A-Z]{3})\\s*-\\s*([A-Z]{3}).{0,80}?(F/O|R/O|IRO|FO2|DH)",
            Pattern.DOTALL);

    static List<FlightLeg> parseCrewAccessText(String text, FlightLeg.ChangeType requestedType, String pairing) {
        String normalized = text.replace('\n', ' ').replaceAll("\\s+", " ");
        List<FlightLeg> result = new ArrayList<>();
        Matcher m = CREW_ACCESS.matcher(normalized);
        while (m.find()) {
            int depMonth = month(m.group(1));
            int depDay = Integer.parseInt(m.group(2));
            int arrMonth = month(m.group(4));
            int arrDay = Integer.parseInt(m.group(5));
            String flight = "UPS" + m.group(7);
            String origin = m.group(8).toUpperCase(Locale.US);
            String destination = m.group(9).toUpperCase(Locale.US);
            String assignment = m.group(10).toUpperCase(Locale.US);
            ZonedDateTime departureUtc = ZonedDateTime.parse(String.format(Locale.US, "2026-%02d-%02dT%sZ", depMonth, depDay, m.group(3)));
            ZonedDateTime arrivalUtc = ZonedDateTime.parse(String.format(Locale.US, "2026-%02d-%02dT%sZ", arrMonth, arrDay, m.group(6)));
            ZonedDateTime departure = departureUtc.withZoneSameInstant(zone(origin));
            ZonedDateTime arrival = arrivalUtc.withZoneSameInstant(zone(destination));
            FlightLeg.ChangeType type = requestedType;
            result.add(new FlightLeg(flight, origin, destination, departure, arrival, departure, arrival,
                    assignment, "Imported in app", null, type, pairing == null ? "" : pairing));
        }
        return result;
    }

    private static int month(String shortName) {
        return Month.valueOf(switch (shortName.substring(0, 3).toLowerCase(Locale.US)) {
            case "jan" -> "JANUARY"; case "feb" -> "FEBRUARY"; case "mar" -> "MARCH";
            case "apr" -> "APRIL"; case "may" -> "MAY"; case "jun" -> "JUNE";
            case "jul" -> "JULY"; case "aug" -> "AUGUST"; case "sep" -> "SEPTEMBER";
            case "oct" -> "OCTOBER"; case "nov" -> "NOVEMBER"; default -> "DECEMBER";
        }).getValue();
    }

    private static ZoneId zone(String airport) {
        return switch (airport) {
            case "ANC" -> ZoneId.of("America/Anchorage"); case "SDF" -> ZoneId.of("America/Kentucky/Louisville");
            case "HNL" -> ZoneId.of("Pacific/Honolulu"); case "HKG" -> ZoneId.of("Asia/Hong_Kong");
            case "ICN" -> ZoneId.of("Asia/Seoul"); case "SZX" -> ZoneId.of("Asia/Shanghai");
            case "DEL" -> ZoneId.of("Asia/Kolkata"); case "CGN" -> ZoneId.of("Europe/Berlin");
            default -> ZoneId.of("UTC");
        };
    }
}
