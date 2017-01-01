package com.eaglesakura.android.bluetooth.gatt;

import com.eaglesakura.android.bluetooth.BleLog;
import com.eaglesakura.android.bluetooth.BluetoothLeUtil;
import com.eaglesakura.android.bluetooth.error.BluetoothException;
import com.eaglesakura.android.bluetooth.error.BluetoothGattConnectFailedException;
import com.eaglesakura.android.bluetooth.gatt.scs.RawSensorValue;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothGattCharacteristic;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

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
     * 指定個数キャッシュの最初と最後を比較する
     *
     * デフォルトは3つ
     */
    private int mCacheNum = 5;

    /**
     * クランク値キャッシュ
     * 回転数・経過時間
     */
    private List<RawSensorValue> mCrankValueList = new ArrayList<>();

    /**
     * ホイール値キャッシュ
     *
     * 回転数・経過時間
     */
    private List<RawSensorValue> mWheelValueList = new ArrayList<>();

    @Override
    public void onGattConnected(BleDeviceConnection self, BleGattController gatt) throws BluetoothException {
        // バッテリーレベルの読み込みを行う
        if (mBatteryLevel == null && gatt.requestRead(BluetoothLeUtil.BLE_UUID_BATTERY_SERVICE, BluetoothLeUtil.BLE_UUID_BATTERY_DATA_LEVEL)) {
            return;
        }

        // データ接続を行う
        if (gatt.requestNotification(BluetoothLeUtil.BLE_UUID_SPEED_AND_CADENCE_SERVICE, BluetoothLeUtil.BLE_UUID_SPEED_AND_CADENCE_MEASUREMENT)) {
            return;
        }

        throw new BluetoothGattConnectFailedException("Speed&Cadence Not Found...");
    }

    public Integer getBatteryLevel() {
        return mBatteryLevel;
    }

    private double getRpm(List<RawSensorValue> values) {
        synchronized (values) {
            if (values.size() < 2) {
                // 平均を測れない
                return 0.0;
            } else {
                // 最初と末尾から計算する
                return values.get(0).getRpm(values.get(values.size() - 1));
            }
        }
    }

    /**
     * 新しいデータを先頭に追加し、不要なデータを削除する。
     */
    private void addValue(List<RawSensorValue> values, RawSensorValue newValue) {
        synchronized (values) {
            values.add(0, newValue);
            if (values.size() > mCacheNum) {
                values.remove(values.size() - 1);
            }
        }
    }

    /**
     * クランク回転速度を取得する
     */
    public double getCrankRpm() {
        return getRpm(mCrankValueList);
    }

    /**
     * ホイール回転速度を取得する
     */
    public double getWheelRpm() {
        return getRpm(mWheelValueList);
    }

    @Nullable
    public RawSensorValue getCrankValue() {
        synchronized (mCrankValueList) {
            if (mCrankValueList.isEmpty()) {
                return null;
            } else {
                return mCrankValueList.get(0);
            }
        }
    }

    @Nullable
    public RawSensorValue getWheelValue() {
        synchronized (mWheelValueList) {
            if (mWheelValueList.isEmpty()) {
                return null;
            } else {
                return mWheelValueList.get(0);
            }
        }
    }

    /**
     * 現在速度を取得する
     *
     * @param wheelOuterLengthMM ミリメートル単位のホイール周長
     * @return 取得できない場合は0.0, 取得できている場合は時速を返却する
     */
    public double getSpeedKmPerHour(double wheelOuterLengthMM) {
        return calcSpeedKmPerHour(getWheelRpm(), wheelOuterLengthMM);
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
                Integer flags = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                if (flags != null) {
                    // ビット[0]がホイール回転数
                    hasCadence = (flags & 0x01) != 0;

                    // ビット[1]がクランクケイデンス
                    hasSpeed = (flags & (0x01 << 1)) != 0;
                } else {
                    hasCadence = false;
                    hasSpeed = false;
                }
            }

            int offset = 1;

            // スピードセンサーチェック
            if (hasSpeed) {
                Integer revolutions = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, offset);
                offset += 4;

                Integer timestamp = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, offset);
                offset += 2;

                if (revolutions != null && timestamp != null) {
                    RawSensorValue newValue = RawSensorValue.nextValue(getWheelValue(), revolutions, timestamp);
                    addValue(mWheelValueList, newValue);
                    onUpdateWheelValue(newValue, getWheelRpm());

                    BleLog.gatt("wheel revolutions(%d)  timestamp(%d) RAW RPM(%.1f) AVG RPM(%.1f)", revolutions, timestamp, newValue.getRpm(), getWheelRpm());
                }
            }

            // ケイデンスセンサーチェック
            if (hasCadence) {
                Integer revolutions = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, offset);
                offset += 2;

                Integer timestamp = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, offset);
                offset += 2;

                if (revolutions != null && timestamp != null) {
                    RawSensorValue newValue = RawSensorValue.nextValue(getCrankValue(), revolutions, timestamp);
                    addValue(mCrankValueList, newValue);

                    onUpdateCrankValue(newValue, getCrankRpm());
                    BleLog.gatt("crank revolutions(%d)  timestamp(%d) RAW RPM(%.1f) AVG RPM(%.1f)", revolutions, timestamp, newValue.getRpm(), getCrankRpm());
                }
            }
        } else if (characteristic.getUuid().equals(BluetoothLeUtil.BLE_UUID_BATTERY_DATA_LEVEL)) {
            Integer newBatteryLevel = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
            BleLog.gatt("S&C Monitor Battery[%d]", newBatteryLevel);
            if (newBatteryLevel != null) {
                boolean notifyRequest = mBatteryLevel == null;
                mBatteryLevel = newBatteryLevel;
                onUpdateBatteryLevel(newBatteryLevel);
                if (notifyRequest && !gatt.requestNotification(BluetoothLeUtil.BLE_UUID_SPEED_AND_CADENCE_SERVICE, BluetoothLeUtil.BLE_UUID_SPEED_AND_CADENCE_MEASUREMENT)) {
                    throw new BluetoothGattConnectFailedException("S&C Not Found...");
                }
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
     *
     * @param newValue 最新値
     * @param crankRpm 平均値
     */
    protected void onUpdateCrankValue(RawSensorValue newValue, double crankRpm) {
    }

    /**
     * ホイール回転情報が更新された
     *
     * @param newValue 最新値
     * @param wheelRpm 平均値
     */
    protected void onUpdateWheelValue(RawSensorValue newValue, double wheelRpm) {
    }
}
