package com.pilotcompanion.app;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

final class PayPeriodCalculator {
    // UPS 2026 Dates to Remember: PP1 begins Dec 28, 2025; each published pay period is 28 days.
    private static final LocalDate ANCHOR = LocalDate.of(2025, 12, 28);
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
