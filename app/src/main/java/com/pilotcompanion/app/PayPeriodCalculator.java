package com.pilotcompanion.app;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

final class PayPeriodCalculator {
    // Configurable 2026 anchor. Contract default is 28-day pay periods; 35-day periods can be supplied later.
    private static final LocalDate ANCHOR = LocalDate.of(2026, 8, 2);
    private static final int DAYS = 28;

    record Period(LocalDate start, LocalDate end, int index) { }

    static Period containing(LocalDate date) {
        long delta = ChronoUnit.DAYS.between(ANCHOR, date);
        long bucket = Math.floorDiv(delta, DAYS);
        LocalDate start = ANCHOR.plusDays(bucket * DAYS);
        return new Period(start, start.plusDays(DAYS - 1), (int) bucket + 1);
    }

    static Period shift(Period period, int amount) {
        LocalDate start = period.start().plusDays((long) amount * DAYS);
        return new Period(start, start.plusDays(DAYS - 1), period.index() + amount);
    }
}
