package com.pilotcompanion.app;
import org.json.JSONArray;
import org.json.JSONObject;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

final class ScheduleBackup {
    static String encode(List<FlightLeg> legs) throws Exception {
        JSONArray rows = new JSONArray();
        for (FlightLeg l : legs) {
            JSONObject r = new JSONObject();
            r.put("number",l.flightNumber()); r.put("origin",l.origin()); r.put("destination",l.destination());
            r.put("departure",l.departure().toString()); r.put("arrival",l.arrival().toString());
            r.put("scheduledDeparture",l.scheduledDeparture().toString()); r.put("scheduledArrival",l.scheduledArrival().toString());
            r.put("assignment",l.assignment()); r.put("source",l.source()); r.put("hotel",l.hotel());
            r.put("change",l.changeType().name()); r.put("pairing",l.pairing()); rows.put(r);
        }
        return new JSONObject().put("format","pilot-companion-backup-1").put("flights",rows).toString();
    }
    static List<FlightLeg> decode(String text) throws Exception {
        JSONObject root=new JSONObject(text);
        if (!"pilot-companion-backup-1".equals(root.getString("format"))) throw new IllegalArgumentException("Unsupported backup");
        JSONArray rows=root.getJSONArray("flights");
        List<FlightLeg> result=new ArrayList<>();
        for(int i=0;i<rows.length();i++){
            JSONObject r=rows.getJSONObject(i);
            FlightLeg l=new FlightLeg(r.getString("number"),r.getString("origin"),r.getString("destination"),
                ZonedDateTime.parse(r.getString("departure")),ZonedDateTime.parse(r.getString("arrival")),
                ZonedDateTime.parse(r.getString("scheduledDeparture")),ZonedDateTime.parse(r.getString("scheduledArrival")),
                r.optString("assignment","FO"),r.optString("source","Backup"),r.isNull("hotel")?null:r.getString("hotel"),
                FlightLeg.ChangeType.valueOf(r.getString("change")),r.getString("pairing"));
            if(l.flightNumber().isBlank() || l.origin().isBlank() || l.destination().isBlank() || !l.arrival().isAfter(l.departure())) throw new IllegalArgumentException("Invalid flight");
            result.add(l);
        }
        if(result.isEmpty()) throw new IllegalArgumentException("Empty backup");
        return result;
    }
}
