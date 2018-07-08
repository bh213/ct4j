package com.whiletrue.clustertasks.timeprovider;

import java.time.Instant;
import java.util.Date;

public interface TimeProvider {
    Instant getCurrent();
    default Date getCurrentDate() {
        return Date.from(getCurrent());
    }
}
