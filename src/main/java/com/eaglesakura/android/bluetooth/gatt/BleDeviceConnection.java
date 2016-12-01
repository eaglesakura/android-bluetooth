package com.eaglesakura.android.bluetooth.gatt;

import com.eaglesakura.android.bluetooth.BleLog;
import com.eaglesakura.android.bluetooth.BluetoothLeUtil;
import com.eaglesakura.android.bluetooth.error.BluetoothConnectAbortException;
import com.eaglesakura.android.bluetooth.error.BluetoothException;
import com.eaglesakura.android.bluetooth.error.BluetoothGattConnectFailedException;
import com.eaglesakura.android.bluetooth.error.BluetoothGattDisconnectedException;
import com.eaglesakura.android.util.AndroidThreadUtil;
import com.eaglesakura.lambda.CallbackUtils;
import com.eaglesakura.lambda.CancelCallback;
import com.eaglesakura.util.CollectionUtil;
import com.eaglesakura.util.Util;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothProfile;
import android.content.Context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * BLEデバイス通信をサポートするクラス
 */
@TargetApi(18)
public class BleDeviceConnection {
    /**
     * 接続先のデバイスアドレス
     */
    private final String mDeviceAddress;

    /**
     * Context
     */
    private final Context mContext;

    public BleDeviceConnection(Context context, String deviceAddress) {
        mDeviceAddress = deviceAddress;
        mContext = context;
    }

    private void assertNotCanceled(CancelCallback cancelCallback) throws BluetoothConnectAbortException {
        if (CallbackUtils.isCanceled(cancelCallback)) {
            throw new BluetoothConnectAbortException("Canceled");
        }
    }

    /**
     * 接続を行う
     *
     * @param callback       制御コールバック
     * @param cancelCallback キャンセル設定コールバック
     */
    public void connect(Callback callback, CancelCallback cancelCallback) throws BluetoothException {
        AndroidThreadUtil.assertBackgroundThread();

        BluetoothGattCallbackImpl gattCallback = new BluetoothGattCallbackImpl();

        gattCallback.findRemoteDevice();
        gattCallback.awaitGattConnected(cancelCallback);
        callback.onGattConnected(this, gattCallback);

        try {
            while (true) {
                // 接続状態チェックを行う
                if (!gattCallback.isConnected()) {
                    throw new BluetoothGattDisconnectedException("Gatt Disconnected");
                }

                // ループ処理を行う
                List<BluetoothGattCharacteristic> values = gattCallback.popValues();
                if (!CollectionUtil.isEmpty(values)) {
                    // 更新を伝える
                    for (BluetoothGattCharacteristic characteristic : values) {
                        callback.onCharacteristicUpdated(this, gattCallback, characteristic);
                    }
                }

                if (callback.onLoop(this, gattCallback)) {
                    BleLog.system("CallbackLoop abort[%s]", mDeviceAddress);
                    return;
                }

                assertNotCanceled(cancelCallback);
                Util.sleep(1);
            }
        } finally {
            // 後片付けは必ず行う
            gattCallback.dispose();
        }
    }

    private class BluetoothGattCallbackImpl extends BluetoothGattCallback implements BleGattController {

        /**
         * 受信したデータのキャッシュ
         */
        private Map<UUID, BluetoothGattCharacteristic> mValues = new HashMap<>();

        /**
         * 接続対象デバイス
         */
        BluetoothDevice mDevice;

        /**
         * 接続中のGATT
         */
        BluetoothGatt mGatt;

        /**
         * デバイスの接続状況
         */
        Integer mConnectionState;

        /**
         * GATTの現在の接続状態
         */
        Integer mGattState;

        /**
         * 削除フラグ
         */
        boolean mDestroy;

        Set<BluetoothGattCharacteristic> mNotificationServices = new HashSet<>();

        /**
         * デバイスを検索する
         */
        public void findRemoteDevice() throws BluetoothException {
            // BLE接続チェック
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            mDevice = adapter.getRemoteDevice(mDeviceAddress);

            // デバイス接続の確認
            if (!adapter.isEnabled() || mDevice == null) {
                throw new BluetoothGattConnectFailedException("Device Error");
            }
        }

        /**
         * デバイスの接続待ちを行う
         */
        public void awaitGattConnected(CancelCallback cancelCallback) throws BluetoothException {
            mDevice.connectGatt(mContext, false, this);

            while (!isConnected()) {
                try {
                    assertNotCanceled(cancelCallback);

                    if (mGattState != null && mGattState != BluetoothGatt.GATT_SUCCESS) {
                        throw new BluetoothGattConnectFailedException("Discover services Failed");
                    }

                } catch (BluetoothException e) {
                    mDestroy = true;
                    dispose();
                    throw e;
                }
            }
        }

        /**
         * 読み取り可能な状態になった
         */
        public boolean isConnected() {
            return mGatt != null &&
                    mGattState != null && mGattState == BluetoothGatt.GATT_SUCCESS &&
                    mConnectionState != null && mConnectionState == BluetoothProfile.STATE_CONNECTED;
        }

        /**
         * 最後に受信した値を取得する
         *
         * @param uuid 対象のUUID値
         */
        public BluetoothGattCharacteristic popValue(UUID uuid) {
            synchronized (this) {
                return mValues.remove(uuid);
            }
        }

        /**
         * 最後に受信した値をすべて返す
         */
        public List<BluetoothGattCharacteristic> popValues() {
            synchronized (this) {
                if (mValues.isEmpty()) {
                    return null;
                }
                try {
                    return new ArrayList<>(mValues.values());
                } finally {
                    mValues.clear();
                }
            }
        }

        public void dispose() {
            synchronized (this) {
                if (mGatt != null) {
                    for (BluetoothGattCharacteristic characteristic : mNotificationServices) {
                        BluetoothLeUtil.notificationDisable(mGatt, characteristic);
                    }

                    mGatt.disconnect();
                    mGatt.close();
                    mGatt = null;
                }
            }
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            synchronized (this) {
                mConnectionState = newState;
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    mGatt = gatt;
                    BleLog.gatt("onConnectionStateChange connected[%s] address[%s]", mDevice.getName(), mDeviceAddress);

                    if (mDestroy) {
                        dispose();
                    } else {
                        mGatt.discoverServices();
                    }
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    BleLog.gatt("onConnectionStateChange disconnected[%s]  address[%s]", mDevice.getName(), mDeviceAddress);
                }
            }
        }


        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (mDestroy) {
                dispose();
                return;
            }

            mGattState = status;
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            synchronized (this) {
                mValues.put(characteristic.getUuid(), characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            synchronized (this) {
                mValues.put(characteristic.getUuid(), characteristic);
            }
        }

        @Override
        public BluetoothGatt getGatt() {
            return mGatt;
        }

        @Override
        public boolean requestRead(UUID serviceUuid, UUID characteristicUuid) {
            synchronized (this) {
                BluetoothGattCharacteristic characteristic = BluetoothLeUtil.findBluetoothGattCharacteristic(mGatt, serviceUuid, characteristicUuid);
                if (characteristic != null) {
                    mGatt.readCharacteristic(characteristic);
                    return true;
                } else {
                    return false;
                }
            }
        }

        @Override
        public boolean requestNotification(UUID serviceUuid, UUID characteristicUuid) {
            synchronized (this) {
                BluetoothGattCharacteristic characteristic = BluetoothLeUtil.findBluetoothGattCharacteristic(mGatt, serviceUuid, characteristicUuid);
                if (characteristic != null) {
                    BluetoothLeUtil.notificationEnable(mGatt, characteristic);
                    mNotificationServices.add(characteristic);
                    return true;
                } else {
                    return false;
                }
            }
        }
    }

    public interface Callback {
        /**
         * GATT接続に成功した
         */
        void onGattConnected(BleDeviceConnection self, BleGattController gatt) throws BluetoothException;

        /**
         * 制御ループ処理を行う
         *
         * ループを終了する場合はtrueを返却する
         */
        boolean onLoop(BleDeviceConnection self, BleGattController gatt) throws BluetoothException;

        /**
         * Characteristicが更新された
         *
         * @param characteristic 対象データ
         */
        void onCharacteristicUpdated(BleDeviceConnection self, BleGattController gatt, BluetoothGattCharacteristic characteristic) throws BluetoothException;
    }
}
