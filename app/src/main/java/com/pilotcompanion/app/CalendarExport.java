package com.pilotcompanion.app;

import android.content.ContentValues;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Events;
import java.util.ArrayList;
import java.util.List;

final class CalendarExport {
    record Target(long id, String name) {}
    static List<Target> targets(Context context) {
        List<Target> result = new ArrayList<>();
        try (Cursor c = context.getContentResolver().query(Calendars.CONTENT_URI,
                new String[]{Calendars._ID, Calendars.CALENDAR_DISPLAY_NAME, Calendars.ACCOUNT_NAME},
                Calendars.CALENDAR_ACCESS_LEVEL + ">=? AND " + Calendars.ACCOUNT_TYPE + "=?",
                new String[]{String.valueOf(Calendars.CAL_ACCESS_CONTRIBUTOR), "com.google"}, null)) {
            if (c != null) while (c.moveToNext()) result.add(new Target(c.getLong(0), c.getString(1)+" ("+c.getString(2)+")"));
        }
        return result;
    }
    static int export(Context context, long calendar, List<FlightLeg> legs) {
        int count = 0;
        for (FlightLeg leg : legs) {
            // Stable identity for a revised flight; never match personal events by title.
            String key = "pilotcompanion:" + leg.pairing() + ":" + leg.flightNumber() + ":" +
                    leg.origin() + ":" + leg.destination() + ":" + leg.scheduledDeparture().toLocalDate();
            ContentValues v = new ContentValues();
            v.put(Events.CALENDAR_ID, calendar);
            v.put(Events.TITLE, leg.seatPosition()+" "+leg.flightNumber()+" "+leg.origin()+" → "+leg.destination());
            v.put(Events.DTSTART, leg.departure().toInstant().toEpochMilli());
            v.put(Events.DTEND, leg.arrival().toInstant().toEpochMilli());
            v.put(Events.EVENT_TIMEZONE, leg.departure().getZone().getId());
            v.put(Events.EVENT_END_TIMEZONE, leg.arrival().getZone().getId());
            v.put(Events.DESCRIPTION, key+"\nTrip "+leg.pairing()+" • "+leg.changeLabel()+"\n"+leg.zuluTimes()+
                    (leg.hotel()==null || leg.hotel().isBlank() ? "" : "\nHotel: "+leg.hotel()));
            v.put(Events.EVENT_LOCATION, leg.origin()+" → "+leg.destination());
            long id = -1;
            try (Cursor c = context.getContentResolver().query(Events.CONTENT_URI, new String[]{Events._ID},
                    Events.CALENDAR_ID+"=? AND "+Events.DESCRIPTION+" LIKE ? AND "+Events.DELETED+"=0",
                    new String[]{String.valueOf(calendar), key+"\n%"}, null)) {
                if (c != null && c.moveToFirst()) id = c.getLong(0);
            }
            if (id >= 0) {
                if (context.getContentResolver().update(ContentUris.withAppendedId(Events.CONTENT_URI,id),v,null,null)>0) count++;
            } else if (context.getContentResolver().insert(Events.CONTENT_URI,v)!=null) count++;
        }
        return count;
    }
}
