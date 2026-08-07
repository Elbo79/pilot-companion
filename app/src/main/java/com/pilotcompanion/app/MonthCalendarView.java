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
import java.util.Locale;
import java.util.function.Consumer;

final class MonthCalendarView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final ScheduleRepository repository;
    private YearMonth month = YearMonth.now();
    private LocalDate selected;
    private Consumer<LocalDate> listener = ignored -> { };

    MonthCalendarView(Context context, ScheduleRepository repository) {
        super(context);
        this.repository = repository;
        selected = repository.firstScheduledDate();
        paint.setTypeface(android.graphics.Typeface.create("sans", android.graphics.Typeface.NORMAL));
    }

    void setMonth(YearMonth month) { this.month = month; invalidate(); }
    void setOnDateSelectedListener(Consumer<LocalDate> listener) { this.listener = listener; }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float cellWidth = getWidth() / 7f;
        float header = dp(30);
        float cellHeight = (getHeight() - header) / 6f;
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(dp(11));
        paint.setColor(0xFF9FB3C8);
        for (int i = 0; i < 7; i++) {
            DayOfWeek day = DayOfWeek.of(((i + 6) % 7) + 1);
            canvas.drawText(day.getDisplayName(TextStyle.SHORT, Locale.US).toUpperCase(Locale.US),
                    cellWidth * (i + .5f), dp(19), paint);
        }

        int offset = month.atDay(1).getDayOfWeek().getValue() % 7;
        for (int day = 1; day <= month.lengthOfMonth(); day++) {
            int index = offset + day - 1;
            int row = index / 7;
            int column = index % 7;
            float left = column * cellWidth;
            float top = header + row * cellHeight;
            LocalDate date = month.atDay(day);
            if (date.equals(selected)) {
                paint.setColor(0xFF1F5A78);
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
            canvas.drawText(String.valueOf(day), left + dp(7), top + dp(19), paint);

            var legs = repository.forDate(date);
            if (!legs.isEmpty()) {
                FlightLeg leg = legs.get(0);
                paint.setTextSize(dp(9));
                paint.setColor(leg.isRevised() ? 0xFFFFC857 : 0xFF68D8FF);
                canvas.drawText(leg.origin() + ">" + leg.destination(), left + dp(5), top + dp(35), paint);
                paint.setColor(0xFFE8F1F8);
                canvas.drawText(leg.localTimes(), left + dp(5), top + dp(48), paint);
                canvas.drawText(leg.seatPosition() + (leg.isRevised() ? " REV " : "  ")
                        + leg.flightTime(), left + dp(5), top + dp(61), paint);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_UP) return true;
        float header = dp(30);
        if (event.getY() < header) return true;
        int column = Math.min(6, (int) (event.getX() / (getWidth() / 7f)));
        int row = Math.min(5, (int) ((event.getY() - header) / ((getHeight() - header) / 6f)));
        int offset = month.atDay(1).getDayOfWeek().getValue() % 7;
        int day = row * 7 + column - offset + 1;
        if (day >= 1 && day <= month.lengthOfMonth()) {
            selected = month.atDay(day);
            listener.accept(selected);
            invalidate();
            performClick();
        }
        return true;
    }

    @Override public boolean performClick() { super.performClick(); return true; }
    private float dp(float value) { return value * getResources().getDisplayMetrics().density; }
}
