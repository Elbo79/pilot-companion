package com.pilotcompanion.app;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class ScheduleChangeClassifierTest {
    @Test public void changeAfterTripStartIsAlwaysRevised() {
        assertEquals(FlightLeg.ChangeType.REVISED,
                ScheduleChangeClassifier.classify(true, true, true));
        assertEquals(FlightLeg.ChangeType.REVISED,
                ScheduleChangeClassifier.classify(true, true, false));
    }

    @Test public void preTripPilotTradeIsTraded() {
        assertEquals(FlightLeg.ChangeType.TRADED,
                ScheduleChangeClassifier.classify(true, false, true));
    }

    @Test public void preTripCompanyChangeIsRevised() {
        assertEquals(FlightLeg.ChangeType.REVISED,
                ScheduleChangeClassifier.classify(true, false, false));
    }

    @Test public void unchangedScheduleStaysOriginal() {
        assertEquals(FlightLeg.ChangeType.ORIGINAL,
                ScheduleChangeClassifier.classify(false, true, true));
    }
}
