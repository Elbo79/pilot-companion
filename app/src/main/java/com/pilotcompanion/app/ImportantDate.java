package com.pilotcompanion.app;

import java.time.LocalDate;
import java.time.LocalTime;

public record ImportantDate(LocalDate date, LocalTime time, String title, Type type, boolean deadline, String source) {
    public enum Type { BID, VACATION, PAY_PERIOD, PAYDAY, REMINDER }

    public String calendarLabel() {
        String timeText = time == null ? "" : " " + time.toString();
        if (title.toUpperCase().startsWith("TRAINING")) return title + timeText;
        return switch (type) {
            case BID -> "BID " + title + timeText;
            case VACATION -> "VAC " + title + timeText;
            case PAY_PERIOD -> "PP " + title + timeText;
            case PAYDAY -> "PAYDAY" + timeText;
            case REMINDER -> "REM " + title + timeText;
        };
    }
}
