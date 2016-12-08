package com.eaglesakura.android.bluetooth.error;

/**
 * GATTが不意に切断された
 */
public class BluetoothGattDisconnectedException extends BluetoothException {
    public BluetoothGattDisconnectedException() {
    }

    public BluetoothGattDisconnectedException(String message) {
        super(message);
    }

    public BluetoothGattDisconnectedException(String message, Throwable cause) {
        super(message, cause);
    }

    public BluetoothGattDisconnectedException(Throwable cause) {
        super(cause);
    }
}
