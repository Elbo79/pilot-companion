package com.pilotcompanion.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import java.time.LocalDate;

public class ScheduleRepositoryTest {
    private final ScheduleRepository repository = new ScheduleRepository();

    @Test public void placesUtcLegOnOriginLocalCalendarDate(){
        FlightLeg leg=repository.forDate(LocalDate.of(2026,8,3)).get(0);
        assertEquals("UPS52",leg.flightNumber());
        assertEquals("10:47-12:44",leg.localTimes());
        assertEquals("9h 57m",leg.flightTime());
    }

    @Test public void keepsAwardedTimesWhenCrewAccessRevisesLeg(){
        FlightLeg leg=repository.forDate(LocalDate.of(2026,8,3)).get(0);
        assertTrue(leg.isRevised());
        assertEquals("10:08-13:10",leg.scheduledLocalTimes());
    }

    @Test public void associatesFollowingHotelWithLeg(){
        assertEquals("Sheraton Incheon",repository.forDate(LocalDate.of(2026,8,6)).get(0).hotel());
        assertEquals("Ala Moana Honolulu by Mantra",repository.forDate(LocalDate.of(2026,9,5)).get(0).hotel());
    }

    @Test public void marksNewPairingAsTraded(){
        FlightLeg leg=repository.forDate(LocalDate.of(2026,8,20)).get(0);
        assertTrue(leg.isTraded());
        assertEquals("A70327R",leg.pairing());
        assertEquals("UPS71",leg.flightNumber());
    }

    @Test public void tradedRestSeatMapsToRo(){
        assertEquals("RO",repository.forDate(LocalDate.of(2026,8,25)).get(0).seatPosition());
    }

    @Test public void replacesOldReturnWithPostStartRevision(){
        FlightLeg cgnSzx=repository.forDate(LocalDate.of(2026,8,11)).get(0);
        assertEquals("UPS008",cgnSzx.flightNumber());
        assertEquals("CGN",cgnSzx.origin());
        assertEquals("SZX",cgnSzx.destination());
        assertEquals("FO",cgnSzx.seatPosition());
        assertEquals("07:27-01:10",cgnSzx.localTimes());
        assertEquals("11h 43m",cgnSzx.flightTime());
        assertTrue(cgnSzx.isRevised());
    }

    @Test public void revisedSzxAncAppearsOnOriginLocalDate(){
        FlightLeg szxAnc=repository.forDate(LocalDate.of(2026,8,13)).get(0);
        assertEquals("UPS071",szxAnc.flightNumber());
        assertEquals("05:05-23:08",szxAnc.localTimes());
        assertEquals("10h 03m",szxAnc.flightTime());
        assertEquals("FO",szxAnc.seatPosition());
        assertTrue(szxAnc.isRevised());
    }
}
