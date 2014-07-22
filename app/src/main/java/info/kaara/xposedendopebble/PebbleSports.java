package info.kaara.xposedendopebble;

import android.content.Context;

import com.getpebble.android.kit.Constants;
import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import de.robv.android.xposed.XposedBridge;

/**
 * Created by vermon on 21/07/14.
 */
public class PebbleSports {

    private Context context;

    public PebbleSports(Context context) {
        this.context = context;
    }

    public void start() {
        XposedBridge.log("Starting watchapp");
        PebbleKit.startAppOnPebble(context, Constants.SPORTS_UUID);
    }

    public void stop() {
        XposedBridge.log("Stopping watchapp");
        PebbleKit.closeAppOnPebble(context, Constants.SPORTS_UUID);
    }

    public void update(long duration, float speed, float distanceInKm, boolean imperial, boolean showPace) {

        String time = UnitHelper.durationToHourMinOrMinSec(duration);

        String distance = String.format("%02.02f", distanceInKm);

        String speedLabel = null;
        if (showPace) {
            speedLabel = UnitHelper.speedToPace(speed, imperial);
        } else {
            speedLabel = UnitHelper.convertToPerHour(speed, imperial);
        }

        XposedBridge.log("Speedlabel: " + speedLabel);

        PebbleDictionary data = new PebbleDictionary();
        data.addString(Constants.SPORTS_TIME_KEY, time);
        data.addString(Constants.SPORTS_DISTANCE_KEY, distance);
        data.addString(Constants.SPORTS_DATA_KEY, speedLabel);

        data.addUint8(Constants.SPORTS_LABEL_KEY, (byte) (showPace ? Constants.SPORTS_DATA_PACE : Constants.SPORTS_DATA_SPEED));

        data.addUint8(Constants.SPORTS_UNITS_KEY, (byte) (imperial ? Constants.SPORTS_UNITS_IMPERIAL : Constants.SPORTS_UNITS_METRIC));

        PebbleKit.sendDataToPebble(context, Constants.SPORTS_UUID, data);
    }
}
