package ar.edu.itba.paw.services.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

public final class DateTimeUtils {

    public static LocalDateTime getCurrent() {
        return getCurrent(ZoneOffset.UTC);
    }

    public static LocalDateTime getCurrent(ZoneOffset offset) {
        return LocalDateTime.ofInstant(Instant.now(), offset);
    }

    public static LocalDateTime inTimezone(LocalDateTime original, String originalZone, String targetZone) {
        final ZoneId source = ZoneId.of(originalZone);
        final ZoneId target = ZoneId.of(targetZone);

        return original.atZone(source).withZoneSameInstant(target).toLocalDateTime();
    }

    // Treats source as UTC
    public static LocalDateTime inTimezone(LocalDateTime original, String targetZone) {
        final ZoneId target = ZoneId.of(targetZone);

        return original.atZone(ZoneOffset.UTC).withZoneSameInstant(target).toLocalDateTime();
    }
}
