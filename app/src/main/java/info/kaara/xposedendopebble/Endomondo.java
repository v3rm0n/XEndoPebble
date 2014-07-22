package info.kaara.xposedendopebble;

import android.app.Activity;
import android.content.Context;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

/**
 * Created by vermon on 21/07/14.
 */
public class Endomondo {

    private ClassLoader cls;

    public interface WorkoutEventHandler {
        public void handle(Event event, Object workoutService, Context context);
    }

    public enum Event {
        START, STOP, PAUSE, RESUME, TRACK, UNKNOWN;

        public static Event fromEndoEvent(String endoEvent) {
            if ("CMD_START_WORKOUT_EVT".equals(endoEvent)) {
                return START;
            } else if ("CMD_STOP_WORKOUT_EVT".equals(endoEvent)) {
                return STOP;
            } else if ("CMD_PAUSE_WORKOUT_EVT".equals(endoEvent)) {
                return PAUSE;
            } else if ("CMD_RESUME_WORKOUT_EVT".equals(endoEvent)) {
                return RESUME;
            } else if ("WORKOUT_TRACK_TIMER_EVT".equals(endoEvent)) {
                return TRACK;
            }
            return UNKNOWN;
        }
    }

    public Endomondo(ClassLoader cls) {
        this.cls = cls;
    }

    public void handleWorkoutEvent(final WorkoutEventHandler workoutEventHandler) {
        final Class<?> workoutServiceClass = XposedHelpers.findClass("com.endomondo.android.common.workout.WorkoutService", cls);

        XposedHelpers.findAndHookMethod(workoutServiceClass, "handleEvent", "com.endomondo.android.common.generic.model.EndoEvent", new XC_MethodHook() {

            protected void afterHookedMethod(MethodHookParam param) throws java.lang.Throwable {
                Object event = param.args[0];

                String eventType = XposedHelpers.getObjectField(event, "typeEvent").toString();
                Activity activity = (Activity) XposedHelpers.getStaticObjectField(workoutServiceClass, "MAIN_ACTIVITY");

                workoutEventHandler.handle(Event.fromEndoEvent(eventType), param.thisObject, activity);
            }

        });
    }

    public boolean isImperial() {
        Class<?> settingsClass = XposedHelpers.findClass("com.endomondo.android.common.settings.Settings", cls);
        return XposedHelpers.getStaticIntField(settingsClass, "units") > 0;
    }

    public void pauseWorkout(Context context) {
        Class<?> workoutGatewayClass = XposedHelpers.findClass("com.endomondo.android.common.workout.WorkoutGateway", cls);
        XposedHelpers.callStaticMethod(workoutGatewayClass, "sendMessageToWs", context, getEndoEvent("CMD_PAUSE_WORKOUT_EVT"), 1);
    }

    public void resumeWorkout(Context context) {
        Class<?> workoutGatewayClass = XposedHelpers.findClass("com.endomondo.android.common.workout.WorkoutGateway", cls);
        XposedHelpers.callStaticMethod(workoutGatewayClass, "sendMessageToWs", context, getEndoEvent("CMD_START_WORKOUT_EVT"), 1);
    }

    private Object getEndoEvent(String eventType) {
        Class<?> endoEventType = XposedHelpers.findClass("com.endomondo.android.common.generic.model.EndoEvent$EventType", cls);
        return XposedHelpers.getStaticObjectField(endoEventType, eventType);
    }


}
