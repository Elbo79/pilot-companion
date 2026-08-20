package com.pilotcompanion.app;

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import java.time.LocalDate;

public class PayPeriodCalculatorTest {
    @Test public void aug20FallsInOfficialPp9() {
        var period = PayPeriodCalculator.containing(LocalDate.of(2026, 8, 20));
        assertEquals(9, period.index());
        assertEquals(LocalDate.of(2026, 8, 9), period.start());
        assertEquals(LocalDate.of(2026, 9, 5), period.end());
    }

    @Test public void sep6StartsOfficialPp10() {
        var period = PayPeriodCalculator.containing(LocalDate.of(2026, 9, 6));
        assertEquals(10, period.index());
        assertEquals(LocalDate.of(2026, 9, 6), period.start());
        assertEquals(LocalDate.of(2026, 10, 3), period.end());
    }
}
