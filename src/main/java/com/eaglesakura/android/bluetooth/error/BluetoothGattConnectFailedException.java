package com.eaglesakura.android.bluetooth.error;

/**
 * GATT接続に失敗した
 */
public class BluetoothGattConnectFailedException extends BluetoothException {
    public BluetoothGattConnectFailedException() {
    }

    public BluetoothGattConnectFailedException(String message) {
        super(message);
    }

    public BluetoothGattConnectFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    public BluetoothGattConnectFailedException(Throwable cause) {
        super(cause);
    }
}
