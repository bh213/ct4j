package com.whiletrue.clustertasks.tasks;

import java.util.concurrent.atomic.AtomicInteger;

public class AtomicCounterTask extends Task<AtomicInteger> {

    @Override
    public void run(AtomicInteger i, TaskExecutionContext taskExecutionContext) throws Exception {

        i.incrementAndGet();
    }
}
