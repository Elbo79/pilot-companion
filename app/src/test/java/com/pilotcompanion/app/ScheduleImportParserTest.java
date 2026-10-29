package com.pilotcompanion.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

public class ScheduleImportParserTest {
    @Test public void parsesNumericTripInformationPageUsingZuluColumns() {
        String text = "Trip Information Date: 08Sep2026 Trip Id: 16168 09Sep2026 " +
                "Day Flight Departure-Arrival Start Start(LT) End End(LT) Block A/C " +
                "1 We 099 ANC-SDF 22:46 14:46 04:53 00:53 06:07 747 " +
                "Duty end 05:23 01:23 Rest: 12:57 " +
                "2 Th DH 094 SDF-ANC 19:50 15:50 02:31 18:31 747";

        List<FlightLeg> legs = ScheduleImportParser.parseCrewAccessText(
                text, FlightLeg.ChangeType.ORIGINAL, "");

        assertEquals(2, legs.size());
        FlightLeg first = legs.get(0);
        assertEquals("UPS099", first.flightNumber());
        assertEquals("16168", first.pairing());
        assertEquals("14:46-00:53", first.localTimes());
        assertEquals("Sep 9 22:46Z", first.departureZulu());
        assertEquals("Sep 10 04:53Z", first.arrivalZulu());
        assertEquals("6h 07m", first.flightTime());

        FlightLeg second = legs.get(1);
        assertEquals("DH 094", second.flightNumber());
        assertEquals("DH", second.seatPosition());
        assertEquals("15:50-18:31", second.localTimes());
        assertEquals("Sep 10 19:50Z", second.departureZulu());
        assertEquals("Sep 11 02:31Z", second.arrivalZulu());
    }

    @Test public void parsesCurrentA70659IncludingDeadheads() {
        String text = "Trip Id: A70659 04Sep2026 Day Flight Departure-Arrival Start Start(LT) End End(LT Block A/C " +
                "1 Fr Duty start 05:43 21:43 DH 071 ANC-SDF 09:13 01:13 15:20 11:20 - 747 Duty end 15:50 11:50 " +
                "2 Sa Duty start 09:56 05:56 036 SDF-HNL 11:26 07:26 20:31 10:31 09:05 747 " +
                "4 Mo Duty start 05:15 19:15 DH 035 HNL-ICN 06:45 20:45 16:20 01:20 - 747 " +
                "5 Tu Duty start 11:20 20:20 099 ICN-ANC 12:50 21:50 20:51 12:51 08:01 747";

        List<FlightLeg> legs = ScheduleImportParser.parseCrewAccessText(
                text, FlightLeg.ChangeType.ORIGINAL, "");

        assertEquals(4, legs.size());
        assertEquals("DH 071", legs.get(0).flightNumber());
        assertEquals("Sep 4 09:13Z", legs.get(0).departureZulu());
        assertEquals("01:13-11:20", legs.get(0).localTimes());
        assertTrue(legs.stream().anyMatch(l -> l.flightNumber().equals("DH 035") && l.seatPosition().equals("DH")));
    }

    @Test public void parsesLooseWrappedColumnsFromOcr() {
        String text = "Trip Id: A70315 28Sep2026 1 Mo Duty start 21:16 13:16 099 ANC-SDF 22:46 14:46 04:53 00:53 06:07 747 " +
                "3 We Duty start 05:27 01:27 223 SDF-CGN 06:57 02:57 14:55 16:55 07:58 747 " +
                "4 Th Duty start 06:46 08:46 010 CGN-PVG 08:16 10:16 19:50 03:50 11:34 747 " +
                "6 Sa Duty start 17:50 01:50 081 PVG-ANC 19:20 03:20 04:04 20:04 08:44 747";
        List<FlightLeg> legs = ScheduleImportParser.parseCrewAccessText(text, FlightLeg.ChangeType.ORIGINAL, "");
        assertEquals(4, legs.size());
        assertEquals("UPS010", legs.get(2).flightNumber());
        assertEquals("Oct 1 08:16Z", legs.get(2).departureZulu());
        assertEquals("Oct 3 19:20Z", legs.get(3).departureZulu());
    }

    @Test public void acceptsTripIdOcrAsLowercaseL() {
        String text = "Trip ld: A70659 04Sep2026 1 Fr DH 071 ANC-SDF 09:13 01:13 15:20 11:20 747";
        List<FlightLeg> legs = ScheduleImportParser.parseCrewAccessText(text, FlightLeg.ChangeType.ORIGINAL, "");
        assertEquals(1, legs.size());
        assertEquals("A70659", legs.get(0).pairing());
    }
    @Test public void importsHyphenatedFlightNumbersFromCrewAccessTable(){
        String text = "Trip Id: A70816 03Aug2026 Day Flight Departure-Arrival Start Start(LT) End End(LT) " +
                "1 Mo Duty start 16:38 08:38 076-2 ANC-SZX 18:47 10:47 07:00 15:00 12:13 747 " +
                "3 We Duty start 18:15 02:15 DH UPS 065-2 SZX-ANC 19:45 03:45 05:15 21:15 09:30 747";
        List<FlightLeg> legs = ScheduleImportParser.parseCrewAccessText(text,FlightLeg.ChangeType.ORIGINAL,"");
        assertEquals(2,legs.size());
        assertEquals("UPS076-2",legs.get(0).flightNumber());
        assertEquals("DH 065-2",legs.get(1).flightNumber());
        assertEquals("DH",legs.get(1).seatPosition());
    }

    @Test public void parsesActualRosterFlightCardsAndSkipsHotels() {
        String text = "Roster Details " +
            "Oct 26 19:11 - Oct 27 07:12 UPS96 Pos AC Block ANC - CRK R/O 747 12:01h " +
            "Oct 27 07:42 - Oct 29 07:20 CRK - Clark Marriott Hotel 47:38h " +
            "Oct 29 08:20 - Oct 29 10:03 UPS95 Pos AC Block CRK - TPE F/O 747 1:43h " +
            "Oct 29 10:33 - Oct 30 11:00 TPE - Sheraton Taipei 24:27h " +
            "Oct 30 12:30 - Oct 30 21:10 UPS95 Pos AC Block TPE - ANC F/O 747 8:40h";
        List<FlightLeg> legs = ScheduleImportParser.parseCrewAccessText(
            text, FlightLeg.ChangeType.REVISED, "A70233R");
        assertEquals(3, legs.size());
        assertEquals("UPS96", legs.get(0).flightNumber());
        assertEquals("R/O", legs.get(0).seatPosition());
        assertEquals("Oct 29 08:20Z", legs.get(1).departureZulu());
        assertEquals("Oct 30 12:30Z", legs.get(2).departureZulu());
        assertEquals("A70233R", legs.get(2).pairing());
    }

    @Test public void rosterDoesNotConfuseHotelWithFlight() {
        String text = "Oct 27 07:42 - Oct 29 07:20 CRK - Clark Marriott Hotel 47:38h";
        assertEquals(0, ScheduleImportParser.parseCrewAccessText(
            text, FlightLeg.ChangeType.REVISED, "").size());
    }

}
