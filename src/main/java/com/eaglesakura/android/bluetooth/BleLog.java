package com.eaglesakura.android.bluetooth;

import com.eaglesakura.util.EnvironmentUtil;
import com.eaglesakura.util.LogUtil;

import android.util.Log;

public class BleLog {
    private static final LogUtil.Logger sAppLogger;

    static {
        if (EnvironmentUtil.isRunningRobolectric()) {
            sAppLogger = ((level, tag, msg) -> {
                switch (level) {
                    case LogUtil.LOGGER_LEVEL_INFO:
                        tag = "I/" + tag;
                        break;
                    case LogUtil.LOGGER_LEVEL_ERROR:
                        tag = "E/" + tag;
                        break;
                    default:
                        tag = "D/" + tag;
                        break;
                }

                try {
                    StackTraceElement[] trace = new Exception().getStackTrace();
                    StackTraceElement elem = trace[Math.min(trace.length - 1, 3)];
                    System.out.println(String.format("%s | %s[%d] : %s", tag, elem.getFileName(), elem.getLineNumber(), msg));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } else {
            sAppLogger = new LogUtil.AndroidLogger(Log.class) {
                @Override
                protected int getStackDepth() {
                    return 3;
                }
            }.setStackInfo(BuildConfig.DEBUG);
        }
    }

    public static void system(String fmt, Object... args) {
        String tag = "Ble.System";

        LogUtil.setLogger(tag, sAppLogger);
        LogUtil.out(tag, fmt, args);
    }


    public static void gatt(String fmt, Object... args) {
        String tag = "Ble.Gatt";

        LogUtil.setLogger(tag, sAppLogger);
        LogUtil.out(tag, fmt, args);
    }


    public static void debug(String fmt, Object... args) {
        String tag = "Ble.Debug";

        LogUtil.setLogger(tag, sAppLogger);
        LogUtil.out(tag, fmt, args);
    }

}
