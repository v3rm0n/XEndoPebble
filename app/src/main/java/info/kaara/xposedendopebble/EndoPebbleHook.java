package info.kaara.xposedendopebble;

import android.content.Context;

import com.getpebble.android.kit.Constants;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Created by vermon on 19/07/14.
 */
public class EndoPebbleHook implements IXposedHookLoadPackage, IXposedHookZygoteInit {

    private static final String PACKAGE_NAME = EndoPebbleHook.class.getPackage().getName();
    private static XSharedPreferences pref;

    private PebbleSports pebbleSports;
    private PebbleSportsDataHandler pebbleSportsDataHandler;
    private Endomondo endomondo;

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        if (!loadPackageParam.packageName.startsWith("com.endomondo.android")) {
            return;
        }

        XposedBridge.log("Loaded " + loadPackageParam.packageName);

        endomondo = new Endomondo(loadPackageParam.classLoader);

        endomondo.handleWorkoutEvent(new Endomondo.WorkoutEventHandler() {
            @Override
            public void handle(Endomondo.Event event, Object workoutService, Context context) {
                switch (event) {
                    case START:
                        pref.reload();
                        startWorkout(context);
                        break;
                    case STOP:
                        stopWorkout();
                        break;
                    case TRACK:
                        trackTimer(context, workoutService, endomondo.isImperial());
                        break;
                    case PAUSE:
                        XposedBridge.log("Workout paused");
                        break;
                    case RESUME:
                        XposedBridge.log("Workout resumed");
                    case UNKNOWN:
                        //Ignore
                        break;
                    default:
                        throw new RuntimeException("Event type doesn't exist: " + event);
                }
            }
        });
    }

    private void startWorkout(final Context context) {
        if (pebbleSports == null) {
            if (pebbleSportsDataHandler != null) {
                pebbleSportsDataHandler.unregisterSportsDataHandler();
            }
            pebbleSports = new PebbleSports(context);
            pebbleSports.start();
            pebbleSportsDataHandler = new PebbleSportsDataHandler(context);
            pebbleSportsDataHandler.init(new PebbleSportsDataHandler.PebbleSportsStateHandler() {
                @Override
                public void handle(int oldSportsState, int newSportsState) {

                    XposedBridge.log("Pebble state changed to " + newSportsState);

                    //Wrong way around, but it works. Why??
                    if (newSportsState == Constants.SPORTS_STATE_PAUSED) {
                        endomondo.resumeWorkout(context);
                    } else if (newSportsState == Constants.SPORTS_STATE_RUNNING) {
                        endomondo.pauseWorkout(context);
                    }
                }
            });
        }
        XposedBridge.log("Workout started");
    }

    private void stopWorkout() {
        if (pebbleSportsDataHandler != null) {
            pebbleSportsDataHandler.destroy();
            pebbleSportsDataHandler = null;
        }
        if (pebbleSports != null) {
            pebbleSports.stop();
            pebbleSports = null;

        }
        XposedBridge.log("Workout stopped");
    }

    private void trackTimer(final Context context, Object workoutService, boolean imperial) {
        if (pebbleSports == null) {
            pref.reload();
            startWorkout(context);
        }
        Object workout = XposedHelpers.getObjectField(workoutService, "mWorkout");

        long duration = XposedHelpers.getLongField(workout, "duration");
        float distanceInKm = XposedHelpers.getFloatField(workout, "distanceInKm");

        float speed3 = XposedHelpers.getFloatField(workoutService, "mSpeed3");

        pebbleSports.update(duration, speed3, distanceInKm, imperial, isShowPace());
    }

    private boolean isShowPace() {
        return pref.getBoolean(SettingsActivity.SPEED_PACE, false);
    }

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        pref = new XSharedPreferences(PACKAGE_NAME);
    }

}
