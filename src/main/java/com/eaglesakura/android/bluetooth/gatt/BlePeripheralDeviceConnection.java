package com.eaglesakura.android.bluetooth.gatt;

import com.eaglesakura.android.bluetooth.error.BluetoothConnectAbortException;
import com.eaglesakura.android.bluetooth.error.BluetoothDataTimeoutException;
import com.eaglesakura.android.bluetooth.error.BluetoothException;
import com.eaglesakura.lambda.CallbackUtils;
import com.eaglesakura.lambda.CancelCallback;
import com.eaglesakura.thread.IntHolder;
import com.eaglesakura.util.Util;

import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;

/**
 * 心拍センサー等、常に接続し続けることを前提としたデバイス接続を管理する
 */
public class BlePeripheralDeviceConnection extends BleDeviceConnection {

    /**
     * 接続前にスリープさせる時間
     */
    private int mInitialSleepTimeMs = 1000 * 2;

    /**
     * 失敗時にスリープ時間を増やす
     */
    private float mBackoff = 1.5f;

    /**
     * 特定時間内にデータが到達しなければタイムアウトとしてみなす
     */
    private int mDataTimeoutMs = 1000 * 60;

    /**
     * 機器検索の最大スリープ時間
     */
    private int mMaxConnectSleepTimeMs = 1000 * 10;

    public BlePeripheralDeviceConnection(Context context, String deviceAddress) {
        super(context, deviceAddress);
    }

    /**
     * データ取得のタイムアウトを設定する
     *
     * タイムアウトを超えた場合、一旦デバイスから切断して再接続を行う
     */
    public void setDataTimeoutMs(int dataTimeoutMs) {
        mDataTimeoutMs = dataTimeoutMs;
    }

    /**
     * 最大スリープ時間
     * 接続失敗後、この時間だけスリープして再接続を行なう。
     */
    public void setMaxConnectSleepTimeMs(int maxConnectSleepTimeMs) {
        mMaxConnectSleepTimeMs = maxConnectSleepTimeMs;
    }

    /**
     * @param sessionCallback セッション情報ごとのコールバック
     * @param bleCallback     接続中のBLEデータ取得用コールバック
     * @param cancelCallback  キャンセルチェック
     */
    public void alwaysConnect(SessionCallback sessionCallback, Callback bleCallback, CancelCallback cancelCallback) throws BluetoothException {
        int tryCount = 0;
        IntHolder sleepTimeMs = new IntHolder(mInitialSleepTimeMs);
        while (true) {
            Session session = new Session(tryCount++);
            try {
                sessionCallback.onSessionStart(this, session);

                // 接続待ちスリープを行う
                for (int i = 0; i < sleepTimeMs.value; ++i) {
                    if (CallbackUtils.isCanceled(cancelCallback)) {
                        throw new BluetoothConnectAbortException("Device Sleep abort.");
                    }
                    Util.sleep(1);
                }

                // キャンセルチェックを行う
                CancelCallback connectAbortCallback = new CancelCallback() {
                    /**
                     * 最大時間まで待ち受ける
                     */
                    long mAbortTime = System.currentTimeMillis() + mMaxConnectSleepTimeMs;

                    @Override
                    public boolean isCanceled() throws Throwable {
                        // GATT接続される前にタイムアウト時刻を過ぎたらabortさせる
                        if (!session.mGattConnected) {
                            if (System.currentTimeMillis() >= mAbortTime) {
                                return true;
                            }
                        }
                        // 通常のキャンセルチェックを行わせる
                        return CallbackUtils.isCanceled(cancelCallback);
                    }
                };

                super.connect(new Callback() {
                    /**
                     * データが到達しない場合のタイムアウト時刻
                     */
                    long mAbortTime = System.currentTimeMillis() + mDataTimeoutMs;

                    @Override
                    public void onGattConnected(BleDeviceConnection self, BleGattController gatt) throws BluetoothException {
                        sleepTimeMs.value = mInitialSleepTimeMs;
                        mAbortTime = System.currentTimeMillis() + mDataTimeoutMs;
                        session.mGattConnected = true;

                        bleCallback.onGattConnected(self, gatt);
                    }

                    @Override
                    public boolean onLoop(BleDeviceConnection self, BleGattController gatt) throws BluetoothException {
                        if (System.currentTimeMillis() >= mAbortTime) {
                            throw new BluetoothDataTimeoutException("DataTimeout [" + mDataTimeoutMs + " ms]");
                        }

                        return bleCallback.onLoop(self, gatt);
                    }

                    @Override
                    public void onCharacteristicUpdated(BleDeviceConnection self, BleGattController gatt, BluetoothGattCharacteristic characteristic) throws BluetoothException {
                        // データが到達したのでタイムアウト時刻を引き伸ばす
                        mAbortTime = (System.currentTimeMillis() + mDataTimeoutMs);

                        // データ更新を行わせる
                        bleCallback.onCharacteristicUpdated(self, gatt, characteristic);
                    }
                }, connectAbortCallback);

                // 通常の方法(onLoop() -> false)で抜けているため、ここで無限ループを終了
                return;
            } catch (BluetoothConnectAbortException e) {
                // 使用者がキャンセルを求めているならばキャンセルで確定させる
                if (CallbackUtils.isCanceled(cancelCallback)) {
                    throw e;
                }
            } finally {
                // 接続に失敗したのでスリープ時間を伸ばす
                sleepTimeMs.value = Math.min((int) (mBackoff * sleepTimeMs.value), mMaxConnectSleepTimeMs);
                session.mGattDisconnected = true;
                sessionCallback.onSessionFinished(this, session);
            }
        }
    }

    @Override
    public void connect(Callback callback, CancelCallback cancelCallback) throws BluetoothException {
        throw new Error("Call alwaysConnect();");
    }

    public static class Session {
        private int mTryCount;

        /**
         * GATT接続が完了していればtrue
         */
        boolean mGattConnected;

        /**
         * GATTが切断された
         */
        boolean mGattDisconnected;

        Session(int tryCount) {
            mTryCount = tryCount;
        }

        public int getTryCount() {
            return mTryCount;
        }

        public boolean isGattConnected() {
            return mGattConnected;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Session session = (Session) o;

            return mTryCount == session.mTryCount;

        }

        @Override
        public int hashCode() {
            return mTryCount;
        }

        @Override
        public String toString() {
            return "Session ID[" + mTryCount + "]" +
                    " GATT CONNECT[" + mGattConnected + "]" +
                    " DISCONNECT[" + mGattDisconnected + "]"
                    ;
        }
    }

    public interface SessionCallback {
        /**
         * 接続セッションを開始する
         */
        void onSessionStart(BlePeripheralDeviceConnection self, Session session);

        /**
         * 接続セッションが終了した
         */
        void onSessionFinished(BlePeripheralDeviceConnection self, Session session);
    }
}
