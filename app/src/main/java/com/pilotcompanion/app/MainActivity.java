package com.pilotcompanion.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
    private static final String PREFS = "pilot_companion";
    private final ScheduleRepository repository = new ScheduleRepository();
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
        selectedDate = repository.nearestScheduledDate(LocalDate.now());
        visibleMonth = YearMonth.from(selectedDate);
        currentPeriod = PayPeriodCalculator.containing(selectedDate);
        setContentView(buildScreen());
        showMonth();
        showDay(selectedDate);
        syncFromCloud();
    }

    @Override protected void onResume() { super.onResume(); clockHandler.removeCallbacks(clockTick); clockHandler.post(clockTick); }
    @Override protected void onPause() { clockHandler.removeCallbacks(clockTick); super.onPause(); }

    private View buildScreen() {
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(12), dp(16), dp(12)); root.setBackgroundColor(0xFF071B2F);
        TextView appName = label("PILOT COMPANION", 13, 0xFF68D8FF); appName.setLetterSpacing(.18f); root.addView(appName);
        root.addView(label("Blue = traded   Yellow = company revised", 11, 0xFFB8C7D9));

        LinearLayout actions = new LinearLayout(this); actions.setGravity(Gravity.CENTER_VERTICAL);
        Button upload = smallButton("Upload schedule"); Button sync = smallButton("Sync now");
        syncStatus = label("Shared schedule: connecting…", 10, 0xFFB8C7D9);
        actions.addView(upload, new LinearLayout.LayoutParams(0, dp(44), 1));
        actions.addView(sync, new LinearLayout.LayoutParams(0, dp(44), 1)); root.addView(actions);
        root.addView(syncStatus);
        upload.setOnClickListener(v -> openSchedulePicker()); sync.setOnClickListener(v -> syncFromCloud());

        LinearLayout controls = new LinearLayout(this); controls.setGravity(Gravity.CENTER_VERTICAL);
        Button previous = button("<"); monthTitle = label("", 23, 0xFFFFFFFF); monthTitle.setGravity(Gravity.CENTER); Button next = button(">");
        controls.addView(previous, new LinearLayout.LayoutParams(dp(48), dp(48)));
        controls.addView(monthTitle, new LinearLayout.LayoutParams(0, dp(52), 1));
        controls.addView(next, new LinearLayout.LayoutParams(dp(48), dp(48))); root.addView(controls);
        payPeriodTitle = label("", 12, 0xFFFFD166); payPeriodTitle.setGravity(Gravity.CENTER); root.addView(payPeriodTitle);

        calendar = new MonthCalendarView(this, repository); calendar.setSelectedDate(selectedDate); calendar.setOnDateSelectedListener(this::showDay);
        root.addView(calendar, new LinearLayout.LayoutParams(-1, 0, 1));
        detailPanel = new LinearLayout(this); detailPanel.setOrientation(LinearLayout.VERTICAL);
        detailPanel.setPadding(dp(14), dp(8), dp(14), dp(8)); detailPanel.setBackgroundColor(0xFF102A43);
        root.addView(detailPanel, new LinearLayout.LayoutParams(-1, dp(215)));
        previous.setOnClickListener(v -> { currentPeriod = PayPeriodCalculator.shift(currentPeriod, -1); visibleMonth = YearMonth.from(currentPeriod.start()); showMonth(); });
        next.setOnClickListener(v -> { currentPeriod = PayPeriodCalculator.shift(currentPeriod, 1); visibleMonth = YearMonth.from(currentPeriod.start()); showMonth(); });
        return root;
    }

    private void showMonth() {
        monthTitle.setText(visibleMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US)));
        payPeriodTitle.setText("Pay period • " + currentPeriod.start().format(DateTimeFormatter.ofPattern("MMM d")) + " – " + currentPeriod.end().format(DateTimeFormatter.ofPattern("MMM d, yyyy")));
        calendar.setMonth(visibleMonth);
    }

    private void showDay(LocalDate date) {
        selectedDate = date; calendar.setSelectedDate(date); currentPeriod = PayPeriodCalculator.containing(date);
        payPeriodTitle.setText("Pay period • " + currentPeriod.start().format(DateTimeFormatter.ofPattern("MMM d")) + " – " + currentPeriod.end().format(DateTimeFormatter.ofPattern("MMM d, yyyy")));
        detailPanel.removeAllViews();
        detailPanel.addView(label(date.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.US)), 17, 0xFFFFFFFF));
        var legs = repository.forDate(date);
        if (legs.isEmpty()) { detailPanel.addView(label("No flying scheduled", 14, 0xFF9FB3C8)); return; }
        for (FlightLeg leg : legs) {
            int stateColor = leg.isRevised() ? 0xFFFFC857 : leg.isTraded() ? 0xFF4DA3FF : 0xFF68D8FF;
            String pairing = leg.pairing().isBlank() ? "" : "  " + leg.pairing();
            TextView flightRow = label(leg.flightNumber() + "  " + leg.origin() + " > " + leg.destination() + "   " + leg.seatPosition() + pairing + "  ›", 16, stateColor);
            flightRow.setPadding(0, dp(5), 0, dp(5)); flightRow.setOnClickListener(v -> showFlightDetails(leg)); detailPanel.addView(flightRow);
            detailPanel.addView(label(leg.changeLabel() + "  •  tap flight for details", 11, stateColor));
            detailPanel.addView(label(departureStatus(leg), 13, 0xFFE8F1F8));
            detailPanel.addView(label("Local " + leg.localTimes() + "  |  Block " + leg.flightTime() + "  |  Seat " + leg.seatPosition(), 12, 0xFFFFFFFF));
            detailPanel.addView(label("Zulu " + leg.zuluTimes(), 12, 0xFFB8C7D9));
            if (leg.hotel() != null) detailPanel.addView(label("Hotel: " + leg.hotel(), 12, 0xFFB8E986));
        }
    }

    private void showFlightDetails(FlightLeg leg) {
        DateTimeFormatter local = DateTimeFormatter.ofPattern("EEE MMM d, HH:mm z", Locale.US);
        String message = "Position: " + leg.seatPosition() + "\n" +
                "Status: " + leg.changeLabel() + "\n" +
                (leg.pairing().isBlank() ? "" : "Pairing: " + leg.pairing() + "\n") +
                "\nLOCAL TIMES\nDepart " + leg.origin() + ": " + leg.departure().format(local) + "\n" +
                "Arrive " + leg.destination() + ": " + leg.arrival().format(local) + "\n" +
                "\nZULU TIMES\nDepart: " + leg.departureZulu() + "\nArrive: " + leg.arrivalZulu() + "\n" +
                "\nFlight time: " + leg.flightTime() + "\n" +
                (leg.hotel() == null ? "" : "Hotel: " + leg.hotel() + "\n") +
                "Source: " + leg.source();
        new AlertDialog.Builder(this).setTitle(leg.flightNumber() + " • " + leg.origin() + " → " + leg.destination())
                .setMessage(message).setPositiveButton("Close", null).show();
    }

    private void openSchedulePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT); intent.addCategory(Intent.CATEGORY_OPENABLE); intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "text/plain", "application/json"});
        startActivityForResult(intent, IMPORT_REQUEST);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != IMPORT_REQUEST || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData(); String type = getContentResolver().getType(uri);
        try {
            if (type != null && type.startsWith("image/")) {
                InputImage image = InputImage.fromFilePath(this, uri);
                TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS).process(image)
                        .addOnSuccessListener(result -> chooseImportType(result.getText()))
                        .addOnFailureListener(error -> Toast.makeText(this, "Could not read schedule image: " + error.getMessage(), Toast.LENGTH_LONG).show());
            } else chooseImportType(readText(uri));
        } catch (Exception error) { Toast.makeText(this, "Could not open schedule: " + error.getMessage(), Toast.LENGTH_LONG).show(); }
    }

    private void chooseImportType(String text) {
        String pairing = detectPairing(text);
        String[] choices = {"Traded by me (blue)", "Company revised (yellow)", "Original schedule"};
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
        if (requestedType == FlightLeg.ChangeType.TRADED && Instant.now().isAfter(firstDeparture)) {
            effective = FlightLeg.ChangeType.REVISED;
            legs = ScheduleImportParser.parseCrewAccessText(text, effective, pairing);
        }
        repository.mergeImported(legs);
        if (persist) getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString("import_text", text).putString("import_type", effective.name()).putString("import_pairing", pairing).apply();
        selectedDate = repository.nearestScheduledDate(LocalDate.now()); visibleMonth = YearMonth.from(selectedDate); currentPeriod = PayPeriodCalculator.containing(selectedDate);
        showMonth(); showDay(selectedDate); Toast.makeText(this, "Schedule imported: " + legs.size() + " flights", Toast.LENGTH_SHORT).show();
    }

    private void restoreLastImport() {
        String text = getSharedPreferences(PREFS, MODE_PRIVATE).getString("import_text", "");
        if (text.isBlank()) return;
        try {
            FlightLeg.ChangeType type = FlightLeg.ChangeType.valueOf(getSharedPreferences(PREFS, MODE_PRIVATE).getString("import_type", "ORIGINAL"));
            String pairing = getSharedPreferences(PREFS, MODE_PRIVATE).getString("import_pairing", "");
            repository.mergeImported(ScheduleImportParser.parseCrewAccessText(text, type, pairing));
        } catch (RuntimeException ignored) { }
    }

    private void syncFromCloud() {
        if (syncStatus != null) syncStatus.setText("Shared schedule: syncing…");
        new Thread(() -> {
            try {
                List<FlightLeg> legs = ScheduleImportParser.parseSharedFormat(CloudScheduleSync.download());
                runOnUiThread(() -> { repository.mergeImported(legs); if (syncStatus != null) syncStatus.setText("Shared schedule: synced • " + legs.size() + " flights"); showDay(selectedDate); });
            } catch (Exception error) {
                runOnUiThread(() -> { if (syncStatus != null) syncStatus.setText("Shared schedule: offline • using saved schedule"); });
            }
        }).start();
    }

    private String detectPairing(String text) {
        Matcher m = Pattern.compile("\\bA\\d{5,6}R?\\b", Pattern.CASE_INSENSITIVE).matcher(text);
        return m.find() ? m.group().toUpperCase(Locale.US) : "";
    }

    private String readText(Uri uri) throws Exception {
        StringBuilder out = new StringBuilder();
        try (InputStream in = getContentResolver().openInputStream(uri); BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line; while ((line = reader.readLine()) != null) out.append(line).append('\n');
        }
        return out.toString();
    }

    private String departureStatus(FlightLeg leg) {
        Instant now = Instant.now();
        if (now.isBefore(leg.departure().toInstant())) return "Departs in " + compactDuration(Duration.between(now, leg.departure().toInstant()));
        if (now.isBefore(leg.arrival().toInstant())) return "In progress - arrives in " + compactDuration(Duration.between(now, leg.arrival().toInstant()));
        return "Departed " + compactDuration(Duration.between(leg.departure().toInstant(), now)) + " ago";
    }
    private String compactDuration(Duration duration) {
        long days = duration.toDays(), hours = duration.minusDays(days).toHours(), minutes = duration.minusDays(days).minusHours(hours).toMinutes();
        return days > 0 ? days + "d " + hours + "h " + minutes + "m" : hours + "h " + minutes + "m";
    }
    private TextView label(String text, int sp, int color) { TextView view = new TextView(this); view.setText(text); view.setTextSize(sp); view.setTextColor(color); view.setGravity(Gravity.CENTER_VERTICAL); return view; }
    private Button button(String text) { Button button = new Button(this); button.setText(text); button.setTextSize(24); button.setTextColor(0xFFFFFFFF); button.setBackgroundColor(0x00000000); return button; }
    private Button smallButton(String text) { Button button = new Button(this); button.setText(text); button.setTextSize(12); return button; }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
