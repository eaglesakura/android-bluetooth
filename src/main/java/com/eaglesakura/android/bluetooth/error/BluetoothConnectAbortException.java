package com.eaglesakura.android.bluetooth.error;

/**
 * Bluetooth接続をあきらめた
 */
public class BluetoothConnectAbortException extends BluetoothException {
    public BluetoothConnectAbortException() {
    }

    public BluetoothConnectAbortException(String message) {
        super(message);
    }

    public BluetoothConnectAbortException(String message, Throwable cause) {
        super(message, cause);
    }

    public BluetoothConnectAbortException(Throwable cause) {
        super(cause);
    }
}

