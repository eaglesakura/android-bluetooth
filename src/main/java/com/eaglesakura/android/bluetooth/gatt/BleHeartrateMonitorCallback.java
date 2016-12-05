package com.eaglesakura.android.bluetooth.gatt;

import com.eaglesakura.android.bluetooth.BleLog;
import com.eaglesakura.android.bluetooth.BluetoothLeUtil;
import com.eaglesakura.android.bluetooth.error.BluetoothException;
import com.eaglesakura.android.bluetooth.error.BluetoothGattConnectFailedException;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothGattCharacteristic;
import android.support.annotation.IntRange;
import android.support.annotation.Nullable;

/**
 * BLE心拍計制御用コールバック
 *
 * 動作確認: Wahoo HR
 */
@TargetApi(18)
public abstract class BleHeartrateMonitorCallback extends BleDeviceConnection.Callback {

    /**
     * バッテリー残量
     * 0-100
     */
    private Integer mBatteryLevel;

    /**
     * 現在の心拍値
     */
    private Integer mHeartrateBpm;

    @IntRange(from = 0, to = 100)
    @Nullable
    public Integer getBatteryLevel() {
        return mBatteryLevel;
    }

    @IntRange(from = 0)
    @Nullable
    public Integer getHeartrateBpm() {
        return mHeartrateBpm;
    }

    @Override
    public void onGattConnected(BleDeviceConnection self, BleGattController gatt) throws BluetoothException {
        // バッテリーレベルの読み込みを行う
        if (!gatt.requestRead(BluetoothLeUtil.BLE_UUID_BATTERY_SERVICE, BluetoothLeUtil.BLE_UUID_BATTERY_DATA_LEVEL)) {
            if (!gatt.requestNotification(BluetoothLeUtil.BLE_UUID_HEARTRATE_SERVICE, BluetoothLeUtil.BLE_UUID_HEARTRATE_DATA_MEASUREMENT)) {
                throw new BluetoothGattConnectFailedException("Heartrate Not Found...");
            }
        }
    }

    @Override
    public void onCharacteristicUpdated(BleDeviceConnection self, BleGattController gatt, BluetoothGattCharacteristic characteristic) throws BluetoothException {
        if (characteristic.getUuid().equals(BluetoothLeUtil.BLE_UUID_HEARTRATE_DATA_MEASUREMENT)) {
            // Blt 0bit目〜1bit目のフラグで値の型を判断する
            int flag = characteristic.getProperties();
            int format;
            if ((flag & 0x1) == 0x01) {
                format = BluetoothGattCharacteristic.FORMAT_UINT16;
            } else {
                format = BluetoothGattCharacteristic.FORMAT_UINT8;
            }

            mHeartrateBpm = characteristic.getIntValue(format, 1);
            onUpdateHeartrateBpm(mHeartrateBpm);
        } else if (characteristic.getUuid().equals(BluetoothLeUtil.BLE_UUID_BATTERY_DATA_LEVEL)) {
            boolean notifyRequest = mBatteryLevel == null;
            mBatteryLevel = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
            BleLog.gatt("HR Monitor Battery[%d]", mBatteryLevel);
            onUpdateBatteryLevel(mBatteryLevel);

            if (notifyRequest && !gatt.requestNotification(BluetoothLeUtil.BLE_UUID_HEARTRATE_SERVICE, BluetoothLeUtil.BLE_UUID_HEARTRATE_DATA_MEASUREMENT)) {
                throw new BluetoothGattConnectFailedException("Heartrate Not Found...");
            }
        }
    }

    protected void onUpdateBatteryLevel(int newLevel) {
    }

    protected void onUpdateHeartrateBpm(int newBpm) {
    }
}
