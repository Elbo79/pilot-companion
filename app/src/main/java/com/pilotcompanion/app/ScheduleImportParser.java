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
            "(?i)(DH\\s*)?(?:UPS\\s*)?(\\d{2,4})\\s+([A-Z]{3})\\s*[-–>]\\s*([A-Z]{3})(?:\\s+(IRO|R/O|RO|FO2|F/O))?");
    private static final Pattern ROUTE_ONLY = Pattern.compile("(?i)\\b([A-Z]{3})\\s*[-–>]\\s*([A-Z]{3})\\b");
    private static final Pattern DUTY_DAY = Pattern.compile("(?i)(\\d{1,2})\\s*(Mo|Tu|We|Th|Fr|Sa|Su)");
    private static final Pattern FLIGHT_BEFORE = Pattern.compile("(?i)(DH\\s*)?(?:UPS\\s*)?(\\d{2,4})\\b");
    private static final Pattern CLOCK = Pattern.compile("\\b(\\d{1,2}:\\d{2})\\b");

    // Each expanded Crew Access roster flight has its own date/time range.
    // Never attach a subsequent flight number to an earlier range.
    private static final Pattern ROSTER_RANGE = Pattern.compile(
            "(?i)\\b(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\s+(\\d{1,2})\\s+(\\d{1,2}:\\d{2})\\s*[-–]\\s*" +
            "(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\s+(\\d{1,2})\\s+(\\d{1,2}:\\d{2})\\b");
    private static final Pattern ROSTER_FLIGHT = Pattern.compile("(?i)\\bUPS\\s*(\\d{1,4})\\b");
    private static final Pattern ROSTER_ROUTE = Pattern.compile("(?i)\\b([A-Z]{3})\\s*[-–>]\\s*([A-Z]{3})\\b");

    private static List<FlightLeg> parseExpandedRoster(String text, FlightLeg.ChangeType type, String pairing) {
        String normalized = normalize(text);
        Matcher ranges = ROSTER_RANGE.matcher(normalized);
        List<FlightLeg> legs = new ArrayList<>();
        List<int[]> spans = new ArrayList<>();
        List<String[]> dates = new ArrayList<>();
        while (ranges.find()) {
            spans.add(new int[]{ranges.start(), ranges.end()});
            dates.add(new String[]{ranges.group(1), ranges.group(2), ranges.group(3),
                    ranges.group(4), ranges.group(5), ranges.group(6)});
        }
        for (int i = 0; i < spans.size(); i++) {
            int begin = spans.get(i)[1];
            int end = i + 1 < spans.size() ? spans.get(i + 1)[0] : normalized.length();
            String segment = normalized.substring(begin, end);
            Matcher flight = ROSTER_FLIGHT.matcher(segment);
            Matcher route = ROSTER_ROUTE.matcher(segment);
            if (!flight.find() || !route.find()) continue;
            String[] date = dates.get(i);
            try {
                LocalDate departureDate = LocalDate.of(2026, month(date[0]), Integer.parseInt(date[1]));
                LocalDate arrivalDate = LocalDate.of(2026, month(date[3]), Integer.parseInt(date[4]));
                ZonedDateTime departure = ZonedDateTime.parse(departureDate + "T" + twoDigitTime(date[2]) + "Z");
                ZonedDateTime arrival = ZonedDateTime.parse(arrivalDate + "T" + twoDigitTime(date[5]) + "Z");
                if (!arrival.isAfter(departure)) continue;
                String origin = route.group(1).toUpperCase(Locale.US);
                String destination = route.group(2).toUpperCase(Locale.US);
                boolean deadhead = Pattern.compile("(?i)\\bDH\\b").matcher(segment).find();
                String seat = deadhead ? "DH" :
                        Pattern.compile("(?i)\\bR/O\\b").matcher(segment).find() ? "R/O" : "F/O";
                legs.add(make("UPS" + flight.group(1), origin, destination, departure, arrival,
                        seat, type, pairing, null, "Imported Crew Access expanded roster"));
            } catch (RuntimeException ignored) { }
        }
        return legs;
    }

    static List<FlightLeg> parseCrewAccessText(String text, FlightLeg.ChangeType requestedType, String pairing) {
        List<FlightLeg> expanded = parseExpandedRoster(text, requestedType, pairing);
        if (!expanded.isEmpty()) return expanded;
        // parseTripInformation returns List.of() when the image is not a Trip Information page.
        // Always use a mutable list before appending fallback Crew Access card matches.
        List<FlightLeg> result = new ArrayList<>(parseTripInformation(text, requestedType, pairing));
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
