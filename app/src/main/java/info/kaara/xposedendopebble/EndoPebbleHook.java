package info.kaara.xposedendopebble;

import android.app.Activity;
import android.content.Context;

import com.getpebble.android.kit.Constants;
import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Created by vermon on 19/07/14.
 */
public class EndoPebbleHook implements IXposedHookLoadPackage {

    private final Random rand = new Random();

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        if (!loadPackageParam.packageName.startsWith("com.endomondo.android")) {
            return;
        }

        XposedBridge.log("Loaded " + loadPackageParam.packageName);

        final Class<?> workoutServiceClass = XposedHelpers.findClass("com.endomondo.android.common.workout.WorkoutService", loadPackageParam.classLoader);

        XposedHelpers.findAndHookMethod(workoutServiceClass, "handleEvent", "com.endomondo.android.common.generic.model.EndoEvent", new XC_MethodHook() {

            protected void afterHookedMethod(MethodHookParam param) throws java.lang.Throwable {
                Object event = param.args[0];

                String eventType = XposedHelpers.getObjectField(event, "typeEvent").toString();
                Activity activity = (Activity) XposedHelpers.getStaticObjectField(workoutServiceClass, "MAIN_ACTIVITY");

                if ("CMD_START_WORKOUT_EVT".equals(eventType)) {
                    startWatchApp(activity.getApplicationContext());
                } else if ("CMD_STOP_WORKOUT_EVT".equals(eventType)) {
                    stopWatchApp(activity.getApplicationContext());
                } else if ("WORKOUT_TRACK_TIMER_EVT".equals(eventType)) {
                    Object workout = XposedHelpers.getObjectField(param.thisObject, "mWorkout");

                    long duration = XposedHelpers.getLongField(workout, "duration");
                    float speed = XposedHelpers.getFloatField(workout, "speed");
                    float distanceInKm = XposedHelpers.getFloatField(workout, "distanceInKm");

                    updateWatchApp(activity.getApplicationContext(), duration, speed, distanceInKm);
                }
            }

        });
    }

    // Send a broadcast to launch the specified application on the connected Pebble
    public void startWatchApp(Context context) {
        XposedBridge.log("Starting watchapp");
        PebbleKit.startAppOnPebble(context, Constants.SPORTS_UUID);
    }

    // Send a broadcast to close the specified application on the connected Pebble
    public void stopWatchApp(Context context) {
        XposedBridge.log("Stopping watchapp");
        PebbleKit.closeAppOnPebble(context, Constants.SPORTS_UUID);
    }

    // Push (distance, time, pace) data to be displayed on Pebble's Sports app.
    //
    // To simplify formatting, values are transmitted to the watch as strings.
    public void updateWatchApp(Context context, long duration, float speed, float distanceInKm) {
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
        String distance = String.format("%02.02f", distanceInKm);

        XposedBridge.log("Speed " + speed);
        String speedLabel = String.format("%02d:%02d", rand.nextInt(10), rand.nextInt(60));

        PebbleDictionary data = new PebbleDictionary();
        data.addString(Constants.SPORTS_TIME_KEY, time);
        data.addString(Constants.SPORTS_DISTANCE_KEY, distance);
        data.addString(Constants.SPORTS_DATA_KEY, speedLabel);

        data.addUint8(Constants.SPORTS_LABEL_KEY, (byte) Constants.SPORTS_DATA_SPEED);
        data.addUint8(Constants.SPORTS_UNITS_KEY, (byte) Constants.SPORTS_UNITS_METRIC);

        PebbleKit.sendDataToPebble(context, Constants.SPORTS_UUID, data);
    }
}
