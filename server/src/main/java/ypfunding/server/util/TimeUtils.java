package ypfunding.server.util;

import java.time.LocalDateTime;
import java.time.ZoneId;

public final class TimeUtils {
    private TimeUtils() {}

    public static LocalDateTime getCurrentTime() {
        return LocalDateTime.now(ZoneId.systemDefault());
    }
}
