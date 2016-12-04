package com.eaglesakura.android.bluetooth.gatt;

import com.eaglesakura.android.bluetooth.error.BluetoothException;

import android.bluetooth.BluetoothGattCharacteristic;

/**
 * BLE接続S&Cセンサーの処理を行う
 *
 *
 * 動作確認: Wahoo SC
 */
public abstract class BleSpeedCadenceSensorCallback extends BleDeviceConnection.Callback {

    @Override
    public void onGattConnected(BleDeviceConnection self, BleGattController gatt) throws BluetoothException {

    }

    @Override
    public void onCharacteristicUpdated(BleDeviceConnection self, BleGattController gatt, BluetoothGattCharacteristic characteristic) throws BluetoothException {

    }
}
