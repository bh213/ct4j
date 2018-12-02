package com.whiletrue.ct4j.tasks.recurring;

import java.time.Instant;

public class RecurringScheduleStrategyFixed implements RecurringScheduleStrategy {

    final private int fixedScheduleMilliseconds;

    public RecurringScheduleStrategyFixed(int fixedScheduleMilliseconds) {
        this.fixedScheduleMilliseconds = fixedScheduleMilliseconds;
    }

    public RecurringScheduleStrategyFixed(String inputAfterSeparator) {
        this.fixedScheduleMilliseconds = Integer.valueOf(inputAfterSeparator);
    }

    @Override
    public Instant nextRunTime(Instant currentTime) {
        return currentTime.plusMillis(fixedScheduleMilliseconds);
    }

    @Override
    public String toDatabaseString() {
        return FIXED + SEPARATOR + fixedScheduleMilliseconds;
    }
}
