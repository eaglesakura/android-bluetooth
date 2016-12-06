package com.eaglesakura.android.bluetooth.error;

/**
 * データが規定時間内に到達しなかった
 */
public class BluetoothDataTimeoutException extends BluetoothException {
    public BluetoothDataTimeoutException() {
    }

    public BluetoothDataTimeoutException(String message) {
        super(message);
    }

    public BluetoothDataTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }

    public BluetoothDataTimeoutException(Throwable cause) {
        super(cause);
    }
}
