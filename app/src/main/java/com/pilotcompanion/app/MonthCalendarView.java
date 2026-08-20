package com.pilotcompanion.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

final class MonthCalendarView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final ScheduleRepository repository;
    private final ImportantDateRepository importantDates;
    private PayPeriodCalculator.Period period = PayPeriodCalculator.containing(LocalDate.now());
    private LocalDate selected;
    private Consumer<LocalDate> listener = ignored -> { };

    MonthCalendarView(Context context, ScheduleRepository repository, ImportantDateRepository importantDates) {
        super(context);
        this.repository = repository;
        this.importantDates = importantDates;
        selected = LocalDate.now();
        paint.setTypeface(android.graphics.Typeface.create("sans", android.graphics.Typeface.NORMAL));
    }

    // Kept for compatibility with MainActivity navigation. The supplied month identifies the pay period
    // whose end falls in that month, so the view always renders exactly the official 28 days.
    void setMonth(YearMonth month) {
        this.period = PayPeriodCalculator.containing(month.atEndOfMonth());
        invalidate();
    }
    void setSelectedDate(LocalDate date) { this.selected = date; invalidate(); }
    void setOnDateSelectedListener(Consumer<LocalDate> listener) { this.listener = listener; }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float cellWidth = getWidth() / 7f;
        float header = dp(30);
        float cellHeight = (getHeight() - header) / 4f;

        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(dp(11));
        paint.setColor(0xFF9FB3C8);
        for (int i = 0; i < 7; i++) {
            DayOfWeek day = DayOfWeek.of(((i + 6) % 7) + 1);
            canvas.drawText(day.getDisplayName(TextStyle.SHORT, Locale.US).toUpperCase(Locale.US),
                    cellWidth * (i + .5f), dp(19), paint);
        }

        for (int index = 0; index < 28; index++) {
            int row = index / 7;
            int column = index % 7;
            float left = column * cellWidth;
            float top = header + row * cellHeight;
            LocalDate date = period.start().plusDays(index);

            if (date.equals(LocalDate.now())) {
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(0xFF1F5A78);
                canvas.drawRoundRect(left + dp(2), top + dp(2), left + cellWidth - dp(2),
                        top + cellHeight - dp(2), dp(9), dp(9), paint);
            } else if (date.equals(selected)) {
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(0xFF173B56);
                canvas.drawRoundRect(left + dp(2), top + dp(2), left + cellWidth - dp(2),
                        top + cellHeight - dp(2), dp(9), dp(9), paint);
            }

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(1));
            paint.setColor(0xFF27445F);
            canvas.drawRect(left, top, left + cellWidth, top + cellHeight, paint);

            paint.setStyle(Paint.Style.FILL);
            paint.setTextAlign(Paint.Align.LEFT);
            paint.setTextSize(dp(13));
            paint.setColor(date.equals(LocalDate.now()) ? 0xFF68D8FF : Color.WHITE);
            canvas.drawText(String.valueOf(date.getDayOfMonth()), left + dp(7), top + dp(19), paint);

            float y = top + dp(35);
            var legs = repository.forDate(date);
            if (!legs.isEmpty()) {
                FlightLeg leg = legs.get(0);
                paint.setTextSize(dp(8.5f));
                int stateColor = leg.isRevised() ? 0xFFFFC857 : leg.isTraded() ? 0xFF4DA3FF : 0xFF68D8FF;
                paint.setColor(stateColor);
                canvas.drawText(leg.origin() + ">" + leg.destination(), left + dp(5), y, paint); y += dp(12);
                paint.setColor(0xFFE8F1F8);
                canvas.drawText(leg.localTimes(), left + dp(5), y, paint); y += dp(12);
                paint.setColor(stateColor);
                canvas.drawText(leg.seatPosition() + " " + leg.changeLabel(), left + dp(5), y, paint); y += dp(12);
            }

            List<ImportantDate> events = importantDates.forDate(date);
            for (int i = 0; i < Math.min(events.size(), 2); i++) {
                ImportantDate event = events.get(i);
                paint.setTextSize(dp(7.5f));
                paint.setColor(eventColor(event.type()));
                canvas.drawText(shortLabel(event.calendarLabel(), 18), left + dp(5), y, paint); y += dp(10);
            }
            if (events.size() > 2) {
                paint.setTextSize(dp(7.5f));
                paint.setColor(0xFFB8C7D9);
                canvas.drawText("+" + (events.size() - 2) + " dates", left + dp(5), y, paint);
            }
        }
    }

    private int eventColor(ImportantDate.Type type) {
        return switch (type) {
            case VACATION -> 0xFFFFD166;
            case BID -> 0xFF9AD5FF;
            case PAY_PERIOD -> 0xFFB8E986;
            case PAYDAY -> 0xFFC9A7FF;
            case REMINDER -> 0xFFFF9F80;
        };
    }

    private String shortLabel(String text, int max) { return text.length() <= max ? text : text.substring(0, max - 1) + "…"; }

    @Override public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_UP) return true;
        float header = dp(30);
        if (event.getY() < header) return true;
        int column = Math.min(6, (int) (event.getX() / (getWidth() / 7f)));
        int row = Math.min(3, (int) ((event.getY() - header) / ((getHeight() - header) / 4f)));
        int index = row * 7 + column;
        if (index >= 0 && index < 28) {
            selected = period.start().plusDays(index);
            listener.accept(selected);
            invalidate();
            performClick();
        }
        return true;
    }

    @Override public boolean performClick() { super.performClick(); return true; }
    private float dp(float value) { return value * getResources().getDisplayMetrics().density; }
}
