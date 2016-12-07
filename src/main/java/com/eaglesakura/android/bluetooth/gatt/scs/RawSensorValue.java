package com.eaglesakura.android.bluetooth.gatt.scs;

import com.eaglesakura.android.bluetooth.BluetoothLeUtil;

import android.support.annotation.Nullable;

/**
 * Speed/Cadenceセンサーの取得値そのものを管理する
 */
public class RawSensorValue {

    /**
     * 今回取得の生データ
     */
    final int mValueRaw;

    /**
     * 前回の取得からの差分値
     */
    final int mValueOffset;

    /**
     * 生データ合計値
     */
    final int mValueSum;

    /**
     * センサーから直接取得したタイムスタンプ値
     * 1/1024秒単位で管理される時刻
     * 1024 = 1.0秒となる。
     */
    final int mTimestampRaw;

    /**
     * 前のセンサー時刻からのオフセット
     * 1/1024秒単位で管理される時刻
     * 1024 = 1.0秒となる。
     */
    final int mTimestampOffset;

    /**
     * 1/1024秒単位で管理される時刻
     * 1024 = 1.0秒となる。
     */
    final int mTimestampSum;

    /**
     * 受信時刻
     */
    final long mSystemTimestamp = System.currentTimeMillis();

    RawSensorValue(int value, int timestamp) {
        mValueRaw = value;
        mValueOffset = 0;
        mValueSum = value;

        mTimestampRaw = timestamp;
        mTimestampOffset = 0;
        mTimestampSum = timestamp;
    }

    RawSensorValue(int valueRaw, int valueOffset, int valueSum, int timestampRaw, int timestampOffset, int timestampSum) {
        mValueOffset = valueOffset;
        mValueSum = valueSum;
        mValueRaw = valueRaw;
        mTimestampRaw = timestampRaw;
        mTimestampOffset = timestampOffset;
        mTimestampSum = timestampSum;
    }

    /**
     * 前回取得値からの差分を取得する
     */
    public int getValueOffset() {
        return mValueOffset;
    }

    public int getValueSum() {
        return mValueSum;
    }

    /**
     * オフセット値を60秒換算して取得する
     */
    public double getRpm() {
        if (mTimestampOffset == 0) {
            return 0;
        }
        final double mult = 60.0 / getOffsetTimeSec();

        // 指定時間に行われた回転数から、1分間の回転数を求める
        return (double) getValueOffset() * mult;
    }

    /**
     * 適当な古い値からの差分で計算する
     *
     * 2点間計算よりも正確になる
     */
    public double getRpm(RawSensorValue oldValue) {

        int timeOffset = (mTimestampSum - oldValue.mTimestampSum);
        double timeOffsetSec = (double) timeOffset / 1024.0;
        if (timeOffset == 0) {
            timeOffset = (int) (mSystemTimestamp - oldValue.mSystemTimestamp);
            if (timeOffset == 0) {
                // リアルタイムでも変わりない
                return 0;
            }
            timeOffsetSec = (double) timeOffset / 1000.0;
        }

        final double valueOffset = (mValueSum - oldValue.mValueSum);

        final double mult = 60.0 / timeOffsetSec;

        // 指定時間に行われた回転数から、1分間の回転数を求める
        return valueOffset * mult;
    }

    /**
     * 前のセンサー取得からのオフセット秒を取得する
     */
    public double getOffsetTimeSec() {
        return (double) mTimestampOffset / 1024.0;
    }

    /**
     * 実際の時刻を取得する
     */
    public long getSystemTimestamp() {
        return mSystemTimestamp;
    }

    @Override
    public String toString() {
        return "Value[" + mValueRaw + "] / Timestamp[" + mTimestampRaw + "]";
    }

    public static RawSensorValue nextValue(@Nullable RawSensorValue oldValue, int newValue, int newTimestamp) {
        if (oldValue == null) {
            return new RawSensorValue(newValue, newTimestamp);
        } else {
            int valueOffset = BluetoothLeUtil.get16bitOffset(oldValue.mValueRaw, newValue);
            int timeOffset = BluetoothLeUtil.get16bitOffset(oldValue.mTimestampRaw, newTimestamp);

            return new RawSensorValue(
                    newValue, valueOffset, oldValue.mValueSum + valueOffset,
                    newTimestamp, timeOffset, oldValue.mTimestampSum + timeOffset
            );
        }
    }
}
