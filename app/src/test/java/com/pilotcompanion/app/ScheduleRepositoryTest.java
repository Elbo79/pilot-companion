package com.pilotcompanion.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.LocalDate;

public class ScheduleRepositoryTest {
    private final ScheduleRepository repository = new ScheduleRepository();

    @Test public void placesUtcLegOnOriginLocalCalendarDate() {
        FlightLeg leg = repository.forDate(LocalDate.of(2026, 8, 3)).get(0);
        assertEquals("UPS52", leg.flightNumber());
        assertEquals("10:47-12:44", leg.localTimes());
        assertEquals("9h 57m", leg.flightTime());
    }

    @Test public void keepsAwardedTimesWhenCrewAccessRevisesLeg() {
        FlightLeg leg = repository.forDate(LocalDate.of(2026, 8, 3)).get(0);
        assertTrue(leg.isRevised());
        assertEquals("10:08-13:10", leg.scheduledLocalTimes());
    }

    @Test public void usesCrewAccessSeatAndAddedDeadheadLegs() {
        assertEquals("RO", repository.forDate(LocalDate.of(2026, 8, 10)).get(0).seatPosition());
        assertEquals("DH", repository.forDate(LocalDate.of(2026, 9, 6)).get(0).seatPosition());
    }

    @Test public void associatesFollowingHotelWithLeg() {
        assertEquals("Sheraton Incheon",
                repository.forDate(LocalDate.of(2026, 8, 6)).get(0).hotel());
        assertEquals("Ala Moana Honolulu by Mantra",
                repository.forDate(LocalDate.of(2026, 9, 5)).get(0).hotel());
    }
}
