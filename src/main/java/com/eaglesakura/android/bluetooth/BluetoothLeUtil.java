package com.eaglesakura.android.bluetooth;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;

import java.util.UUID;

@SuppressLint("NewApi")
public class BluetoothLeUtil {
    public static final String TAG = "ble.util";

    /**
     * config
     */
    public static final UUID BLE_UUID_CLIENT_CHARACTERISTIC_CONFIG = createUUIDFromAssignedNumber("0x2902");

    /**
     * ハートレートモニターのBLEサービスを示すUUID
     * <br>
     * 基本は0000XXXX-0000-1000-8000-00805f9b34fb
     * <br>
     * 参考：
     * https://developer.bluetooth.org/gatt/services/Pages/ServiceViewer.aspx?u=org.bluetooth.service.heart_rate.xml
     */
    public static final UUID BLE_UUID_HEARTRATE_SERVICE = BluetoothLeUtil.createUUIDFromAssignedNumber("0x180d");


    /**
     * 心拍値を示すUUID
     */
    public static final UUID BLE_UUID_HEARTRATE_DATA_MEASUREMENT = BluetoothLeUtil.createUUIDFromAssignedNumber("0x2a37");

    /**
     * スピード・ケイデンスセンサーのBLEデバイスを示すUUID
     * https://developer.bluetooth.org/gatt/services/Pages/ServiceViewer.aspx?u=org.bluetooth.service.cycling_speed_and_cadence.xml
     */
    public static final UUID BLE_UUID_SPEED_AND_CADENCE_SERVICE = BluetoothLeUtil.createUUIDFromAssignedNumber("0x1816");

    /**
     * スピード・ケイデンスセンサーの各種パラメーター取得
     * https://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.csc_measurement.xml
     */
    public static final UUID BLE_UUID_SPEED_AND_CADENCE_MEASUREMENT = BluetoothLeUtil.createUUIDFromAssignedNumber("0x2A5B");

    /**
     * バッテリーサービスを示すUUID
     *
     * 参考: http://stackoverflow.com/questions/19539535/how-to-get-the-battery-level-after-connect-to-the-ble-device
     */
    public static final UUID BLE_UUID_BATTERY_SERVICE = BluetoothLeUtil.createUUIDFromAssignedNumber("0x180f");

    /**
     * バッテリー残量を示すUUID
     */
    public static final UUID BLE_UUID_BATTERY_DATA_LEVEL = BluetoothLeUtil.createUUIDFromAssignedNumber("0x2a19");

    /**
     * developer.bluetooth.orgに示されるAssigned NumberからUUIDを生成する
     */
    public static UUID createUUIDFromAssignedNumber(String an) {
        if (an.startsWith("0x")) {
            an = an.substring(2);
        }

        return UUID.fromString(String.format("0000%s-0000-1000-8000-00805f9b34fb", an));
    }

    /**
     * 指定したCharacteristicを取得する
     */
    public static BluetoothGattCharacteristic findBluetoothGattCharacteristic(BluetoothGatt gatt, UUID serviceUuid, UUID characteristicUuid) {
        if (gatt == null) {
            return null;
        }
        BluetoothGattService service = gatt.getService(serviceUuid);
        if (service == null) {
            return null;
        }

        return service.getCharacteristic(characteristicUuid);
    }

    /**
     * 通知をONにする
     */
    public static void notificationEnable(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        gatt.setCharacteristicNotification(characteristic, true);

        // notificationを有効化する
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(BLE_UUID_CLIENT_CHARACTERISTIC_CONFIG);
        if (descriptor != null) {
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            gatt.writeDescriptor(descriptor);
        }
    }

    /**
     * 通知をOFFにする
     */
    public static void notificationDisable(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        gatt.setCharacteristicNotification(characteristic, false);

        // notificationを有効化する
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(BLE_UUID_CLIENT_CHARACTERISTIC_CONFIG);
        if (descriptor != null) {
            descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
            gatt.writeDescriptor(descriptor);
        }

    }


    /**
     * 16bitデータ用のマスク値
     */
    public static final int SENSOR_16BIT_MASK = 0x0000FFFF;

    /**
     * 16bit値がオーバーフローしていたらtrue
     *
     * @param oldValue 古い値
     * @param newValue 新しい値
     */
    public static boolean is16bitOverflow(int oldValue, int newValue) {
        return (oldValue & SENSOR_16BIT_MASK) > (newValue & SENSOR_16BIT_MASK);
    }

    /**
     * 差分を取得する
     *
     * 16bit循環でnewTime < oldTimeになった場合は内部で値を調整する
     *
     * @param oldValue 古い値
     * @param newValue 新しい値
     * @return 差分
     */
    public static int get16bitOffset(int oldValue, int newValue) {
        oldValue &= SENSOR_16BIT_MASK;
        newValue &= SENSOR_16BIT_MASK;

        if (newValue < oldValue) {
            // newTimeが循環していたら、値を1順進める
            newValue += (SENSOR_16BIT_MASK + 1);
        }
        return newValue - oldValue;
    }

    /**
     * 低精度時計データを秒に変換する
     * 1024 = 1.0秒となる。16bitが最大値のため、約65秒で1順する。
     */
    public static double sensorTimeToSeconds(int sensorTime) {
        sensorTime &= SENSOR_16BIT_MASK;
        return (double) sensorTime / 1024.0;
    }
}
