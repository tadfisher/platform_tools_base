package com.android.tools.perflib.vmtrace.viz;

import java.text.DecimalFormat;
import java.util.concurrent.TimeUnit;

public class TimeUtils {
    public static String humanizeTime(long time, long span, TimeUnit timeUnits) {
        String units;
        double scale;
        if (timeUnits.toSeconds(span) > 0) {
            units = "s";
            scale = 1e-9;
        } else if (timeUnits.toMillis(span) > 0) {
            units = "ms";
            scale = 1e-6;
        } else {
            units = "us";
            scale = 1e-3;
        }

        return String.format("%1$s %2$s", formatTime(timeUnits.toNanos(time), scale), units);
    }

    private static final DecimalFormat TIME_FORMATTER = new DecimalFormat("#.###");

    private static String formatTime(long nsecs, double scale) {
        return TIME_FORMATTER.format(nsecs * scale);
    }
}
