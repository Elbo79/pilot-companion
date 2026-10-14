package com.pilotcompanion.app;

import static org.junit.Assert.*;
import org.junit.Test;
import java.time.*;

public class ScheduleRepositoryTest {
    @Test public void convertsZuluToLocalTimeAcrossDateLine(){
        ZonedDateTime departure=Instant.parse("2026-11-05T19:53:00Z").atZone(ZoneId.of("America/Anchorage"));
        ZonedDateTime arrival=Instant.parse("2026-11-06T08:05:00Z").atZone(ZoneId.of("Asia/Shanghai"));
        FlightLeg leg=new FlightLeg("UPS076-2","ANC","SZX",departure,arrival,departure,arrival,"FO","2607",null);
        assertEquals("10:53-16:05",leg.localTimes());
        assertEquals("12h 12m",leg.flightTime());
    }
    @Test public void preservesIroAndDeadheadAssignments(){
        ZonedDateTime now=ZonedDateTime.now(ZoneOffset.UTC);
        assertEquals("RO",new FlightLeg("UPS014","CGN","SZX",now,now.plusHours(11),now,now.plusHours(11),"IRO","2607",null).seatPosition());
        assertEquals("DH",new FlightLeg("UPS201","SDF","CGN",now,now.plusHours(8),now,now.plusHours(8),"DH","2607",null).seatPosition());
    }
}
