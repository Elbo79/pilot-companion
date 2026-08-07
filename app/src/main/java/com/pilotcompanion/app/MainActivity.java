package com.pilotcompanion.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        visibleMonth = YearMonth.from(repository.firstScheduledDate());
        setContentView(buildScreen());
        showMonth();
        showDay(repository.firstScheduledDate());
    }

    private View buildScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(12), dp(16), dp(12));
        root.setBackgroundColor(0xFF071B2F);

        TextView appName = label("PILOT COMPANION", 13, 0xFF68D8FF);
        appName.setLetterSpacing(.18f);
        root.addView(appName);

        LinearLayout controls = new LinearLayout(this);
        controls.setGravity(Gravity.CENTER_VERTICAL);
        Button previous = button("<");
        monthTitle = label("", 25, 0xFFFFFFFF);
        monthTitle.setGravity(Gravity.CENTER);
        Button next = button(">");
        controls.addView(previous, new LinearLayout.LayoutParams(dp(48), dp(48)));
        controls.addView(monthTitle, new LinearLayout.LayoutParams(0, dp(58), 1));
        controls.addView(next, new LinearLayout.LayoutParams(dp(48), dp(48)));
        root.addView(controls);

        calendar = new MonthCalendarView(this, repository);
        calendar.setOnDateSelectedListener(this::showDay);
        root.addView(calendar, new LinearLayout.LayoutParams(-1, 0, 1));

        detailPanel = new LinearLayout(this);
        detailPanel.setOrientation(LinearLayout.VERTICAL);
        detailPanel.setPadding(dp(14), dp(8), dp(14), dp(8));
        detailPanel.setBackgroundColor(0xFF102A43);
        root.addView(detailPanel, new LinearLayout.LayoutParams(-1, dp(142)));

        previous.setOnClickListener(v -> { visibleMonth = visibleMonth.minusMonths(1); showMonth(); });
        next.setOnClickListener(v -> { visibleMonth = visibleMonth.plusMonths(1); showMonth(); });
        return root;
    }

    private void showMonth() {
        monthTitle.setText(visibleMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US)));
        calendar.setMonth(visibleMonth);
    }

    private void showDay(LocalDate date) {
        detailPanel.removeAllViews();
        detailPanel.addView(label(date.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.US)), 17, 0xFFFFFFFF));
        var legs = repository.forDate(date);
        if (legs.isEmpty()) {
            detailPanel.addView(label("No flying scheduled", 14, 0xFF9FB3C8));
            return;
        }
        for (FlightLeg leg : legs) {
            detailPanel.addView(label(leg.flightNumber() + "  " + leg.origin() + " > "
                    + leg.destination() + "   " + leg.seatPosition(), 16, 0xFF68D8FF));
            detailPanel.addView(label("Local  " + leg.localTimes() + "     Flight  "
                    + leg.flightTime(), 14, 0xFFFFFFFF));
            String source = leg.isRevised()
                    ? "REVISED - previously " + leg.scheduledLocalTimes()
                    : leg.source();
            detailPanel.addView(label(source, 11, leg.isRevised() ? 0xFFFFC857 : 0xFF9FB3C8));
        }
    }

    private TextView label(String text, int sp, int color) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setGravity(Gravity.CENTER_VERTICAL);
        return view;
    }

    private Button button(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(24);
        button.setTextColor(0xFFFFFFFF);
        button.setBackgroundColor(0x00000000);
        return button;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
