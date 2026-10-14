package com.pilotcompanion.app;

import android.content.Context;
import java.time.*;
import java.util.*;

final class ImportantDateRepository {
    private final List<ImportantDate> dates=new ArrayList<>();
    ImportantDateRepository(){}
    ImportantDateRepository(Context context){attach(context);}
    void attach(Context context){
        dates.clear();
        dates.addAll(new PilotDatabase(context).loadDates());
        // One-day reminders are generated from stored deadlines, not separately hard-coded.
        dates.addAll(dates.stream().filter(ImportantDate::deadline).map(d->
            new ImportantDate(d.date().minusDays(1),d.time(),d.title()+" tomorrow",ImportantDate.Type.REMINDER,false,d.source())).toList());
        dates.sort(Comparator.comparing(ImportantDate::date).thenComparing(d->d.time()==null?LocalTime.MAX:d.time()));
    }
    List<ImportantDate> forDate(LocalDate date){return dates.stream().filter(d->d.date().equals(date)).toList();}
    List<ImportantDate> deadlines(){return dates.stream().filter(ImportantDate::deadline).toList();}
}
