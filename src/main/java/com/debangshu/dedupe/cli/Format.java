package com.debangshu.dedupe.cli;

import java.util.Locale;

/** Number formatting for console output: 1,234,567 / 99.98% / 2.4 s. */
final class Format {

    private Format() {
    }

    static String number(long value) {
        return String.format(Locale.ROOT, "%,d", value);
    }

    static String percent(double value) {
        return String.format(Locale.ROOT, "%.2f%%", value);
    }

    static String seconds(long millis) {
        return String.format(Locale.ROOT, "%.1f s", millis / 1000.0);
    }
}
