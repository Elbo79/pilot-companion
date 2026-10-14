package com.pilotcompanion.app;

import static org.junit.Assert.*;
import org.junit.Test;
import java.time.*;

public class ScheduleRepositoryTest {
    private FlightLeg leg(String pairing,String date){
        ZonedDateTime out=LocalDateTime.parse(date+"T18:47").atZone(ZoneOffset.UTC).withZoneSameInstant(ZoneId.of("America/Anchorage"));
        ZonedDateTime in=out.plusHours(10).withZoneSameInstant(ZoneId.of("Asia/Hong_Kong"));
        return new FlightLeg("UPS099","ANC","HKG",out,in,out,in,"F/O","Crew Access",null,FlightLeg.ChangeType.ORIGINAL,pairing);
    }
    @Test public void storesSeparatePairingsIndependently(){
        assertNotEquals(PilotDatabase.identity(leg("16168","2026-09-09")),PilotDatabase.identity(leg("A70315","2026-09-28")));
    }
    @Test public void keepsOriginLocalDateAndFlightTime(){
        FlightLeg flight=leg("A70816","2026-08-03");
        assertEquals(LocalDate.of(2026,8,3),flight.departure().toLocalDate());
        assertEquals("10h 00m",flight.flightTime());
    }
}
