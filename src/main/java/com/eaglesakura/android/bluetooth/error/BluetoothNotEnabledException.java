package com.eaglesakura.android.bluetooth.error;

/**
 * Bluetooth処理関連の例外
 */
public class BluetoothNotEnabledException extends BluetoothException {
    public BluetoothNotEnabledException() {
    }

    public BluetoothNotEnabledException(String message) {
        super(message);
    }

    public BluetoothNotEnabledException(String message, Throwable cause) {
        super(message, cause);
    }

    public BluetoothNotEnabledException(Throwable cause) {
        super(cause);
    }
}
