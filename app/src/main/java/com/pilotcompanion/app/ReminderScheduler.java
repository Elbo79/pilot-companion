package com.pilotcompanion.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

final class ReminderScheduler {
    private static final ZoneId DOMICILE = ZoneId.of("America/Anchorage");

    static void scheduleAll(Context context, ImportantDateRepository repository) {
        AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        long now = System.currentTimeMillis();
        int request = 4000;
        for (ImportantDate deadline : repository.deadlines()) {
            LocalTime reminderTime = deadline.time() == null ? LocalTime.of(9, 0) : deadline.time();
            long trigger = LocalDateTime.of(deadline.date().minusDays(1), reminderTime)
                    .atZone(DOMICILE).toInstant().toEpochMilli();
            if (trigger <= now) { request++; continue; }
            Intent intent = new Intent(context, ReminderReceiver.class)
                    .putExtra("title", deadline.title() + " due tomorrow")
                    .putExtra("body", deadline.time() == null
                            ? "Pilot Companion reminder: " + deadline.title() + " is due tomorrow."
                            : "Pilot Companion reminder: " + deadline.title() + " is due tomorrow at " + deadline.time() + " ANC time.");
            PendingIntent pending = PendingIntent.getBroadcast(context, request++, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pending);
        }
    }

    private ReminderScheduler() { }
}
