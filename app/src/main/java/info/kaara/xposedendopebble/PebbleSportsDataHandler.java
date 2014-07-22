package info.kaara.xposedendopebble;

import android.content.Context;

import com.getpebble.android.kit.Constants;
import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Created by vermon on 21/07/14.
 */
public class PebbleSportsDataHandler {

    public interface PebbleSportsStateHandler {
        void handle(int oldSportsState, int newSportsState);
    }

    private PebbleKit.PebbleDataReceiver sportsDataHandler;
    private int sportsState = Constants.SPORTS_STATE_INIT;
    private Context context;
    private XC_MethodHook.Unhook onResume;
    private XC_MethodHook.Unhook onPause;

    public PebbleSportsDataHandler(Context context) {
        this.context = context;
    }

    public void init(final PebbleSportsStateHandler pebbleSportsStateHandler) {

        XposedBridge.log("Registering initially");

        registerSportsDataHandler(pebbleSportsStateHandler);

        onResume = XposedHelpers.findAndHookMethod(context.getClass(), "onResume", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                registerSportsDataHandler(pebbleSportsStateHandler);
                XposedBridge.log("Registering on resume");
            }
        });

        onPause = XposedHelpers.findAndHookMethod(context.getClass(), "onPause", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                unregisterSportsDataHandler();
                XposedBridge.log("Unregistering on pause");
            }
        });
    }

    private void registerSportsDataHandler(final PebbleSportsStateHandler pebbleSportsStateHandler) {
        sportsDataHandler = new PebbleKit.PebbleDataReceiver(Constants.SPORTS_UUID) {
            @Override
            public void receiveData(final Context context, final int transactionId, final PebbleDictionary data) {
                int newState = data.getUnsignedInteger(Constants.SPORTS_STATE_KEY).intValue();

                PebbleKit.sendAckToPebble(context, transactionId);

                pebbleSportsStateHandler.handle(sportsState, newState);

                sportsState = newState;

            }
        };
        PebbleKit.registerReceivedDataHandler(context, sportsDataHandler);
    }

    public void unregisterSportsDataHandler() {
        if (sportsDataHandler != null) {
            context.unregisterReceiver(sportsDataHandler);
            sportsDataHandler = null;
        }
    }

    public void destroy() {
        unregisterSportsDataHandler();
        if (onPause != null) {
            onPause.unhook();
        }
        if (onResume != null) {
            onResume.unhook();
        }
    }
}
