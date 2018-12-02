package com.whiletrue.ct4j.timeprovider;

import java.time.Instant;

public class LocalTimeProvider implements TimeProvider {
    @Override
    public Instant getCurrent() {
        return Instant.now();
    }
}
