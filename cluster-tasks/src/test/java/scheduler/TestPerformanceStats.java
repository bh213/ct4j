package scheduler;


import com.whiletrue.clustertasks.tasks.Task;
import com.whiletrue.clustertasks.scheduler.ExecutionStats;
import com.whiletrue.clustertasks.scheduler.TaskPerformanceStatsInterval;
import com.whiletrue.clustertasks.scheduler.TaskPerformanceStatsSnapshot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import tasks.NoOpTestTask;


import java.time.Instant;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;


@DisplayName("Performance snapshot and intervals test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestPerformanceStats {


    @Test
    @DisplayName("Test snapshot creation and proper cloning of data")
    public void testPerformanceStatsCreationAndClone() {
        HashMap<Class<? extends Task>, ExecutionStats> init = new HashMap<>();
        final ExecutionStats initSource = new ExecutionStats(1, 2, 3, 4);
        final ExecutionStats totalStats = new ExecutionStats(33, 34, 35, 36);

        init.put(NoOpTestTask.class, initSource);


        final Instant now = Instant.now();
        TaskPerformanceStatsSnapshot snapshot = new TaskPerformanceStatsSnapshot(init, totalStats, now);
        assertThat(snapshot).isNotNull();
        assertThat(snapshot.getSnapshotTimestamp()).isEqualTo(now);
        assertThat(snapshot.getPerTaskStats()).isNotNull();
        assertThat(snapshot.getPerTaskStats().size()).isEqualTo(2);
        assertThat(snapshot.getPerTaskStats().containsKey(NoOpTestTask.class)).isTrue();
        assertThat(snapshot.getPerTaskStats().get(NoOpTestTask.class).getErrors()).isEqualTo(2);
        assertThat(snapshot.getPerTaskStats().get(NoOpTestTask.class).getExecutions()).isEqualTo(1);
        assertThat(snapshot.getPerTaskStats().get(NoOpTestTask.class).getSuccess()).isEqualTo(3);
        assertThat(snapshot.getPerTaskStats().get(NoOpTestTask.class).getFailures()).isEqualTo(4);

        initSource.taskFailed();
        initSource.taskError();
        initSource.taskCompleted();

        assertThat(snapshot.getPerTaskStats().get(NoOpTestTask.class).getErrors()).isEqualTo(2);
        assertThat(snapshot.getPerTaskStats().get(NoOpTestTask.class).getExecutions()).isEqualTo(1);
        assertThat(snapshot.getPerTaskStats().get(NoOpTestTask.class).getSuccess()).isEqualTo(3);
        assertThat(snapshot.getPerTaskStats().get(NoOpTestTask.class).getFailures()).isEqualTo(4);


    }


    @Test
    @DisplayName("Test interval creation and proper cloning of data")
    public void testPerformanceStatsInterval() {
        HashMap<Class<? extends Task>, ExecutionStats> init = new HashMap<>();
        final ExecutionStats initSource = new ExecutionStats(1, 2, 3, 4);
        final ExecutionStats totalStats = new ExecutionStats(33, 34, 35, 36);
        init.put(NoOpTestTask.class, initSource);
        Instant now = Instant.now();
        TaskPerformanceStatsSnapshot snapshotStart1 = new TaskPerformanceStatsSnapshot(init, totalStats, now);


        HashMap<Class<? extends Task>, ExecutionStats> init2 = new HashMap<>();
        final ExecutionStats initSource2 = new ExecutionStats(1 + 5, 2 + 7, 3 + 9, 4 + 11);
        final ExecutionStats totalStats2 = new ExecutionStats(33 + 12, 34 + 13, 35 + 14, 36 + 15);
        init2.put(NoOpTestTask.class, initSource2);
        TaskPerformanceStatsSnapshot snapshotStart2 = new TaskPerformanceStatsSnapshot(init2, totalStats2, now.plusSeconds(1));

        TaskPerformanceStatsInterval interval = new TaskPerformanceStatsInterval(snapshotStart1, snapshotStart2);

        assertThat(interval.getIntervalStart()).isEqualTo(now);
        assertThat(interval.getIntervalEnd()).isEqualTo(now.plusSeconds(1));
        assertThat(interval.getIntervalMilliseconds()).isEqualTo(1000);

        assertThat(interval.getIntervalStats()).isNotNull();
        assertThat(interval.getIntervalStats().size()).isEqualTo(2);
        assertThat(interval.getIntervalStats().containsKey(NoOpTestTask.class)).isTrue();
        assertThat(interval.getIntervalStats().get(NoOpTestTask.class).getExecutions()).isEqualTo(5);
        assertThat(interval.getIntervalStats().get(NoOpTestTask.class).getErrors()).isEqualTo(7);
        assertThat(interval.getIntervalStats().get(NoOpTestTask.class).getSuccess()).isEqualTo(9);
        assertThat(interval.getIntervalStats().get(NoOpTestTask.class).getFailures()).isEqualTo(11);

        assertThat(interval.getIntervalStats().containsKey(null)).isTrue();
        assertThat(interval.getIntervalStats().get(null).getExecutions()).isEqualTo(12);
        assertThat(interval.getIntervalStats().get(null).getErrors()).isEqualTo(13);
        assertThat(interval.getIntervalStats().get(null).getSuccess()).isEqualTo(14);
        assertThat(interval.getIntervalStats().get(null).getFailures()).isEqualTo(15);



    }
}
