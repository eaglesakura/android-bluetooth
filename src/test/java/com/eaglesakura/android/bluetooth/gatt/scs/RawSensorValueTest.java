package com.eaglesakura.android.bluetooth.gatt.scs;

import com.eaglesakura.android.bluetooth.BluetoothLeUtil;
import com.eaglesakura.android.bluetooth.UnitTestCase;

import org.junit.Test;

public class RawSensorValueTest extends UnitTestCase {

    @Test
    public void 想定通りの回転値が取得できることを確認する() throws Exception {
        // ランダムで結果が多少上下するので、サンプリング数を上げる
        for (int i = 0; i < 1024; ++i) {
            final long START_TIME = System.currentTimeMillis();

            long clock = START_TIME;


            float current = 0;  // 経過時間（分）
            double sensorTime = 12345;    // センサー時間
            double revolveCount = 1234; // 合計回転数

            int sumUpdated = 0;
            double sumRpm = 0;
            final double SAMPLE_RPM = 200.0;

            RawSensorValue oldValue = null;

            while (current < 60.0) {
                final double OFFSET_MINUTE = (1.0 / 60.0 * (1.5 + Math.random())); // 2.5秒以内の適当なインターバル秒でデータが飛んできていることにする
                clock += ((long) (OFFSET_MINUTE * 60.0 * 1000.0));
                sensorTime += (OFFSET_MINUTE * 60.0 * 1024.0);
                revolveCount += (SAMPLE_RPM * OFFSET_MINUTE);
                current += OFFSET_MINUTE;

                // データを流してみる
                RawSensorValue newValue = RawSensorValue.nextValue(oldValue, ((int) revolveCount) & BluetoothLeUtil.SENSOR_16BIT_MASK, ((int) sensorTime) & BluetoothLeUtil.SENSOR_16BIT_MASK);
                if (oldValue != null) {
                    // 合計回転数が上がらなければならない
                    validate(newValue.mValueSum).from(oldValue.mValueSum + 1);
                    // 誤差があるので適当に上下1割は許容する
                    validate(newValue.getRpm())
                            .from(SAMPLE_RPM * 0.8)
                            .to(SAMPLE_RPM * 1.2);
                }

                // 平均計算用
                ++sumUpdated;
                sumRpm += newValue.getRpm();

                assertNotNull(newValue);
                oldValue = newValue;
            }

            // 平均値は誤差を小さく見積もるする
            final double AVG_RPM = (sumRpm / sumUpdated);
            validate(AVG_RPM).from(SAMPLE_RPM * 0.99).to(SAMPLE_RPM * 1.01);
        }
    }
}