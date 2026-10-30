package com.pilotcompanion.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import org.json.JSONArray;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;

final class PilotDatabase extends SQLiteOpenHelper {
    private final Context context;
    PilotDatabase(Context context) { super(context, "pilot_schedule.db", null, 3); this.context=context.getApplicationContext(); }
    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE flights (identity TEXT PRIMARY KEY, flight TEXT NOT NULL, origin TEXT NOT NULL, destination TEXT NOT NULL, departure TEXT NOT NULL, arrival TEXT NOT NULL, scheduled_departure TEXT NOT NULL, scheduled_arrival TEXT NOT NULL, assignment TEXT, source TEXT, hotel TEXT, change_type TEXT, pairing TEXT)");
        db.execSQL("CREATE INDEX idx_flights_departure ON flights(departure)");
        db.execSQL("CREATE TABLE important_dates (identity TEXT PRIMARY KEY, date TEXT NOT NULL, time TEXT, title TEXT NOT NULL, type TEXT NOT NULL, deadline INTEGER NOT NULL, source TEXT)");
        seed(db);
        seedNovember(db);
        db.execSQL("CREATE TABLE day_status (date TEXT PRIMARY KEY, status TEXT NOT NULL)");
    }
    @Override public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion) {
        if(oldVersion<2){
            // Replace incorrect Nov 19 Line 159 placeholder, retain all imported flights.
            db.delete("important_dates","date=? AND title=?",new String[]{"2026-11-19","Line 159 • Trip 354"});
            seedNovember(db);
        }
        if(oldVersion<3)db.execSQL("CREATE TABLE IF NOT EXISTS day_status (date TEXT PRIMARY KEY, status TEXT NOT NULL)");
    }
    private void seedNovember(SQLiteDatabase db){
        try{
            String json=new String(context.getAssets().open("november_line159.json").readAllBytes(),StandardCharsets.UTF_8);
            JSONArray legs=new JSONArray(json);
            for(int i=0;i<legs.length();i++)insert(db,ScheduleRepository.fromSeed(legs.getJSONObject(i)));
        }catch(Exception e){android.util.Log.e("PilotDatabase","Could not seed November flights",e);}
    }
    private void seed(SQLiteDatabase db) {
        try {
            String json=new String(context.getAssets().open("seed_flights.json").readAllBytes(), StandardCharsets.UTF_8);
            JSONArray flights=new JSONArray(json);
            for(int i=0;i<flights.length();i++){
                JSONObject o=flights.getJSONObject(i);
                FlightLeg leg=ScheduleRepository.fromSeed(o);
                insert(db,leg);
            }
        }catch(Exception e){android.util.Log.e("PilotDatabase","Could not seed flights",e);}
        try {
            String json=new String(context.getAssets().open("seed_dates.json").readAllBytes(), StandardCharsets.UTF_8);
            JSONArray dates=new JSONArray(json);
            for(int i=0;i<dates.length();i++){
                JSONObject o=dates.getJSONObject(i);
                saveDate(db,new ImportantDate(LocalDate.parse(o.getString("date")),o.isNull("time")?null:LocalTime.parse(o.getString("time")),o.getString("title"),ImportantDate.Type.valueOf(o.getString("type")),o.getBoolean("deadline"),o.getString("source")));
            }
        }catch(Exception e){android.util.Log.e("PilotDatabase","Could not seed dates",e);}
    }
    static String identity(FlightLeg leg){
        // Include scheduled occurrence, so repeated flight numbers in separate trips remain distinct.
        return leg.pairing()+"|"+leg.flightNumber()+"|"+leg.origin()+"|"+leg.destination()+"|"+leg.scheduledDeparture().toInstant().toString();
    }
    static void insert(SQLiteDatabase db,FlightLeg leg) {
        ContentValues v=new ContentValues();
        v.put("identity",identity(leg));v.put("flight",leg.flightNumber());v.put("origin",leg.origin());v.put("destination",leg.destination());
        v.put("departure",leg.departure().toInstant().toString());v.put("arrival",leg.arrival().toInstant().toString());
        v.put("scheduled_departure",leg.scheduledDeparture().toInstant().toString());v.put("scheduled_arrival",leg.scheduledArrival().toInstant().toString());
        v.put("assignment",leg.assignment());v.put("source",leg.source());v.put("hotel",leg.hotel());v.put("change_type",leg.changeType().name());v.put("pairing",leg.pairing());
        db.insertWithOnConflict("flights",null,v,SQLiteDatabase.CONFLICT_REPLACE);
    }
    void saveFlights(List<FlightLeg> flights) { saveFlights(flights, null); }
    void saveFlights(List<FlightLeg> flights, String replacedPairing) {
        SQLiteDatabase db=getWritableDatabase();db.beginTransaction();
        try{
            if (replacedPairing != null && !replacedPairing.isBlank()) {
                db.delete("flights", "pairing=?", new String[]{replacedPairing});
            }
            for(FlightLeg leg:flights){
                if(replacedPairing == null && leg.changeType()==FlightLeg.ChangeType.REVISED){
                    // A revised departure can shift to another calendar date.
                    // Replace matching flight/route within four days, but not unrelated legs.
                    String from=leg.departure().toInstant().minus(java.time.Duration.ofDays(4)).toString();
                    String to=leg.departure().toInstant().plus(java.time.Duration.ofDays(4)).toString();
                    db.delete("flights","flight=? AND origin=? AND destination=? AND departure>=? AND departure<=?",
                        new String[]{leg.flightNumber(),leg.origin(),leg.destination(),from,to});
                }
                insert(db,leg);
            }
            db.setTransactionSuccessful();
        }finally{db.endTransaction();}
    }
    void replaceRemainingTrip(List<FlightLeg> incoming, String pairing, Instant cutoff) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete("flights", "pairing=? AND departure>=?", new String[]{pairing, cutoff.toString()});
            for (FlightLeg leg : incoming) insert(db, leg);
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }
    void removePairing(String pairing) {
        if (pairing == null || pairing.isBlank()) throw new IllegalArgumentException("Pairing required");
        getWritableDatabase().delete("flights", "pairing=?", new String[]{pairing});
    }
    List<FlightLeg> loadFlights() {
        List<FlightLeg> list=new ArrayList<>();
        try(Cursor c=getReadableDatabase().rawQuery("SELECT flight,origin,destination,departure,arrival,scheduled_departure,scheduled_arrival,assignment,source,hotel,change_type,pairing FROM flights ORDER BY departure",null)){
            while(c.moveToNext()){
                String from=c.getString(1),to=c.getString(2);
                list.add(new FlightLeg(c.getString(0),from,to,ScheduleRepository.zoned(c.getString(3),from),ScheduleRepository.zoned(c.getString(4),to),
                ScheduleRepository.zoned(c.getString(5),from),ScheduleRepository.zoned(c.getString(6),to),c.getString(7),c.getString(8),c.getString(9),FlightLeg.ChangeType.valueOf(c.getString(10)),c.getString(11)));
            }
        }return list;
    }
    private static void saveDate(SQLiteDatabase db,ImportantDate date) {
        ContentValues v=new ContentValues();
        v.put("identity",date.date()+"|"+date.title()+"|"+date.type());
        v.put("date",date.date().toString());v.put("time",date.time()==null?null:date.time().toString());
        v.put("title",date.title());v.put("type",date.type().name());v.put("deadline",date.deadline()?1:0);v.put("source",date.source());
        db.insertWithOnConflict("important_dates",null,v,SQLiteDatabase.CONFLICT_REPLACE);
    }
    List<ImportantDate> loadDates(){
        List<ImportantDate> list=new ArrayList<>();
        try(Cursor c=getReadableDatabase().rawQuery("SELECT date,time,title,type,deadline,source FROM important_dates ORDER BY date,time",null)){
            while(c.moveToNext())list.add(new ImportantDate(LocalDate.parse(c.getString(0)),c.isNull(1)?null:LocalTime.parse(c.getString(1)),c.getString(2),ImportantDate.Type.valueOf(c.getString(3)),c.getInt(4)!=0,c.getString(5)));
        }return list;
    }
    Map<LocalDate,String> loadDayStatuses(){
        Map<LocalDate,String> out=new HashMap<>();
        try(Cursor c=getReadableDatabase().rawQuery("SELECT date,status FROM day_status",null)){
            while(c.moveToNext())out.put(LocalDate.parse(c.getString(0)),c.getString(1));
        }
        return out;
    }
    void setDayStatus(LocalDate date,String status){
        SQLiteDatabase db=getWritableDatabase();
        if(status==null){db.delete("day_status","date=?",new String[]{date.toString()});return;}
        ContentValues v=new ContentValues();v.put("date",date.toString());v.put("status",status);
        db.insertWithOnConflict("day_status",null,v,SQLiteDatabase.CONFLICT_REPLACE);
    }

}
