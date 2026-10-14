package com.pilotcompanion.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import java.io.InputStream;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public final class MainActivity extends Activity {
    private static final int PICK=42;
    private ScheduleRepository repository;
    private YearMonth visibleMonth;
    private TextView title;
    private MonthCalendarView calendar;
    private LinearLayout details;
    private LocalDate selected;
    private final int white=0xFFFFFFFF,blue=0xFF68D8FF;
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        repository=new ScheduleRepository(this);
        visibleMonth=YearMonth.of(2026,10);
        selected=LocalDate.now();
        build();
    }
    private TextView text(String value,int size,int color){
        TextView v=new TextView(this);v.setText(value);v.setTextSize(size);v.setTextColor(color);v.setPadding(0,6,0,6);return v;
    }
    private Button button(String value){Button b=new Button(this);b.setText(value);b.setAllCaps(false);return b;}
    private void build(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(12,8,12,8);root.setBackgroundColor(0xFF071B2F);
        LinearLayout nav=new LinearLayout(this);nav.setGravity(Gravity.CENTER_VERTICAL);
        Button prev=button("‹"),next=button("›");
        title=text("",21,white);title.setGravity(Gravity.CENTER);
        nav.addView(prev,new LinearLayout.LayoutParams(54,56));
        nav.addView(title,new LinearLayout.LayoutParams(0,56,1));
        nav.addView(next,new LinearLayout.LayoutParams(54,56));root.addView(nav);
        LinearLayout actions=new LinearLayout(this);
        Button upload=button("Upload screenshot"),csv=button("Import text / CSV");
        actions.addView(upload,new LinearLayout.LayoutParams(0,50,1));
        actions.addView(csv,new LinearLayout.LayoutParams(0,50,1));root.addView(actions);
        calendar=new MonthCalendarView(this,repository);
        calendar.setOnDateSelectedListener(this::showDay);
        root.addView(calendar,new LinearLayout.LayoutParams(-1,0,3));
        ScrollView scroll=new ScrollView(this);
        details=new LinearLayout(this);details.setOrientation(1);details.setPadding(10,4,10,4);
        scroll.addView(details);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,2));
        setContentView(root);
        prev.setOnClickListener(v->{visibleMonth=visibleMonth.minusMonths(1);refresh();});
        next.setOnClickListener(v->{visibleMonth=visibleMonth.plusMonths(1);refresh();});
        upload.setOnClickListener(v->{Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);intent.setType("image/*");intent.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(intent,PICK);});
        csv.setOnClickListener(v->editImport(""));
        refresh();showDay(selected);
    }
    private void refresh(){title.setText(visibleMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy",Locale.US)));calendar.setMonth(visibleMonth);}
    private void showDay(LocalDate day){
        selected=day;details.removeAllViews();
        details.addView(text(day.format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy",Locale.US)),19,white));
        for(String note:repository.notes(day))details.addView(text("★ "+note,14,0xFFFFC857));
        var legs=repository.forDate(day);
        if(legs.isEmpty())details.addView(text("No flights loaded for this day. Upload your Crew Access screenshot to add or revise flights.",13,0xFF9FB3C8));
        for(FlightLeg leg:legs){
            details.addView(text(leg.flightNumber()+"  "+leg.origin()+" → "+leg.destination()+"  "+leg.seatPosition(),16,blue));
            details.addView(text(leg.departure().format(DateTimeFormatter.ofPattern("MMM d HH:mm z"))+" → "+leg.arrival().format(DateTimeFormatter.ofPattern("MMM d HH:mm z")),13,white));
            details.addView(text("Block "+leg.flightTime()+"  •  "+leg.source(),12,0xFF9FB3C8));
        }
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request!=PICK||result!=RESULT_OK||data==null)return;
        try(InputStream input=getContentResolver().openInputStream(data.getData())){
            BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;
            BitmapFactory.decodeStream(input,null,options);
            int sample=1;while(options.outWidth/sample>1800||options.outHeight/sample>1800)sample*=2;
            try(InputStream second=getContentResolver().openInputStream(data.getData())){
                BitmapFactory.Options small=new BitmapFactory.Options();small.inSampleSize=sample;
                Bitmap bmp=BitmapFactory.decodeStream(second,null,small);
                if(bmp==null)throw new Exception("Unable to decode image");
                var recognizer=TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
                recognizer.process(InputImage.fromBitmap(bmp,0)).addOnSuccessListener(resultText->{
                    editImport(resultText.getText());bmp.recycle();recognizer.close();
                }).addOnFailureListener(e->{bmp.recycle();recognizer.close();toast("Text recognition failed: "+e.getMessage());});
            }
        }catch(Exception e){toast("Image import failed: "+e.getMessage());}
    }
    private void editImport(String recognized){
        LinearLayout layout=new LinearLayout(this);layout.setOrientation(1);layout.setPadding(16,4,16,0);
        TextView hint=text("Review recognized text. Import flight rows as:\nYYYY-MM-DD,UPS52,ANC,HKG,2026-10-03T18:47,2026-10-04T04:44,FO\nTimes must be UTC. IRO=RO, DH=deadhead. Raw OCR may need editing.",13,0xFF222222);
        EditText editor=new EditText(this);editor.setText(recognized);editor.setMinLines(7);editor.setMaxLines(14);editor.setGravity(Gravity.TOP);editor.setTextSize(13);
        layout.addView(hint);layout.addView(editor);
        new android.app.AlertDialog.Builder(this).setTitle("Review schedule screenshot").setView(layout)
            .setNegativeButton("Cancel",null).setPositiveButton("Import", (d,w)->{
                int n=repository.importCsv(editor.getText().toString());
                toast(n+" flight legs imported");
                refresh();showDay(selected);
            }).show();
    }
    private void toast(String message){Toast.makeText(this,message,Toast.LENGTH_LONG).show();}
}
