package com.whiletrue.ct4j.config;

import com.whiletrue.ct4j.timeprovider.TimeProvider;

import java.time.Instant;

public class FixedTimeProvider implements TimeProvider {

    private Instant fixed;

    public FixedTimeProvider(Instant fixed) {
        this.fixed = fixed;
    }

    public FixedTimeProvider() {
        this(Instant.now());
    }

    @Override
    public Instant getCurrent() {
        return fixed;
    }


    public void setCurrent(Instant newNow) {
        this.fixed = newNow;
    }

    public void plusMillis(long millis) {
        this.fixed = this.fixed.plusMillis(millis);
    }

}
