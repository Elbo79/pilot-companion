package com.pilotcompanion.app;

import static org.junit.Assert.*;
import org.junit.Test;
import org.json.JSONObject;
import java.time.*;

public class ScheduleRepositoryTest {
    @Test public void retainsLocalTimeZoneAndPairingFromStructuredSeed() throws Exception {
        JSONObject seed=new JSONObject();
        seed.put("flight","UPS52");seed.put("from","ANC");seed.put("to","HKG");
        seed.put("out","2026-08-03T18:47");seed.put("in","2026-08-04T04:44");
        seed.put("scheduledOut",JSONObject.NULL);seed.put("scheduledIn",JSONObject.NULL);
        seed.put("assignment","F/O");seed.put("source","Crew Access");seed.put("hotel","New World Millennium");
        seed.put("change","ORIGINAL");seed.put("pairing","A70816");
        FlightLeg leg=ScheduleRepository.fromSeed(seed);
        assertEquals("10:47-12:44",leg.localTimes());
        assertEquals("9h 57m",leg.flightTime());
        assertEquals("A70816",leg.pairing());
    }
    @Test public void multiplePairingsDoNotOverwriteSameFlightNumber() throws Exception {
        JSONObject a=new JSONObject("{\\"flight\\":\\"UPS099\\",\\"from\\":\\"ANC\\",\\"to\\":\\"SDF\\",\\"out\\":\\"2026-09-09T22:46\\",\\"in\\":\\"2026-09-10T04:53\\",\\"scheduledOut\\":null,\\"scheduledIn\\":null,\\"assignment\\":\\"FO\\",\\"source\\":\\"test\\",\\"hotel\\":null,\\"change\\":\\"ORIGINAL\\",\\"pairing\\":\\"16168\\"}");
        JSONObject b=new JSONObject(a.toString());b.put("pairing","A70315");b.put("out","2026-09-28T22:46");b.put("in","2026-09-29T04:53");
        assertNotEquals(PilotDatabase.identity(ScheduleRepository.fromSeed(a)),PilotDatabase.identity(ScheduleRepository.fromSeed(b)));
    }
}
