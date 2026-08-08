package com.pilotcompanion.app;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class MainActivity extends Activity {
    private final ScheduleRepository repository = new ScheduleRepository();
    private YearMonth visibleMonth;
    private TextView monthTitle;
    private MonthCalendarView calendar;
    private LinearLayout detailPanel;
    private LocalDate selectedDate;
    private final Handler clockHandler = new Handler(Looper.getMainLooper());
    private final Runnable clockTick = new Runnable() {
        @Override public void run() { if (selectedDate != null) showDay(selectedDate); clockHandler.postDelayed(this, 60_000); }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        visibleMonth = YearMonth.from(repository.firstScheduledDate());
        setContentView(buildScreen()); showMonth(); showDay(repository.firstScheduledDate());
    }
    @Override protected void onResume() { super.onResume(); clockHandler.removeCallbacks(clockTick); clockHandler.post(clockTick); }
    @Override protected void onPause() { clockHandler.removeCallbacks(clockTick); super.onPause(); }

    private View buildScreen() {
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(12), dp(16), dp(12)); root.setBackgroundColor(0xFF071B2F);
        TextView appName = label("PILOT COMPANION", 13, 0xFF68D8FF); appName.setLetterSpacing(.18f); root.addView(appName);
        root.addView(label("Blue = traded   Yellow = company revised", 11, 0xFFB8C7D9));

        LinearLayout controls = new LinearLayout(this); controls.setGravity(Gravity.CENTER_VERTICAL);
        Button previous = button("<"); monthTitle = label("", 25, 0xFFFFFFFF); monthTitle.setGravity(Gravity.CENTER); Button next = button(">");
        controls.addView(previous, new LinearLayout.LayoutParams(dp(48), dp(48)));
        controls.addView(monthTitle, new LinearLayout.LayoutParams(0, dp(58), 1));
        controls.addView(next, new LinearLayout.LayoutParams(dp(48), dp(48))); root.addView(controls);

        calendar = new MonthCalendarView(this, repository); calendar.setOnDateSelectedListener(this::showDay);
        root.addView(calendar, new LinearLayout.LayoutParams(-1, 0, 1));
        detailPanel = new LinearLayout(this); detailPanel.setOrientation(LinearLayout.VERTICAL);
        detailPanel.setPadding(dp(14), dp(8), dp(14), dp(8)); detailPanel.setBackgroundColor(0xFF102A43);
        root.addView(detailPanel, new LinearLayout.LayoutParams(-1, dp(175)));
        previous.setOnClickListener(v -> { visibleMonth = visibleMonth.minusMonths(1); showMonth(); });
        next.setOnClickListener(v -> { visibleMonth = visibleMonth.plusMonths(1); showMonth(); }); return root;
    }

    private void showMonth() { monthTitle.setText(visibleMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US))); calendar.setMonth(visibleMonth); }

    private void showDay(LocalDate date) {
        selectedDate = date; detailPanel.removeAllViews();
        detailPanel.addView(label(date.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.US)), 17, 0xFFFFFFFF));
        var legs = repository.forDate(date);
        if (legs.isEmpty()) { detailPanel.addView(label("No flying scheduled", 14, 0xFF9FB3C8)); return; }
        for (FlightLeg leg : legs) {
            int stateColor = leg.isRevised() ? 0xFFFFC857 : leg.isTraded() ? 0xFF4DA3FF : 0xFF68D8FF;
            String pairing = leg.pairing().isBlank() ? "" : "  " + leg.pairing();
            detailPanel.addView(label(leg.flightNumber() + "  " + leg.origin() + " > " + leg.destination() + "   " + leg.seatPosition() + pairing, 16, stateColor));
            detailPanel.addView(label(leg.changeLabel(), 12, stateColor));
            detailPanel.addView(label(departureStatus(leg), 14, 0xFFE8F1F8));
            detailPanel.addView(label("Local " + leg.localTimes() + "  |  Block " + leg.flightTime() + "  |  Seat " + leg.seatPosition(), 13, 0xFFFFFFFF));
            if (leg.hotel() != null) detailPanel.addView(label("Hotel: " + leg.hotel(), 13, 0xFFB8E986));
            detailPanel.addView(label(revisionDescription(leg), 11, stateColor));
        }
    }

    private String revisionDescription(FlightLeg leg) {
        if (!leg.isRevised()) return leg.source();
        if ("Unacknowledged Roster Changes".equals(leg.source())) {
            return "Company revision after trip start • Crew Access roster change";
        }
        if (leg.scheduledDeparture().toInstant().equals(leg.departure().toInstant())
                && leg.scheduledArrival().toInstant().equals(leg.arrival().toInstant())) {
            return "Company revised • " + leg.source();
        }
        return "Company revised - previously " + leg.scheduledLocalTimes();
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
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
