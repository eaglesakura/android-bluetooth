package com.eaglesakura.android.bluetooth.gatt;

import com.eaglesakura.android.bluetooth.BleLog;
import com.eaglesakura.android.bluetooth.error.BluetoothException;
import com.eaglesakura.android.devicetest.DeviceTestCase;

import org.junit.Test;

import android.bluetooth.BluetoothGattCharacteristic;

public class BlePeripheralDeviceConnectionTest extends DeviceTestCase {

    @Test
    public void ケイデンスセンサーに継続接続する() throws Throwable {
        BlePeripheralDeviceConnection connection = new BlePeripheralDeviceConnection(getContext(), "F9:EC:8E:EE:46:2B");
        connection.alwaysConnect(
                new BlePeripheralDeviceConnection.SessionCallback() {
                    BlePeripheralDeviceConnection.Session latestSession;

                    @Override
                    public void onSessionStart(BlePeripheralDeviceConnection self, BlePeripheralDeviceConnection.Session session) {
                        assertTrue(isTestingThread());
                        assertNotNull(session);
                        latestSession = session;
                        BleLog.debug("onSessionStart[%d]", session.getTryCount());
                    }

                    @Override
                    public void onSessionFinished(BlePeripheralDeviceConnection self, BlePeripheralDeviceConnection.Session session) {
                        assertTrue(isTestingThread());
                        assertEquals(session, latestSession);
                        assertTrue(session.mGattDisconnected);
                        BleLog.debug("onSessionFinished[%d]", session.getTryCount());
                    }
                },
                new BleSpeedCadenceSensorCallback() {
                    @Override
                    public boolean onLoop(BleDeviceConnection self, BleGattController gatt) throws BluetoothException {
                        return false;
                    }

                    @Override
                    public void onCharacteristicUpdated(BleDeviceConnection self, BleGattController gatt, BluetoothGattCharacteristic characteristic) throws BluetoothException {
                        super.onCharacteristicUpdated(self, gatt, characteristic);

                        BleLog.debug("Wheel[%s] RPM[%.1f]", getWheelValue(), getWheelRpm());
                        BleLog.debug("Crank[%s] RPM[%.1f]", getCrankValue(), getCrankRpm());
                        BleLog.debug("Battery :: " + getBatteryLevel());
                    }
                }, () -> false);
    }
}