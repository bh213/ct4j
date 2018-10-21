package com.whiletrue.clustertasks.scheduler;

import com.whiletrue.clustertasks.tasks.Task;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class TaskPerformanceStatsSnapshot {
    private final HashMap<Class<? extends Task>, ExecutionStats> perTaskStats;
    private final Instant snapshotTimestamp;

    public TaskPerformanceStatsSnapshot(HashMap<Class<? extends Task>, ExecutionStats> currentTaskSnapshots, ExecutionStats currentTotalTasksStats, Instant snapshotTimestamp) {

        HashMap<Class<? extends Task>, ExecutionStats> clonedMap = new HashMap<>();

        currentTaskSnapshots.forEach((aClass, executionStats) -> clonedMap.put(aClass, new ExecutionStats(executionStats)));
        clonedMap.put(null, new ExecutionStats(currentTotalTasksStats));

        this.perTaskStats = clonedMap;
        this.snapshotTimestamp = snapshotTimestamp;
    }

    public Instant getSnapshotTimestamp() {
        return snapshotTimestamp;
    }

    public HashMap<Class<? extends Task>, ExecutionStats> getPerTaskStats() {
        return perTaskStats;
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append("===============================================================================\n");
        perTaskStats.forEach((name, totals) ->
                sb.append(String.format("%30s %5d %5d %6d %5d \n", name == null ? "total" : name.getName(), totals.getExecutions(), totals.getErrors(), totals.getSuccess(), totals.getFailures())));
        sb.append("===============================================================================\n");
        return sb.toString();
    }

    public Map<String, ExecutionStats> toMap() {
        return perTaskStats.entrySet().stream().collect(Collectors.toMap(e-> e.getKey() == null ? "total" : e.getKey().getName(), Map.Entry::getValue));
    }





}

