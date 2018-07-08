package com.whiletrue.clustertasks.scheduler;

import com.whiletrue.clustertasks.tasks.Task;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class TaskPerformanceStatsInterval {
    private final Instant intervalStart;
    private final Instant intervalEnd;
    private final float intervalMilliseconds;
    private final Map<Class<? extends Task>, ExecutionStats> intervalStats;

    public TaskPerformanceStatsInterval(TaskPerformanceStatsSnapshot start, TaskPerformanceStatsSnapshot end) {
        Objects.requireNonNull(start);
        Objects.requireNonNull(end);
        intervalStart = start.getSnapshotTimestamp();
        intervalEnd = end.getSnapshotTimestamp();
        intervalMilliseconds = Duration.between(intervalStart, intervalEnd).toMillis();


        HashMap<Class<? extends Task>, ExecutionStats> clonedEnd= new HashMap<>();

        end.getPerTaskStats().forEach((aClass, executionStats) -> clonedEnd.put(aClass, new ExecutionStats(executionStats)));



        intervalStats = clonedEnd.entrySet().stream().map(entry -> {

            if (start.getPerTaskStats().containsKey(entry.getKey())) {
                final ExecutionStats endStats = entry.getValue();
                final ExecutionStats startStats = start.getPerTaskStats().get(entry.getKey());

                ExecutionStats value =
                        new ExecutionStats(
                                endStats.getExecutions() - startStats.getExecutions(),
                                endStats.getErrors() - startStats.getErrors(),
                                endStats.getSuccess() - startStats.getSuccess(),
                                endStats.getFailures() - startStats.getFailures()
                        );
                entry.setValue(value);
            }
            return entry;
        }).collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue
        ));


    }

    public Instant getIntervalStart() {
        return intervalStart;
    }

    public Instant getIntervalEnd() {
        return intervalEnd;
    }

    public float getIntervalMilliseconds() {
        return intervalMilliseconds;
    }

    public Map<Class<? extends Task>, ExecutionStats> getIntervalStats() {
        return intervalStats;
    }

    public String toTableString() {
        StringBuilder sb = new StringBuilder();
        sb.append("===============================================================================\n");

        sb.append(String.format("%-30.30s %5s %5s %6s %5s %8s %8s\n", "name", "execs", "error", "success", "fail", "req/s", "duration"));
        sb.append("-------------------------------------------------------------------------------\n");

        intervalStats.forEach((name, totals) ->
                sb.append(String.format("%30s %5d %5d %6d %5d %8.1f %8.1f ms\n", name == null ? "total" : name.getName(), totals.getExecutions(), totals.getErrors(), totals.getSuccess(), totals.getFailures(), (float) (1000.0 * totals.getExecutions() / this.intervalMilliseconds), this.intervalMilliseconds)));

        sb.append("===============================================================================\n");
        return sb.toString();
    }
}
