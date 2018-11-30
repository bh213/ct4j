package com.whiletrue.clustertasks.tasks.recurring;

import java.time.Instant;
import java.util.Objects;

public interface RecurringScheduleStrategy {

    String SEPARATOR = ":-:";
    String FIXED = "fixed";

    Instant nextRunTime(Instant currentTime);

    String toDatabaseString();

    static RecurringScheduleStrategy fromString(String input) throws Exception {
        final String[] split = Objects.requireNonNull(input, "input must be non-null").split(SEPARATOR);
        if (split.length != 2) {
            throw new IllegalArgumentException("Input must have exactly two parts");
        }
        final String scheduleType = split[0];
        final String inputAfterSeparator = split[1];

        switch(scheduleType) {
            case FIXED: return new RecurringScheduleStrategyFixed(inputAfterSeparator);
            default: throw new IllegalArgumentException("Unknown RecurringScheduleStrategy:" + scheduleType);
        }

    }
}
