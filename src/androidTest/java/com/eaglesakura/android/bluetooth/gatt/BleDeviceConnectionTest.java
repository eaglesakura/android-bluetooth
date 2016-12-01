package com.eaglesakura.android.bluetooth.gatt;

import com.eaglesakura.android.bluetooth.BleLog;
import com.eaglesakura.android.bluetooth.error.BluetoothException;
import com.eaglesakura.android.devicetest.DeviceTestCase;

import org.junit.Test;

import android.bluetooth.BluetoothGattCharacteristic;

/**
 * 各種接続テストを行う
 */
public class BleDeviceConnectionTest extends DeviceTestCase {

    @Test
    public void ハートレートモニターに接続する() throws Throwable {
        BleDeviceConnection connection = new BleDeviceConnection(getContext(), "CE:16:3A:86:48:F9");

        connection.connect(new BleHeartrateMonitorCallback() {
            @Override
            public boolean onLoop(BleDeviceConnection self, BleGattController gatt) throws BluetoothException {
                return getHeartrateBpm() != null;
            }

            @Override
            public void onCharacteristicUpdated(BleDeviceConnection self, BleGattController gatt, BluetoothGattCharacteristic characteristic) throws BluetoothException {
                super.onCharacteristicUpdated(self, gatt, characteristic);
                BleLog.debug("Battery :: " + getBatteryLevel());
                BleLog.debug("Heartrate :: " + getHeartrateBpm());
            }
        }, () -> false);
    }
}