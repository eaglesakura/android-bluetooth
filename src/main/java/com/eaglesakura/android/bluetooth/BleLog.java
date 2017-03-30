package com.eaglesakura.android.bluetooth;

import com.eaglesakura.log.Logger;
import com.eaglesakura.util.EnvironmentUtil;

import android.util.Log;

public class BleLog {
    private static final Logger.Impl sAppLogger;

    static {
        if (EnvironmentUtil.isRunningRobolectric()) {
            sAppLogger = new Logger.RobolectricLogger() {
                @Override
                protected int getStackDepth() {
                    return super.getStackDepth() + 1;
                }
            };
        } else {
            sAppLogger = new Logger.AndroidLogger(Log.class) {
                @Override
                protected int getStackDepth() {
                    return super.getStackDepth() + 1;
                }
            }.setStackInfo(BuildConfig.DEBUG);
        }
    }

    public static void system(String fmt, Object... args) {
        String tag = "Ble.System";
        Logger.out(Logger.LEVEL_DEBUG, tag, fmt, args);
    }


    public static void gatt(String fmt, Object... args) {
        String tag = "Ble.Gatt";
        Logger.out(Logger.LEVEL_DEBUG, tag, fmt, args);
    }


    public static void debug(String fmt, Object... args) {
        String tag = "Ble.Debug";
        Logger.out(Logger.LEVEL_DEBUG, tag, fmt, args);
    }

}
