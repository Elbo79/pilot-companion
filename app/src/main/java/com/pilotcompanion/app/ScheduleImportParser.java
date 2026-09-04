package com.pilotcompanion.app;

import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class ScheduleImportParser {
    private static final Pattern CREW_ACCESS_CARD = Pattern.compile(
            "(?i)(Aug|Sep|Oct|Nov|Dec|Jan|Feb|Mar|Apr|May|Jun|Jul)\\s+(\\d{1,2})\\s+(\\d{1,2}:\\d{2})\\s*-\\s*" +
            "(Aug|Sep|Oct|Nov|Dec|Jan|Feb|Mar|Apr|May|Jun|Jul)\\s+(\\d{1,2})\\s+(\\d{1,2}:\\d{2}).{0,80}?" +
            "UPS\\s*(\\d+).{0,100}?([A-Z]{3})\\s*-\\s*([A-Z]{3}).{0,80}?(F/O|R/O|IRO|FO2|DH)",
            Pattern.DOTALL);

    private static final Pattern TRIP_DATE = Pattern.compile(
            "(?i)Trip\\s*Id\\s*:?\\s*([A-Z]?\\d{4,6}R?)\\s+(\\d{1,2})(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)(20\\d{2})");

    // Strict row parser for ideal OCR output.
    private static final Pattern TRIP_ROW = Pattern.compile(
            "(?i)(\\d{1,2})\\s+(?:Mo|Tu|We|Th|Fr|Sa|Su)\\s+" +
            "(DH\\s*)?(?:UPS\\s*)?(\\d{2,4})\\s+" +
            "([A-Z]{3})\\s*[-–>]\\s*([A-Z]{3})(?:\\s+(IRO|R/O|RO|FO2|F/O))?\\s+" +
            "(\\d{1,2}:\\d{2})\\s+(\\d{1,2}:\\d{2})\\s+" +
            "(\\d{1,2}:\\d{2})\\s+(\\d{1,2}:\\d{2})");

    // Tolerant parser for the actual UPS Trip Information screenshot. ML Kit can separate the Day column
    // from the flight row, so do not require "1 Fr" immediately before the flight number.
    private static final Pattern TRIP_ROW_LOOSE = Pattern.compile(
            "(?i)(DH\\s*)?(?:UPS\\s*)?(\\d{2,4})\\s+" +
            "([A-Z]{3})\\s*[-–>]\\s*([A-Z]{3})(?:\\s+(IRO|R/O|RO|FO2|F/O))?\\s+" +
            "(\\d{1,2}:\\d{2})\\s+(\\d{1,2}:\\d{2})\\s+" +
            "(\\d{1,2}:\\d{2})\\s+(\\d{1,2}:\\d{2})");

    private static final Pattern DUTY_DAY = Pattern.compile("(?i)(\\d{1,2})\\s*(?:Mo|Tu|We|Th|Fr|Sa|Su)\\b");

    static List<FlightLeg> parseCrewAccessText(String text, FlightLeg.ChangeType requestedType, String pairing) {
        List<FlightLeg> result = parseTripInformation(text, requestedType, pairing);
        if (!result.isEmpty()) return result;

        String normalized = normalize(text);
        Matcher m = CREW_ACCESS_CARD.matcher(normalized);
        while (m.find()) {
            int depMonth = month(m.group(1));
            int depDay = Integer.parseInt(m.group(2));
            int arrMonth = month(m.group(4));
            int arrDay = Integer.parseInt(m.group(5));
            String flight = "UPS" + m.group(7);
            String origin = m.group(8).toUpperCase(Locale.US);
            String destination = m.group(9).toUpperCase(Locale.US);
            String assignment = m.group(10).toUpperCase(Locale.US);
            ZonedDateTime departureUtc = ZonedDateTime.parse(String.format(Locale.US, "2026-%02d-%02dT%sZ", depMonth, depDay, twoDigitTime(m.group(3))));
            ZonedDateTime arrivalUtc = ZonedDateTime.parse(String.format(Locale.US, "2026-%02d-%02dT%sZ", arrMonth, arrDay, twoDigitTime(m.group(6))));
            result.add(make(flight, origin, destination, departureUtc, arrivalUtc, assignment, requestedType, pairing, null, "Imported Crew Access card"));
        }
        return result;
    }

    private static List<FlightLeg> parseTripInformation(String text, FlightLeg.ChangeType requestedType, String suppliedPairing) {
        String normalized = normalize(text);
        Matcher header = TRIP_DATE.matcher(normalized);
        if (!header.find()) return List.of();

        String detectedPairing = header.group(1).toUpperCase(Locale.US);
        String pairing = suppliedPairing == null || suppliedPairing.isBlank() ? detectedPairing : suppliedPairing;
        int startDay = Integer.parseInt(header.group(2));
        int startMonth = month(header.group(3));
        int year = Integer.parseInt(header.group(4));
        LocalDate tripStart = LocalDate.of(year, startMonth, startDay);

        List<FlightLeg> strict = parseStrictRows(normalized, requestedType, pairing, tripStart);
        if (!strict.isEmpty()) return strict;
        return parseLooseRows(normalized, requestedType, pairing, tripStart);
    }

    private static List<FlightLeg> parseStrictRows(String normalized, FlightLeg.ChangeType requestedType,
                                                    String pairing, LocalDate tripStart) {
        List<FlightLeg> result = new ArrayList<>();
        Matcher row = TRIP_ROW.matcher(normalized);
        while (row.find()) {
            int dutyDay = Integer.parseInt(row.group(1));
            boolean deadhead = row.group(2) != null;
            String number = row.group(3);
            String origin = row.group(4).toUpperCase(Locale.US);
            String destination = row.group(5).toUpperCase(Locale.US);
            String seat = row.group(6);
            String startZulu = twoDigitTime(row.group(7));
            String endZulu = twoDigitTime(row.group(9));
            result.add(buildTripLeg(dutyDay, deadhead, number, origin, destination, seat, startZulu,
                    endZulu, requestedType, pairing, tripStart));
        }
        return result;
    }

    private static List<FlightLeg> parseLooseRows(String normalized, FlightLeg.ChangeType requestedType,
                                                   String pairing, LocalDate tripStart) {
        List<FlightLeg> result = new ArrayList<>();
        Matcher row = TRIP_ROW_LOOSE.matcher(normalized);
        int previousDutyDay = 1;
        while (row.find()) {
            boolean deadhead = row.group(1) != null;
            String number = row.group(2);
            String origin = row.group(3).toUpperCase(Locale.US);
            String destination = row.group(4).toUpperCase(Locale.US);
            String seat = row.group(5);
            String startZulu = twoDigitTime(row.group(6));
            String endZulu = twoDigitTime(row.group(8));

            int dutyDay = nearestDutyDayBefore(normalized, row.start(), previousDutyDay);
            previousDutyDay = dutyDay;
            result.add(buildTripLeg(dutyDay, deadhead, number, origin, destination, seat, startZulu,
                    endZulu, requestedType, pairing, tripStart));
        }
        return result;
    }

    private static int nearestDutyDayBefore(String text, int rowStart, int fallback) {
        int from = Math.max(0, rowStart - 120);
        String prefix = text.substring(from, rowStart);
        Matcher matcher = DUTY_DAY.matcher(prefix);
        Integer last = null;
        while (matcher.find()) last = Integer.parseInt(matcher.group(1));
        return last == null ? fallback : last;
    }

    private static FlightLeg buildTripLeg(int dutyDay, boolean deadhead, String number, String origin,
                                          String destination, String seat, String startZulu, String endZulu,
                                          FlightLeg.ChangeType requestedType, String pairing, LocalDate tripStart) {
        LocalDate departureDate = tripStart.plusDays(dutyDay - 1L);
        ZonedDateTime departureUtc = ZonedDateTime.parse(departureDate + "T" + startZulu + "Z");
        ZonedDateTime arrivalUtc = ZonedDateTime.parse(departureDate + "T" + endZulu + "Z");
        if (!arrivalUtc.isAfter(departureUtc)) arrivalUtc = arrivalUtc.plusDays(1);

        String assignment;
        if (deadhead) assignment = "DH";
        else if (seat != null && !seat.isBlank()) assignment = seat.toUpperCase(Locale.US);
        else assignment = "F/O";

        String flight = deadhead ? "DH " + number : "UPS" + number;
        return make(flight, origin, destination, departureUtc, arrivalUtc, assignment,
                requestedType, pairing, null, "Imported Crew Access Trip Information");
    }

    static List<FlightLeg> parseSharedFormat(String text) {
        List<FlightLeg> result = new ArrayList<>();
        for (String raw : text.split("\\R")) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] p = line.split("\\|", -1);
            if (p.length < 9) continue;
            try {
                ZonedDateTime departureUtc = ZonedDateTime.parse(p[0].trim());
                ZonedDateTime arrivalUtc = ZonedDateTime.parse(p[1].trim());
                FlightLeg.ChangeType type = FlightLeg.ChangeType.valueOf(p[6].trim().toUpperCase(Locale.US));
                result.add(make(p[2].trim(), p[3].trim(), p[4].trim(), departureUtc, arrivalUtc,
                        p[5].trim(), type, p[7].trim(), p[8].trim().isEmpty() ? null : p[8].trim(), "Shared schedule sync"));
            } catch (RuntimeException ignored) { }
        }
        return result;
    }

    private static FlightLeg make(String flight, String origin, String destination, ZonedDateTime departureUtc,
                                  ZonedDateTime arrivalUtc, String assignment, FlightLeg.ChangeType type,
                                  String pairing, String hotel, String source) {
        ZonedDateTime departure = departureUtc.withZoneSameInstant(zone(origin));
        ZonedDateTime arrival = arrivalUtc.withZoneSameInstant(zone(destination));
        return new FlightLeg(flight, origin, destination, departure, arrival, departure, arrival,
                assignment, source, hotel, type, pairing == null ? "" : pairing);
    }

    private static String normalize(String text) {
        return text.replace('\n', ' ').replace('\r', ' ').replaceAll("\\s+", " ").trim();
    }

    private static String twoDigitTime(String time) {
        String[] parts = time.split(":");
        return String.format(Locale.US, "%02d:%02d", Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
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
