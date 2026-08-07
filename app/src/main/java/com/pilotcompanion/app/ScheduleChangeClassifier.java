package com.pilotcompanion.app;

/**
 * Central rule for classifying schedule changes.
 *
 * User rule: once a trip has started, any subsequent schedule change is a revision.
 */
final class ScheduleChangeClassifier {
    private ScheduleChangeClassifier() { }

    static FlightLeg.ChangeType classify(boolean scheduleChanged, boolean tripStarted, boolean pilotInitiatedTrade) {
        if (!scheduleChanged) return FlightLeg.ChangeType.ORIGINAL;
        if (tripStarted) return FlightLeg.ChangeType.REVISED;
        if (pilotInitiatedTrade) return FlightLeg.ChangeType.TRADED;
        return FlightLeg.ChangeType.REVISED;
    }
}
