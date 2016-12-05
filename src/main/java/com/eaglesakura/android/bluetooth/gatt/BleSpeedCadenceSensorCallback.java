package com.eaglesakura.android.bluetooth.gatt;

import com.eaglesakura.android.bluetooth.BleLog;
import com.eaglesakura.android.bluetooth.BluetoothLeUtil;
import com.eaglesakura.android.bluetooth.error.BluetoothException;
import com.eaglesakura.android.bluetooth.error.BluetoothGattConnectFailedException;
import com.eaglesakura.android.bluetooth.gatt.scs.RawSensorValue;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothGattCharacteristic;

/**
 * BLE接続S&Cセンサーの処理を行う
 *
 *
 * 動作確認: Wahoo SC
 */
@TargetApi(18)
public abstract class BleSpeedCadenceSensorCallback extends BleDeviceConnection.Callback {

    /**
     * バッテリー残量
     * 0-100
     */
    private Integer mBatteryLevel;

    /**
     * クランクから受信した情報
     *
     * 回転数・経過時間
     */
    private RawSensorValue mCrankValue;

    /**
     * ホイールから受信した情報
     *
     * 回転数・経過時間
     */
    private RawSensorValue mWheelValue;

    @Override
    public void onGattConnected(BleDeviceConnection self, BleGattController gatt) throws BluetoothException {
        // バッテリーレベルの読み込みを行う
        if (!gatt.requestRead(BluetoothLeUtil.BLE_UUID_BATTERY_SERVICE, BluetoothLeUtil.BLE_UUID_BATTERY_DATA_LEVEL)) {
            if (!gatt.requestNotification(BluetoothLeUtil.BLE_UUID_SPEED_AND_CADENCE_SERVICE, BluetoothLeUtil.BLE_UUID_SPEED_AND_CADENCE_MEASUREMENT)) {
                throw new BluetoothGattConnectFailedException("Speed&Cadence Not Found...");
            }
        }
    }

    public Integer getBatteryLevel() {
        return mBatteryLevel;
    }

    public RawSensorValue getCrankValue() {
        return mCrankValue;
    }

    public RawSensorValue getWheelValue() {
        return mWheelValue;
    }

    /**
     * 現在速度を取得する
     *
     * @param wheelOuterLengthMM ミリメートル単位のホイール周長
     * @return 取得できない場合は0.0, 取得できている場合は時速を返却する
     */
    public double getSpeedKmPerHout(double wheelOuterLengthMM) {
        if (mWheelValue == null) {
            return 0.0;
        }
        return calcSpeedKmPerHour(mWheelValue.getRpm(), wheelOuterLengthMM);
    }

    /**
     * ホイール回転数と外周長から時速を算出する
     */
    public static double calcSpeedKmPerHour(double wheelRpm, double wheelOuterLength) {
        // 現在の1分間回転数から毎時間回転数に変換
        final double currentRpHour = wheelRpm * 60;
        // ホイールの外周mm/hに変換
        double moveLength = currentRpHour * wheelOuterLength;
        // mm => m => km
        moveLength /= (1000.0 * 1000.0);
        return moveLength;
    }

    @Override
    public void onCharacteristicUpdated(BleDeviceConnection self, BleGattController gatt, BluetoothGattCharacteristic characteristic) throws BluetoothException {
        if (characteristic.getUuid().equals(BluetoothLeUtil.BLE_UUID_SPEED_AND_CADENCE_MEASUREMENT)) {
            final boolean hasCadence;
            final boolean hasSpeed;
            {
                int flags = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                // ビット[0]がホイール回転数
                hasCadence = (flags & 0x01) != 0;

                // ビット[1]がクランクケイデンス
                hasSpeed = (flags & (0x01 << 1)) != 0;
            }

            int offset = 1;

            // スピードセンサーチェック
            if (hasSpeed) {
                int revolutions = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, offset);
                offset += 4;

                int timestamp = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, offset);
                offset += 2;

                mWheelValue = RawSensorValue.nextValue(mWheelValue, revolutions, timestamp);
                BleLog.gatt("wheel revolutions(%d)  timestamp(%d) RPM(%.1f)", revolutions, timestamp, mWheelValue.getRpm());
            }

            // ケイデンスセンサーチェック
            if (hasCadence) {
                int revolutions = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, offset);
                offset += 4;

                int timestamp = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, offset);
                offset += 2;

                mCrankValue = RawSensorValue.nextValue(mCrankValue, revolutions, timestamp);
                BleLog.gatt("crank revolutions(%d)  timestamp(%d) RPM(%.1f)", revolutions, timestamp, mWheelValue.getRpm());
            }
        } else if (characteristic.getUuid().equals(BluetoothLeUtil.BLE_UUID_BATTERY_DATA_LEVEL)) {
            boolean notifyRequest = mBatteryLevel == null;
            mBatteryLevel = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
            BleLog.gatt("SC Sensor Battery[%d]", mBatteryLevel);
            onUpdateBatteryLevel(mBatteryLevel);

            if (notifyRequest && !gatt.requestNotification(BluetoothLeUtil.BLE_UUID_SPEED_AND_CADENCE_SERVICE, BluetoothLeUtil.BLE_UUID_SPEED_AND_CADENCE_MEASUREMENT)) {
                throw new BluetoothGattConnectFailedException("Speed&Cadence Not Found...");
            }
        }
    }

    /**
     * バッテリー残量が更新された
     */
    protected void onUpdateBatteryLevel(int newLevel) {
    }

    /**
     * クランク回転情報が更新された
     */
    protected void onUpdateCrankValue(RawSensorValue newValue) {
    }

    /**
     * ホイール回転情報が更新された
     */
    protected void onUpdateWheelValue(RawSensorValue newValue) {
    }
}
