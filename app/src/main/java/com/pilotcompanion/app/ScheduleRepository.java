package com.pilotcompanion.app;

import android.content.Context;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;

final class ScheduleRepository {
    private final Map<LocalDate,List<FlightLeg>> schedule=new HashMap<>();
    private final Map<LocalDate,List<String>> reminders=new HashMap<>();
    private final Context context;
    ScheduleRepository(Context context){this.context=context;loadAward();loadSaved();}
    private static ZoneId zone(String code){return switch(code){
        case "ANC"->ZoneId.of("America/Anchorage");case "SDF"->ZoneId.of("America/Kentucky/Louisville");
        case "CGN"->ZoneId.of("Europe/Berlin");case "SZX"->ZoneId.of("Asia/Shanghai");
        case "ICN"->ZoneId.of("Asia/Seoul");case "HKG"->ZoneId.of("Asia/Hong_Kong");
        case "DEL"->ZoneId.of("Asia/Kolkata");case "HNL"->ZoneId.of("Pacific/Honolulu");
        case "ONT"->ZoneId.of("America/Los_Angeles");case "SYD"->ZoneId.of("Australia/Sydney");
        case "CRK"->ZoneId.of("Asia/Manila");default->ZoneOffset.UTC;};}
    void add(String number,String from,String to,String out,String in,String seat,String source){
        try {
            ZonedDateTime dep=LocalDateTime.parse(out).atZone(ZoneOffset.UTC).withZoneSameInstant(zone(from));
            ZonedDateTime arr=LocalDateTime.parse(in).atZone(ZoneOffset.UTC).withZoneSameInstant(zone(to));
            FlightLeg leg=new FlightLeg(number,from,to,dep,arr,dep,arr,seat,source,null);
            schedule.computeIfAbsent(dep.toLocalDate(),k->new ArrayList<>()).add(leg);
        }catch(Exception ignored){}
    }
    private void loadAward(){
        try(InputStream stream=context.getAssets().open("award-2607.json")){
            String json=new String(stream.readAllBytes(),StandardCharsets.UTF_8);
            JSONObject root=new JSONObject(json);
            JSONArray trips=root.getJSONArray("trips");
            for(int i=0;i<trips.length();i++){
                JSONObject trip=trips.getJSONObject(i);
                JSONArray dates=trip.getJSONArray("reportDatesZulu"),legs=trip.getJSONArray("legs");
                for(int d=0;d<dates.length();d++){
                    LocalDate report=LocalDate.parse(dates.getString(d));
                    LocalDate cursor=report;
                    Instant prior=null;
                    for(int j=0;j<legs.length();j++){
                        JSONObject leg=legs.getJSONObject(j);
                        String out=leg.getString("departureZulu"),in=leg.getString("arrivalZulu");
                        LocalDateTime departure=LocalDateTime.of(cursor,LocalTime.parse(out));
                        if(prior!=null){while(!departure.toInstant(ZoneOffset.UTC).isAfter(prior))departure=departure.plusDays(1);}
                        LocalDateTime arrival=LocalDateTime.of(departure.toLocalDate(),LocalTime.parse(in));
                        while(!arrival.isAfter(departure))arrival=arrival.plusDays(1);
                        prior=arrival.toInstant(ZoneOffset.UTC);cursor=arrival.toLocalDate();
                        String seat=leg.optBoolean("deadhead")?"DH":leg.optBoolean("iro")?"IRO":leg.optString("seatPosition","UNKNOWN");
                        add(leg.getString("flight"),leg.getString("from"),leg.getString("to"),departure.toString(),arrival.toString(),seat,"2607 bid / Trip "+trip.getInt("tripNumber"));
                    }
                }
            }
            JSONArray dates=root.optJSONArray("importantDates");
            if(dates!=null)for(int i=0;i<dates.length();i++){JSONObject o=dates.getJSONObject(i);if(!o.isNull("date"))remind(LocalDate.parse(o.getString("date")),o.getString("label"));}
        }catch(Exception ignored){}
    }
    private void remind(LocalDate date,String message){reminders.computeIfAbsent(date,k->new ArrayList<>()).add(message);}
    private void loadSaved(){
        try(BufferedReader reader=new BufferedReader(new InputStreamReader(context.openFileInput("schedule.csv")))){
            String line;while((line=reader.readLine())!=null)parseLine(line);
        }catch(Exception ignored){}
    }
    private void parseLine(String line){
        String[] a=line.trim().split(",",-1);
        if(a.length==7)add(a[1].trim(),a[2].trim().toUpperCase(Locale.US),a[3].trim().toUpperCase(Locale.US),a[4].trim(),a[5].trim(),a[6].trim(),"Imported screenshot / CSV");
    }
    int importCsv(String text){
        int count=0;StringBuilder accepted=new StringBuilder();
        for(String line:text.split("\\R")){
            String[] a=line.trim().split(",",-1);
            if(a.length!=7)continue;
            try{
                LocalDate.parse(a[0].trim());LocalDateTime.parse(a[4].trim());LocalDateTime.parse(a[5].trim());
                if(!a[2].trim().matches("[A-Za-z]{3}")||!a[3].trim().matches("[A-Za-z]{3}"))continue;
                parseLine(line);accepted.append(line.trim()).append('\n');count++;
            }catch(Exception ignored){}
        }
        if(count>0)try(FileOutputStream f=context.openFileOutput("schedule.csv",Context.MODE_APPEND)){f.write(accepted.toString().getBytes(StandardCharsets.UTF_8));}catch(Exception ignored){}
        return count;
    }
    List<FlightLeg> forDate(LocalDate date){return schedule.getOrDefault(date,List.of());}
    List<String> notes(LocalDate date){return reminders.getOrDefault(date,List.of());}
    LocalDate firstScheduledDate(){return LocalDate.of(2026,10,1);}
    boolean hasAnything(LocalDate date){return !forDate(date).isEmpty()||!notes(date).isEmpty();}
}
