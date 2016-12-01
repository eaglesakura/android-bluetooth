package com.eaglesakura.android.bluetooth.gatt;

import android.bluetooth.BluetoothGatt;

import java.util.UUID;

/**
 * Bluetooth GATT制御を行う
 */
public interface BleGattController {
    BluetoothGatt getGatt();

    /**
     * 読み込みを行わせる
     */
    boolean requestRead(UUID serviceUuid, UUID characteristicUuid);

    /**
     * 通知を行わせる
     */
    boolean requestNotification(UUID serviceUuid, UUID characteristicUuid);
}
