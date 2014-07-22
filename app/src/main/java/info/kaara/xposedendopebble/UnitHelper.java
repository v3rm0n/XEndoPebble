package info.kaara.xposedendopebble;

import java.util.concurrent.TimeUnit;

/**
 * Created by vermon on 21/07/14.
 */
public class UnitHelper {

    private static final float ratio = 1.609344f;

    public static String durationToHourMinOrMinSec(long duration) {
        String time = null;
        long hours = TimeUnit.SECONDS.toHours(duration);
        if (hours > 0) {
            long minutes = TimeUnit.SECONDS.toMinutes(duration - TimeUnit.HOURS.toSeconds(hours));
            time = String.format("%02d:%02d", hours, minutes);
        } else {
            long minutes = TimeUnit.SECONDS.toMinutes(duration);
            long seconds = duration - TimeUnit.MINUTES.toSeconds(minutes);
            time = String.format("%02d:%02d", minutes, seconds);
        }
        return time;
    }

    public static String distance(float distanceInKm, boolean imperial) {
        float distance = distanceInKm;
        if (imperial) {
            distance = distance / ratio;
        }
        return String.format("%02.02f", distance);
    }

    public static String convertToPerHour(float speedInMetersPerSecond, boolean imperial) {
        float speedPerHour = speedInMetersPerSecond / 1000 * 60 * 60;
        if (imperial) {
            speedPerHour = speedPerHour / ratio;
        }
        return String.format("%02.02f", speedPerHour);
    }

    public static String speedToPace(float speedInMetersPerSecond, boolean imperial) {
        if (speedInMetersPerSecond <= 0) {
            return "0:00";
        }
        float speedPerHour = speedInMetersPerSecond / 1000 * 60 * 60;
        if (imperial) {
            speedPerHour = speedPerHour / ratio;
        }
        float pacePerMinute = 60 / speedPerHour;
        long minutes = (long) Math.floor(pacePerMinute);
        long seconds = Math.round((pacePerMinute - minutes) * 60);
        return String.format("%02d:%02d", minutes, seconds);
    }
}
