package com.pilotcompanion.app;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;

public class FlightLegTest {
    private FlightLeg leg(String assignment) {
        ZonedDateTime departure = ZonedDateTime.of(2026, 8, 1, 22, 0, 0, 0, ZoneId.of("America/Anchorage"));
        ZonedDateTime arrival = departure.toInstant().plusSeconds(5 * 3600 + 25 * 60).atZone(ZoneId.of("Asia/Seoul"));
        return new FlightLeg("5X1", "ANC", "ICN", departure, arrival,
                departure, arrival, assignment, "test", null);
    }

    @Test public void appliesSeatPositionRules() {
        assertEquals("RO", leg("IRO").seatPosition());
        assertEquals("RO", leg("R/O").seatPosition());
        assertEquals("RO", leg("RO").seatPosition());
        assertEquals("FO2", leg("FO2").seatPosition());
        assertEquals("DH", leg("deadhead").seatPosition());
        assertEquals("FO", leg("Captain").seatPosition());
    }

    @Test public void calculatesElapsedFlightTimeAcrossTimeZones() {
        assertEquals("5h 25m", leg("FO").flightTime());
    }
}
