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

    @Test public void parsesA70659TripInformationPage() {
        String text = "Trip Id: A70659 04Sep2026 " +
                "1 Fr 197 ANC-SDF 07:13 23:13 13:26 09:26 06:13 747 " +
                "2 Sa 036 SDF-HNL 11:26 07:26 20:31 10:31 09:05 747 " +
                "4 Mo DH 035 HNL-ICN 06:45 20:45 16:20 01:20 747 " +
                "5 Tu 099 ICN-ANC 12:50 21:50 20:51 12:51 08:01 747";

        List<FlightLeg> legs = ScheduleImportParser.parseCrewAccessText(
                text, FlightLeg.ChangeType.ORIGINAL, "");

        assertEquals(4, legs.size());
        assertEquals("A70659", legs.get(0).pairing());
        assertEquals("23:13-09:26", legs.get(0).localTimes());
        assertTrue(legs.stream().anyMatch(l -> l.flightNumber().equals("DH 035") && l.seatPosition().equals("DH")));
    }
}
