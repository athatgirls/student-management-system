package org.example.util;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/** Server-generated times have always been persisted as UTC on the Linux server. */
public final class UtcTimestamps {
    private UtcTimestamps() { }
    public static String toWire(LocalDateTime value) {
        return value == null ? null : DateTimeFormatter.ISO_INSTANT.format(value.toInstant(ZoneOffset.UTC));
    }
}
