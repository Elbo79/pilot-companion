package com.pilotcompanion.app;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class ImportantDateRepository {
    private final List<ImportantDate> dates = new ArrayList<>();

    ImportantDateRepository() {
        // BP2606 First Officer transition dates. Times are ANC local domicile time unless stated otherwise by the source.
        add(2026, 8, 17, 5, 0, "Primary bid closes", ImportantDate.Type.BID, true, "2606 ANC 74Y Bid Package FO timeline");
        add(2026, 8, 17, 9, 0, "Tentative awards", ImportantDate.Type.BID, false, "2606 ANC 74Y Bid Package FO timeline");
        add(2026, 8, 18, null, null, "BV banking deadline", ImportantDate.Type.VACATION, true, "2026 Dates to Remember");
        add(2026, 8, 20, 4, 0, "LINE self-adjust opens", ImportantDate.Type.BID, false, "2606 ANC 74Y Bid Package FO timeline");
        add(2026, 8, 21, 6, 0, "LINE self-adjust closes", ImportantDate.Type.BID, true, "2606 ANC 74Y Bid Package FO timeline");
        add(2026, 8, 24, null, null, "Payday", ImportantDate.Type.PAYDAY, false, "2026 Dates to Remember");
        add(2026, 8, 26, 8, 0, "Secondary VTO/BRL closes", ImportantDate.Type.BID, true, "2606 ANC 74Y Bid Package FO timeline");
        add(2026, 8, 27, 10, 0, "VTO OCV closes", ImportantDate.Type.BID, true, "2606 ANC 74Y Bid Package FO timeline");

        add(2026, 9, 1, null, null, "Primary vacation bid posted", ImportantDate.Type.VACATION, false, "2026 Dates to Remember");
        add(2026, 9, 6, 2, 59, "PP09 ends", ImportantDate.Type.PAY_PERIOD, false, "2026 Dates to Remember / 2606 Bid Package");
        add(2026, 9, 6, 3, 0, "PP10 begins", ImportantDate.Type.PAY_PERIOD, false, "2606 ANC 74Y Bid Package");
        add(2026, 9, 8, null, null, "Payday", ImportantDate.Type.PAYDAY, false, "2026 Dates to Remember");
        add(2026, 9, 15, null, null, "Primary vacation due", ImportantDate.Type.VACATION, true, "2026 Dates to Remember");
        add(2026, 9, 18, null, null, "Primary vacation awards", ImportantDate.Type.VACATION, false, "2026 Dates to Remember");
        add(2026, 9, 21, null, null, "Secondary vacation posted", ImportantDate.Type.VACATION, false, "2026 Dates to Remember");
        add(2026, 9, 21, null, null, "Payday", ImportantDate.Type.PAYDAY, false, "2026 Dates to Remember");
        add(2026, 9, 29, 8, 0, "LITT requests processed", ImportantDate.Type.BID, false, "2606 ANC 74Y Bid Package FO timeline");

        add(2026, 10, 1, 4, 0, "BP2607 published", ImportantDate.Type.BID, false, "2606 ANC 74Y Bid Package FO timeline");
        add(2026, 10, 4, 2, 59, "PP10 ends", ImportantDate.Type.PAY_PERIOD, false, "2606 ANC 74Y Bid Package");
        add(2026, 10, 4, 3, 0, "PP11 begins", ImportantDate.Type.PAY_PERIOD, false, "2606 ANC 74Y Bid Package");
        add(2026, 10, 5, null, null, "Secondary vacation due", ImportantDate.Type.VACATION, true, "2026 Dates to Remember");
        add(2026, 10, 5, null, null, "Payday", ImportantDate.Type.PAYDAY, false, "2026 Dates to Remember");
        add(2026, 10, 8, null, null, "Secondary vacation awards", ImportantDate.Type.VACATION, false, "2026 Dates to Remember");
        add(2026, 10, 11, null, null, "Tertiary vacation posted", ImportantDate.Type.VACATION, false, "2026 Dates to Remember");

        // Current roster items from Crew Access screenshots supplied 04 Sep 2026.
        // CQ06 card shows 15:30Z-00:30Z at ANC; 15:30Z = 07:30 AKDT on Oct 13.
        add(2026, 10, 13, 7, 30, "TRAINING CQ06", ImportantDate.Type.BID, false, "Crew Access roster screenshot");
        add(2026, 10, 18, null, null, "Vacation day", ImportantDate.Type.VACATION, false, "Crew Access roster screenshot");
        add(2026, 10, 19, null, null, "Vacation day", ImportantDate.Type.VACATION, false, "Crew Access roster screenshot");

        add(2026, 10, 19, null, null, "Payday", ImportantDate.Type.PAYDAY, false, "2026 Dates to Remember");
        add(2026, 10, 25, null, null, "Tertiary vacation due", ImportantDate.Type.VACATION, true, "2026 Dates to Remember");
        add(2026, 10, 28, null, null, "Tertiary vacation awards", ImportantDate.Type.VACATION, false, "2026 Dates to Remember");

        add(2026, 11, 1, 2, 59, "PP11 ends", ImportantDate.Type.PAY_PERIOD, false, "2606 ANC 74Y Bid Package");
        add(2026, 11, 1, 3, 0, "PP12 begins", ImportantDate.Type.PAY_PERIOD, false, "2026 Dates to Remember");
        add(2026, 11, 2, null, null, "Payday", ImportantDate.Type.PAYDAY, false, "2026 Dates to Remember");
        add(2026, 11, 16, null, null, "Payday", ImportantDate.Type.PAYDAY, false, "2026 Dates to Remember");
        add(2026, 11, 29, 2, 59, "PP12 ends", ImportantDate.Type.PAY_PERIOD, false, "2026 Dates to Remember");
        add(2026, 11, 29, 3, 0, "PP13 begins", ImportantDate.Type.PAY_PERIOD, false, "2026 Dates to Remember");
        add(2026, 11, 30, null, null, "Payday", ImportantDate.Type.PAYDAY, false, "2026 Dates to Remember");

        // One-day advance reminder entries for every deadline/due date.
        List<ImportantDate> reminders = dates.stream().filter(ImportantDate::deadline).map(d ->
                new ImportantDate(d.date().minusDays(1), d.time(), d.title() + " tomorrow", ImportantDate.Type.REMINDER, false, d.source())
        ).toList();
        dates.addAll(reminders);
        dates.sort(Comparator.comparing(ImportantDate::date).thenComparing(d -> d.time() == null ? LocalTime.MAX : d.time()));
    }

    List<ImportantDate> forDate(LocalDate date) {
        return dates.stream().filter(d -> d.date().equals(date)).toList();
    }

    List<ImportantDate> deadlines() {
        return dates.stream().filter(ImportantDate::deadline).toList();
    }

    private void add(int year, int month, int day, Integer hour, Integer minute, String title,
                     ImportantDate.Type type, boolean deadline, String source) {
        LocalTime time = hour == null ? null : LocalTime.of(hour, minute == null ? 0 : minute);
        dates.add(new ImportantDate(LocalDate.of(year, month, day), time, title, type, deadline, source));
    }
}
