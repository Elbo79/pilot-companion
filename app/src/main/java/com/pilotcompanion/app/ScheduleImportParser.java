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

    // ML Kit sometimes reads "Id" as "ld" or "1d", so accept those OCR variants.
    private static final Pattern TRIP_DATE = Pattern.compile(
            "(?i)Trip\\s*[Iil1][dD]\\s*:?\\s*([A-Z]?\\d{4,6}R?)\\s+(\\d{1,2})\\s*(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\s*(20\\d{2})");

    private static final Pattern ROUTE_ROW = Pattern.compile(
            "(?i)(DH\\s*)?(?:UPS\\s*)?(\\d{2,4}(?:-\\d)?)\\s+([A-Z]{3})\\s*[-–>]\\s*([A-Z]{3})(?:\\s+(IRO|R/O|RO|FO2|F/O))?");
    private static final Pattern ROUTE_ONLY = Pattern.compile("(?i)\\b([A-Z]{3})\\s*[-–>]\\s*([A-Z]{3})\\b");
    private static final Pattern DUTY_DAY = Pattern.compile("(?i)(\\d{1,2})\\s*(Mo|Tu|We|Th|Fr|Sa|Su)");
    private static final Pattern FLIGHT_BEFORE = Pattern.compile("(?i)(DH\\s*)?(?:UPS\\s*)?(\\d{2,4}(?:-\\d)?)\\b");
    private static final Pattern CLOCK = Pattern.compile("\\b(\\d{1,2}:\\d{2})\\b");

    static List<FlightLeg> parseCrewAccessText(String text, FlightLeg.ChangeType requestedType, String pairing) {
        List<FlightLeg> result = parseTripInformation(text, requestedType, pairing);
        if (!result.isEmpty()) return result;

        // Roster Details OCR is processed card by card so hotel cards cannot
        // accidentally be consumed as flight data.
        String normalized = normalize(text);
        Pattern roster = Pattern.compile(
            "(?i)([A-Z]{3}\\s+\\d{1,2}\\s+\\d{1,2}:\\d{2})\\s*-\\s*([A-Z]{3}\\s+\\d{1,2}\\s+\\d{1,2}:\\d{2})" +
            "\\s+UPS\\s*(\\d{1,4}(?:-\\d)?)\\s+(?:Pos\\s+AC\\s+Block\\s+)?" +
            "([A-Z]{3})\\s*-\\s*([A-Z]{3})\\s+(R/O|F/O|IRO|FO2|DH)\\b");
        Matcher m = roster.matcher(normalized);
        while (m.find()) {
            try {
                String[] dep = m.group(1).split("\\s+");
                String[] arr = m.group(2).split("\\s+");
                String origin = m.group(4).toUpperCase(Locale.US);
                String destination = m.group(5).toUpperCase(Locale.US);
                ZonedDateTime departureUtc = ZonedDateTime.parse(String.format(Locale.US,
                    "2026-%02d-%02dT%sZ", month(dep[0]), Integer.parseInt(dep[1]), twoDigitTime(dep[2])));
                ZonedDateTime arrivalUtc = ZonedDateTime.parse(String.format(Locale.US,
                    "2026-%02d-%02dT%sZ", month(arr[0]), Integer.parseInt(arr[1]), twoDigitTime(arr[2])));
                if (!arrivalUtc.isAfter(departureUtc)) continue;
                long minutes = java.time.Duration.between(departureUtc,arrivalUtc).toMinutes();
                if (minutes < 20 || minutes > 1200) continue;
                result.add(make("UPS"+m.group(3),origin,destination,departureUtc,arrivalUtc,
                    m.group(6).toUpperCase(Locale.US),requestedType,pairing,null,"Imported UPS Roster Details"));
            } catch (RuntimeException ignored) { }
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

        List<FlightLeg> result = parseRowAnchors(normalized, requestedType, pairing, tripStart);
        if (!result.isEmpty()) return result;
        return parseRouteAnchors(normalized, requestedType, pairing, tripStart);
    }

    private static List<FlightLeg> parseRowAnchors(String normalized, FlightLeg.ChangeType requestedType,
                                                    String pairing, LocalDate tripStart) {
        List<FlightLeg> result = new ArrayList<>();
        Matcher row = ROUTE_ROW.matcher(normalized);
        while (row.find()) {
            int dutyDay = dutyDayBefore(normalized, row.start());
            if (dutyDay < 1) continue;
            String number = row.group(2);
            String origin = row.group(3).toUpperCase(Locale.US);
            String destination = row.group(4).toUpperCase(Locale.US);
            String seat = row.group(5);
            boolean deadhead = row.group(1) != null;
            FlightLeg leg = legFromAnchor(normalized, row.end(), dutyDay, number, origin, destination, seat,
                    deadhead, requestedType, pairing, tripStart);
            if (leg != null) result.add(leg);
        }
        return result;
    }

    private static List<FlightLeg> parseRouteAnchors(String normalized, FlightLeg.ChangeType requestedType,
                                                      String pairing, LocalDate tripStart) {
        List<FlightLeg> result = new ArrayList<>();
        Matcher route = ROUTE_ONLY.matcher(normalized);
        while (route.find()) {
            int dutyDay = dutyDayBefore(normalized, route.start());
            if (dutyDay < 1) continue;
            String origin = route.group(1).toUpperCase(Locale.US);
            String destination = route.group(2).toUpperCase(Locale.US);
            FlightToken token = flightBefore(normalized, route.start());
            if (token == null) continue;
            FlightLeg leg = legFromAnchor(normalized, route.end(), dutyDay, token.number(), origin, destination,
                    null, token.deadhead(), requestedType, pairing, tripStart);
            if (leg != null && result.stream().noneMatch(x -> sameImportedLeg(x, leg))) result.add(leg);
        }
        return result;
    }

    private static FlightLeg legFromAnchor(String normalized, int anchorEnd, int dutyDay, String number,
                                           String origin, String destination, String seat, boolean deadhead,
                                           FlightLeg.ChangeType requestedType, String pairing, LocalDate tripStart) {
        List<String> clocks = clocksAfter(normalized, anchorEnd, 260);
        if (clocks.size() < 4) return null;
        String startZulu = twoDigitTime(clocks.get(0));
        String endZulu = twoDigitTime(clocks.get(2));
        LocalDate departureDate = tripStart.plusDays(dutyDay - 1L);
        ZonedDateTime departureUtc = ZonedDateTime.parse(departureDate + "T" + startZulu + "Z");
        ZonedDateTime arrivalUtc = ZonedDateTime.parse(departureDate + "T" + endZulu + "Z");
        if (!arrivalUtc.isAfter(departureUtc)) arrivalUtc = arrivalUtc.plusDays(1);
        long elapsed = java.time.Duration.between(departureUtc, arrivalUtc).toMinutes();
        if (elapsed < 30 || elapsed > 1200) return null;
        String assignment = deadhead ? "DH" : (seat == null || seat.isBlank() ? "F/O" : seat.toUpperCase(Locale.US));
        String flight = deadhead ? "DH " + number : "UPS" + number;
        return make(flight, origin, destination, departureUtc, arrivalUtc, assignment,
                requestedType, pairing, null, "Imported Crew Access Trip Information");
    }

    private static boolean sameImportedLeg(FlightLeg a, FlightLeg b) {
        return a.flightNumber().equals(b.flightNumber()) && a.origin().equals(b.origin())
                && a.destination().equals(b.destination()) && a.departure().toInstant().equals(b.departure().toInstant());
    }

    private static int dutyDayBefore(String text, int routeStart) {
        int from = Math.max(0, routeStart - 220);
        Matcher m = DUTY_DAY.matcher(text.substring(from, routeStart));
        int last = -1;
        while (m.find()) last = Integer.parseInt(m.group(1));
        return last;
    }

    private static FlightToken flightBefore(String text, int routeStart) {
        int from = Math.max(0, routeStart - 120);
        Matcher m = FLIGHT_BEFORE.matcher(text.substring(from, routeStart));
        FlightToken last = null;
        while (m.find()) {
            String number = m.group(2);
            if (number.length() == 4 && number.startsWith("20")) continue;
            last = new FlightToken(number, m.group(1) != null);
        }
        return last;
    }

    private static List<String> clocksAfter(String text, int start, int maxChars) {
        int end = Math.min(text.length(), start + maxChars);
        Matcher m = CLOCK.matcher(text.substring(start, end));
        List<String> out = new ArrayList<>();
        while (m.find() && out.size() < 4) out.add(m.group(1));
        return out;
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
            case "ICN" -> ZoneId.of("Asia/Seoul"); case "SZX", "PVG" -> ZoneId.of("Asia/Shanghai");
            case "DEL" -> ZoneId.of("Asia/Kolkata"); case "CGN" -> ZoneId.of("Europe/Berlin");
            case "CRK" -> ZoneId.of("Asia/Manila"); case "TPE" -> ZoneId.of("Asia/Taipei");
            default -> ZoneId.of("UTC");
        };
    }

    private record FlightToken(String number, boolean deadhead) { }
}
