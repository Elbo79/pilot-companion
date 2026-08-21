package com.pilotcompanion.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
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
        paint.setTypeface(Typeface.create("sans", Typeface.NORMAL));
    }

    void setMonth(YearMonth month) {
        this.period = PayPeriodCalculator.containing(month.atEndOfMonth());
        invalidate();
    }

    void setPeriod(PayPeriodCalculator.Period period) {
        this.period = period;
        invalidate();
    }

    void setSelectedDate(LocalDate date) { this.selected = date; invalidate(); }
    void setOnDateSelectedListener(Consumer<LocalDate> listener) { this.listener = listener; }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float cellWidth = getWidth() / 7f;
        float header = dp(28);
        float cellHeight = (getHeight() - header) / 4f;

        paint.setTypeface(Typeface.create("sans", Typeface.NORMAL));
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(dp(11));
        paint.setColor(0xFF9FB3C8);
        for (int i = 0; i < 7; i++) {
            DayOfWeek day = DayOfWeek.of(((i + 6) % 7) + 1);
            canvas.drawText(day.getDisplayName(TextStyle.SHORT, Locale.US).toUpperCase(Locale.US),
                    cellWidth * (i + .5f), dp(18), paint);
        }

        for (int index = 0; index < 28; index++) {
            int row = index / 7;
            int column = index % 7;
            float left = column * cellWidth;
            float top = header + row * cellHeight;
            float right = left + cellWidth;
            float bottom = top + cellHeight;
            LocalDate date = period.start().plusDays(index);

            paint.setStyle(Paint.Style.FILL);
            paint.setColor((date.getMonthValue() % 2 == 0) ? 0xFF0C2942 : 0xFF102F4B);
            canvas.drawRect(left + dp(1), top + dp(1), right - dp(1), bottom - dp(1), paint);

            if (date.equals(LocalDate.now())) {
                paint.setColor(0xFF1F5A78);
                canvas.drawRoundRect(left + dp(2), top + dp(2), right - dp(2), bottom - dp(2), dp(9), dp(9), paint);
            } else if (date.equals(selected)) {
                paint.setColor(0xFF173B56);
                canvas.drawRoundRect(left + dp(2), top + dp(2), right - dp(2), bottom - dp(2), dp(9), dp(9), paint);
            }

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(1));
            paint.setColor(0xFF365A78);
            canvas.drawRect(left, top, right, bottom, paint);

            // Hard-clip all date/schedule/event text to this cell so nothing can bleed into a neighboring day.
            int save = canvas.save();
            canvas.clipRect(left + dp(2), top + dp(2), right - dp(2), bottom - dp(2));

            paint.setStyle(Paint.Style.FILL);
            paint.setTextAlign(Paint.Align.LEFT);
            paint.setTypeface(Typeface.create("sans", Typeface.BOLD));
            paint.setTextSize(dp(11.5f));
            boolean monthStart = index == 0 || date.getDayOfMonth() == 1;
            String dateLabel = monthStart
                    ? date.getMonth().getDisplayName(TextStyle.SHORT, Locale.US).toUpperCase(Locale.US) + " " + date.getDayOfMonth()
                    : String.valueOf(date.getDayOfMonth());
            paint.setColor(date.equals(LocalDate.now()) ? 0xFF7FE1FF : 0xFFB9DFFF);
            canvas.drawText(dateLabel, left + dp(5), top + dp(17), paint);

            float y = top + dp(33);
            var legs = repository.forDate(date);
            if (!legs.isEmpty()) {
                FlightLeg leg = legs.get(0);
                paint.setTypeface(Typeface.create("sans", Typeface.NORMAL));
                paint.setTextSize(dp(8.1f));
                int stateColor = leg.isRevised() ? 0xFFFFC857 : leg.isTraded() ? 0xFF4DA3FF : 0xFF68D8FF;
                paint.setColor(stateColor);
                canvas.drawText(leg.origin() + ">" + leg.destination(), left + dp(4), y, paint); y += dp(11);

                paint.setTypeface(Typeface.create("sans", Typeface.BOLD));
                paint.setColor(Color.WHITE);
                canvas.drawText(leg.localTimes(), left + dp(4), y, paint); y += dp(11);

                paint.setTypeface(Typeface.create("sans", Typeface.NORMAL));
                paint.setColor(stateColor);
                canvas.drawText(leg.seatPosition() + " " + leg.changeLabel(), left + dp(4), y, paint); y += dp(11);
            }

            List<ImportantDate> events = importantDates.forDate(date);
            for (int i = 0; i < Math.min(events.size(), 2); i++) {
                ImportantDate event = events.get(i);
                paint.setTypeface(Typeface.create("sans", Typeface.NORMAL));
                paint.setTextSize(dp(7.1f));
                paint.setColor(eventColor(event.type()));
                canvas.drawText(shortLabel(event.calendarLabel(), 16), left + dp(4), y, paint); y += dp(9);
            }
            if (events.size() > 2) {
                paint.setTextSize(dp(7.1f));
                paint.setColor(0xFFB8C7D9);
                canvas.drawText("+" + (events.size() - 2) + " dates", left + dp(4), y, paint);
            }

            canvas.restoreToCount(save);
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
        float header = dp(28);
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
