package com.whiletrue.ct4j.tasks.recurring;

import java.time.Instant;

public class RecurringSchedule {

    private final RecurringScheduleStrategy strategy;

    public void setNextScheduledRun(Instant nextScheduledRun) {
        this.nextScheduledRun = nextScheduledRun;
    }

    private Instant nextScheduledRun;

    private RecurringSchedule(RecurringScheduleStrategy strategy, Instant nextScheduledRun) {
        this.strategy = strategy;
        this.nextScheduledRun = nextScheduledRun;
    }

    public static RecurringSchedule createNewRecurringSchedule(RecurringScheduleStrategy strategy, Instant currentTime) {
        return new RecurringSchedule(strategy, strategy.nextRunTime(currentTime));
    }

    public static RecurringSchedule createExistingRecurringSchedule(RecurringScheduleStrategy strategy, Instant nextScheduledRun) {
        return new RecurringSchedule(strategy, nextScheduledRun);
    }

    public Instant calculateNextScheduledRun(Instant lastScheduledRun){
        return getStrategy().nextRunTime(lastScheduledRun);
    }


    public RecurringScheduleStrategy getStrategy() {
        return strategy;
    }


    public Instant getNextScheduledRun() {
        return nextScheduledRun;
    }
}
