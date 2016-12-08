package com.eaglesakura.android.bluetooth.error;

/**
 * Bluetooth処理関連の例外
 */
public class BluetoothException extends Exception {
    public BluetoothException() {
    }

    public BluetoothException(String message) {
        super(message);
    }

    public BluetoothException(String message, Throwable cause) {
        super(message, cause);
    }

    public BluetoothException(Throwable cause) {
        super(cause);
    }
}
