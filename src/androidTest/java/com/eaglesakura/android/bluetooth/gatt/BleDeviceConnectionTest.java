package com.eaglesakura.android.bluetooth.gatt;

import com.eaglesakura.android.bluetooth.BleLog;
import com.eaglesakura.android.bluetooth.error.BluetoothConnectAbortException;
import com.eaglesakura.android.bluetooth.error.BluetoothException;
import com.eaglesakura.android.devicetest.DeviceTestCase;
import com.eaglesakura.thread.IntHolder;
import com.eaglesakura.util.Util;

import org.junit.Test;

import android.bluetooth.BluetoothGattCharacteristic;

/**
 * 各種接続テストを行う
 */
public class BleDeviceConnectionTest extends DeviceTestCase {

    @Test
    public void ハートレートモニターに複数回接続する() throws Throwable {
        BleDeviceConnection connection = new BleDeviceConnection(getContext(), "CE:16:3A:86:48:F9");

        // 適当な回数を繰り返させる
        for (int i = 0; i < 5; ++i) {
            try {
                IntHolder heartrateUpdateCount = new IntHolder();
                Util.sleep(1000 * 10);
                connection.connect(new BleHeartrateMonitorCallback() {
                    @Override
                    public boolean onLoop(BleDeviceConnection self, BleGattController gatt) throws BluetoothException {
                        return false;
                    }

                    @Override
                    public void onCharacteristicUpdated(BleDeviceConnection self, BleGattController gatt, BluetoothGattCharacteristic characteristic) throws BluetoothException {
                        super.onCharacteristicUpdated(self, gatt, characteristic);
                        heartrateUpdateCount.add(1);
                        BleLog.debug("Battery :: " + getBatteryLevel());
                        BleLog.debug("Heartrate :: " + getHeartrateBpm());
                    }
                }, () -> heartrateUpdateCount.value > 3);

                // 例外が投げられなければならない
                fail();
            } catch (BluetoothConnectAbortException e) {
                // 正常に抜ける
                Util.sleep(1000 * 10);
                connection.connect(new BleHeartrateMonitorCallback() {
                    @Override
                    public boolean onLoop(BleDeviceConnection self, BleGattController gatt) throws BluetoothException {
                        return getHeartrateBpm() != null && getBatteryLevel() != 0;
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
    }

    @Test
    public void ケイデンスセンサーに複数回接続する() throws Throwable {
        BleDeviceConnection connection = new BleDeviceConnection(getContext(), "F9:EC:8E:EE:46:2B");
        for (int i = 0; i < 5; ++i) {
            try {
                IntHolder updateCount = new IntHolder();
                Util.sleep(1000 * 5);
                connection.connect(new BleSpeedCadenceSensorCallback() {
                    @Override
                    public boolean onLoop(BleDeviceConnection self, BleGattController gatt) throws BluetoothException {
                        return false;
                    }

                    @Override
                    public void onCharacteristicUpdated(BleDeviceConnection self, BleGattController gatt, BluetoothGattCharacteristic characteristic) throws BluetoothException {
                        super.onCharacteristicUpdated(self, gatt, characteristic);

                        BleLog.debug("Wheel   :: " + getWheelValue());
                        BleLog.debug("Crank   :: " + getCrankValue());
                        BleLog.debug("Battery :: " + getBatteryLevel());
                        updateCount.add(1);
                    }
                }, () -> updateCount.value > 3);
            } catch (BluetoothException e) {
                Util.sleep(1000 * 5);
                connection.connect(new BleSpeedCadenceSensorCallback() {
                    @Override
                    public boolean onLoop(BleDeviceConnection self, BleGattController gatt) throws BluetoothException {
                        return getWheelValue() != null && getCrankValue() != null && getBatteryLevel() != null;
                    }

                    @Override
                    public void onCharacteristicUpdated(BleDeviceConnection self, BleGattController gatt, BluetoothGattCharacteristic characteristic) throws BluetoothException {
                        super.onCharacteristicUpdated(self, gatt, characteristic);

                        BleLog.debug("Wheel   :: " + getWheelValue());
                        BleLog.debug("Crank   :: " + getCrankValue());
                        BleLog.debug("Battery :: " + getBatteryLevel());
                    }
                }, () -> false);
            }
        }
    }
}