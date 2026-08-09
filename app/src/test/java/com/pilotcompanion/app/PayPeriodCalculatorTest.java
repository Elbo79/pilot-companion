package com.pilotcompanion.app;

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import java.time.LocalDate;

public class PayPeriodCalculatorTest {
    @Test public void groupsDatesInto28DayPayPeriods() {
        var period = PayPeriodCalculator.containing(LocalDate.of(2026, 8, 20));
        assertEquals(LocalDate.of(2026, 8, 2), period.start());
        assertEquals(LocalDate.of(2026, 8, 29), period.end());
    }
}
