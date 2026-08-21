package com.pilotcompanion.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MainActivity extends Activity {
    private static final int IMPORT_REQUEST = 1001;
    private static final int NOTIFICATION_REQUEST = 1002;
    private static final String PREFS = "pilot_companion";
    private final ScheduleRepository repository = new ScheduleRepository();
    private final ImportantDateRepository importantDates = new ImportantDateRepository();
    private YearMonth visibleMonth;
    private TextView monthTitle;
    private TextView payPeriodTitle;
    private TextView syncStatus;
    private MonthCalendarView calendar;
    private LinearLayout detailPanel;
    private LocalDate selectedDate;
    private PayPeriodCalculator.Period currentPeriod;
    private final Handler clockHandler = new Handler(Looper.getMainLooper());
    private final Runnable clockTick = new Runnable() {
        @Override public void run() { if (selectedDate != null) showDay(selectedDate); clockHandler.postDelayed(this, 60_000); }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        restoreLastImport();
        selectedDate = LocalDate.now();
        currentPeriod = PayPeriodCalculator.containing(selectedDate);
        visibleMonth = YearMonth.from(currentPeriod.end());
        setContentView(buildScreen());
        showMonth();
        showDay(selectedDate);
        requestNotificationPermission();
        ReminderScheduler.scheduleAll(this, importantDates);
        syncFromCloud();
    }

    @Override protected void onResume() { super.onResume(); clockHandler.removeCallbacks(clockTick); clockHandler.post(clockTick); }
    @Override protected void onPause() { clockHandler.removeCallbacks(clockTick); super.onPause(); }

    private View buildScreen() {
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        // Narrower side padding gives each of the seven calendar columns more usable room.
        root.setPadding(dp(6), dp(8), dp(6), dp(8)); root.setBackgroundColor(0xFF071B2F);
        TextView appName = label("PILOT COMPANION", 13, 0xFF68D8FF); appName.setLetterSpacing(.18f); root.addView(appName);
        root.addView(label("Blue = traded   Yellow = company revised", 11, 0xFFB8C7D9));
        root.addView(label("ANC FO dates: bids • vacation • pay periods • paydays • reminders", 10, 0xFFFFD166));

        LinearLayout actions = new LinearLayout(this); actions.setGravity(Gravity.CENTER_VERTICAL);
        Button upload = smallButton("Upload schedule"); Button sync = smallButton("Sync now");
        syncStatus = label("Shared schedule: connecting…", 10, 0xFFB8C7D9);
        actions.addView(upload, new LinearLayout.LayoutParams(0, dp(40), 1));
        actions.addView(sync, new LinearLayout.LayoutParams(0, dp(40), 1)); root.addView(actions); root.addView(syncStatus);
        upload.setOnClickListener(v -> openSchedulePicker()); sync.setOnClickListener(v -> syncFromCloud());

        LinearLayout controls = new LinearLayout(this); controls.setGravity(Gravity.CENTER_VERTICAL);
        Button previous = button("<"); monthTitle = label("", 22, 0xFFB9DFFF); monthTitle.setGravity(Gravity.CENTER); Button next = button(">");
        controls.addView(previous, new LinearLayout.LayoutParams(dp(44), dp(44)));
        controls.addView(monthTitle, new LinearLayout.LayoutParams(0, dp(46), 1));
        controls.addView(next, new LinearLayout.LayoutParams(dp(44), dp(44))); root.addView(controls);
        payPeriodTitle = label("", 12, 0xFFFFD166); payPeriodTitle.setGravity(Gravity.CENTER); root.addView(payPeriodTitle);

        calendar = new MonthCalendarView(this, repository, importantDates);
        calendar.setPeriod(currentPeriod);
        calendar.setSelectedDate(selectedDate);
        calendar.setOnDateSelectedListener(this::showDay);
        root.addView(calendar, new LinearLayout.LayoutParams(-1, 0, 1));

        detailPanel = new LinearLayout(this); detailPanel.setOrientation(LinearLayout.VERTICAL);
        detailPanel.setPadding(dp(10), dp(6), dp(10), dp(6)); detailPanel.setBackgroundColor(0xFF102A43);
        // Slightly smaller detail panel gives the four calendar rows more vertical room.
        root.addView(detailPanel, new LinearLayout.LayoutParams(-1, dp(205)));

        previous.setOnClickListener(v -> {
            currentPeriod = PayPeriodCalculator.shift(currentPeriod, -1);
            visibleMonth = YearMonth.from(currentPeriod.end());
            showMonth();
        });
        next.setOnClickListener(v -> {
            currentPeriod = PayPeriodCalculator.shift(currentPeriod, 1);
            visibleMonth = YearMonth.from(currentPeriod.end());
            showMonth();
        });
        return root;
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_REQUEST);
        }
    }

    private void showMonth() {
        String startMonth = currentPeriod.start().format(DateTimeFormatter.ofPattern("MMMM", Locale.US));
        String endMonth = currentPeriod.end().format(DateTimeFormatter.ofPattern("MMMM", Locale.US));
        String monthSpan = startMonth.equals(endMonth) ? startMonth : startMonth + "–" + endMonth;
        monthTitle.setText(monthSpan + " • PP" + currentPeriod.index());
        payPeriodTitle.setText("UPS Pay Period • " + currentPeriod.start().format(DateTimeFormatter.ofPattern("MMM d")) + " – " + currentPeriod.end().format(DateTimeFormatter.ofPattern("MMM d, yyyy")));
        calendar.setPeriod(currentPeriod);
    }

    private void showDay(LocalDate date) {
        selectedDate = date;
        calendar.setSelectedDate(date);
        detailPanel.removeAllViews();
        detailPanel.addView(label(date.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.US)), 17, 0xFFFFFFFF));
        var legs = repository.forDate(date);
        if (legs.isEmpty()) { detailPanel.addView(label("No flying scheduled", 14, 0xFF9FB3C8)); return; }
        for (FlightLeg leg : legs) {
            int stateColor = leg.isRevised() ? 0xFFFFC857 : leg.isTraded() ? 0xFF4DA3FF : 0xFF68D8FF;
            String pairing = leg.pairing().isBlank() ? "" : "  " + leg.pairing();
            TextView flightRow = label(leg.flightNumber() + "  " + leg.origin() + " > " + leg.destination() + "   " + leg.seatPosition() + pairing + "  ›", 16, stateColor);
            flightRow.setPadding(0, dp(4), 0, dp(4));
            flightRow.setOnClickListener(v -> showFlightDetails(leg));
            detailPanel.addView(flightRow);
            detailPanel.addView(label(leg.changeLabel() + "  •  tap flight for details", 11, stateColor));
            detailPanel.addView(label(departureStatus(leg), 13, 0xFFE8F1F8));

            TextView localRow = label("LOCAL  " + leg.localTimes() + "  |  Block " + leg.flightTime() + "  |  Seat " + leg.seatPosition(), 13, 0xFFFFFFFF);
            localRow.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            detailPanel.addView(localRow);

            detailPanel.addView(label("Zulu " + leg.zuluTimes(), 12, 0xFFB8C7D9));
            detailPanel.addView(label("California " + leg.californiaTimes(), 12, 0xFF9AD5FF));
            if (leg.hotel() != null) detailPanel.addView(label("Hotel: " + leg.hotel(), 12, 0xFFB8E986));
        }
    }

    private void showFlightDetails(FlightLeg leg) {
        DateTimeFormatter local = DateTimeFormatter.ofPattern("EEE MMM d, HH:mm z", Locale.US);
        String message = "Position: " + leg.seatPosition() + "\nStatus: " + leg.changeLabel() + "\n" +
                (leg.pairing().isBlank() ? "" : "Pairing: " + leg.pairing() + "\n") +
                "\nLOCAL TIMES\nDepart " + leg.origin() + ": " + leg.departure().format(local) + "\nArrive " + leg.destination() + ": " + leg.arrival().format(local) + "\n" +
                "\nZULU TIMES\nDepart: " + leg.departureZulu() + "\nArrive: " + leg.arrivalZulu() + "\n" +
                "\nCALIFORNIA TIME\nDepart: " + leg.departureCalifornia() + "\nArrive: " + leg.arrivalCalifornia() + "\n" +
                "\nFlight time: " + leg.flightTime() + "\n" + (leg.hotel() == null ? "" : "Hotel: " + leg.hotel() + "\n") + "Source: " + leg.source();
        new AlertDialog.Builder(this).setTitle(leg.flightNumber() + " • " + leg.origin() + " → " + leg.destination()).setMessage(message).setPositiveButton("Close", null).show();
    }

    private void openSchedulePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT); intent.addCategory(Intent.CATEGORY_OPENABLE); intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "text/plain", "application/json"}); startActivityForResult(intent, IMPORT_REQUEST);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != IMPORT_REQUEST || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData(); String type = getContentResolver().getType(uri);
        try {
            if (type != null && type.startsWith("image/")) {
                InputImage image = InputImage.fromFilePath(this, uri);
                TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS).process(image).addOnSuccessListener(result -> chooseImportType(result.getText()))
                        .addOnFailureListener(error -> Toast.makeText(this, "Could not read schedule image: " + error.getMessage(), Toast.LENGTH_LONG).show());
            } else chooseImportType(readText(uri));
        } catch (Exception error) { Toast.makeText(this, "Could not open schedule: " + error.getMessage(), Toast.LENGTH_LONG).show(); }
    }

    private void chooseImportType(String text) {
        String pairing = detectPairing(text); String[] choices = {"Traded by me (blue)", "Company revised (yellow)", "Original schedule"};
        new AlertDialog.Builder(this).setTitle("How did this schedule change?").setItems(choices, (dialog, which) -> {
            FlightLeg.ChangeType type = which == 0 ? FlightLeg.ChangeType.TRADED : which == 1 ? FlightLeg.ChangeType.REVISED : FlightLeg.ChangeType.ORIGINAL;
            importText(text, type, pairing, true);
        }).show();
    }

    private void importText(String text, FlightLeg.ChangeType requestedType, String pairing, boolean persist) {
        List<FlightLeg> legs = ScheduleImportParser.parseCrewAccessText(text, requestedType, pairing);
        if (legs.isEmpty()) { Toast.makeText(this, "I could not find Crew Access flight cards in that file yet.", Toast.LENGTH_LONG).show(); return; }
        Instant firstDeparture = legs.stream().map(l -> l.departure().toInstant()).min(Instant::compareTo).orElse(Instant.MAX);
        FlightLeg.ChangeType effective = requestedType;
        if (requestedType == FlightLeg.ChangeType.TRADED && Instant.now().isAfter(firstDeparture)) { effective = FlightLeg.ChangeType.REVISED; legs = ScheduleImportParser.parseCrewAccessText(text, effective, pairing); }
        repository.mergeImported(legs);
        if (persist) getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString("import_text", text).putString("import_type", effective.name()).putString("import_pairing", pairing).apply();
        selectedDate = LocalDate.now();
        currentPeriod = PayPeriodCalculator.containing(selectedDate);
        visibleMonth = YearMonth.from(currentPeriod.end());
        showMonth(); showDay(selectedDate); Toast.makeText(this, "Schedule imported: " + legs.size() + " flights", Toast.LENGTH_SHORT).show();
    }

    private void restoreLastImport() {
        String text = getSharedPreferences(PREFS, MODE_PRIVATE).getString("import_text", ""); if (text.isBlank()) return;
        try {
            FlightLeg.ChangeType type = FlightLeg.ChangeType.valueOf(getSharedPreferences(PREFS, MODE_PRIVATE).getString("import_type", "ORIGINAL"));
            String pairing = getSharedPreferences(PREFS, MODE_PRIVATE).getString("import_pairing", ""); repository.mergeImported(ScheduleImportParser.parseCrewAccessText(text, type, pairing));
        } catch (RuntimeException ignored) { }
    }

    private void syncFromCloud() {
        if (syncStatus != null) syncStatus.setText("Shared schedule: syncing…");
        new Thread(() -> {
            try {
                List<FlightLeg> legs = ScheduleImportParser.parseSharedFormat(CloudScheduleSync.download());
                runOnUiThread(() -> { repository.mergeImported(legs); if (syncStatus != null) syncStatus.setText("Shared schedule: synced • " + legs.size() + " flights"); showDay(selectedDate); });
            } catch (Exception error) { runOnUiThread(() -> { if (syncStatus != null) syncStatus.setText("Shared schedule: offline • using saved schedule"); }); }
        }).start();
    }

    private String detectPairing(String text) { Matcher m = Pattern.compile("\\bA\\d{5,6}R?\\b", Pattern.CASE_INSENSITIVE).matcher(text); return m.find() ? m.group().toUpperCase(Locale.US) : ""; }
    private String readText(Uri uri) throws Exception { StringBuilder out = new StringBuilder(); try (InputStream in = getContentResolver().openInputStream(uri); BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) { String line; while ((line = reader.readLine()) != null) out.append(line).append('\n'); } return out.toString(); }
    private String departureStatus(FlightLeg leg) { Instant now = Instant.now(); if (now.isBefore(leg.departure().toInstant())) return "Departs in " + compactDuration(Duration.between(now, leg.departure().toInstant())); if (now.isBefore(leg.arrival().toInstant())) return "In progress - arrives in " + compactDuration(Duration.between(now, leg.arrival().toInstant())); return "Departed " + compactDuration(Duration.between(leg.departure().toInstant(), now)) + " ago"; }
    private String compactDuration(Duration duration) { long days = duration.toDays(), hours = duration.minusDays(days).toHours(), minutes = duration.minusDays(days).minusHours(hours).toMinutes(); return days > 0 ? days + "d " + hours + "h " + minutes + "m" : hours + "h " + minutes + "m"; }
    private TextView label(String text, int sp, int color) { TextView view = new TextView(this); view.setText(text); view.setTextSize(sp); view.setTextColor(color); view.setGravity(Gravity.CENTER_VERTICAL); return view; }
    private Button button(String text) { Button button = new Button(this); button.setText(text); button.setTextSize(24); button.setTextColor(0xFFFFFFFF); button.setBackgroundColor(0x00000000); return button; }
    private Button smallButton(String text) { Button button = new Button(this); button.setText(text); button.setTextSize(12); return button; }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
