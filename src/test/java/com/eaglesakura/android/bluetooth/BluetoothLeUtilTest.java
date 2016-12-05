package com.eaglesakura.android.bluetooth;

import org.junit.Test;

public class BluetoothLeUtilTest extends UnitTestCase {

    @Test
    public void _16bit加算値が正常に差分取得できることを確認する() throws Exception {
        assertEquals(BluetoothLeUtil.get16bitOffset(0, 1), 1);
        assertEquals(BluetoothLeUtil.get16bitOffset(0xFFFE, 0), 2);
        assertEquals(BluetoothLeUtil.get16bitOffset(0xFFFF, 0x00010000), 1);
        assertEquals(BluetoothLeUtil.get16bitOffset(1, 0x007FFFF), (0xFFFF - 1));
        assertEquals(BluetoothLeUtil.get16bitOffset(0, 0x1234FFFF), 0xFFFF);

        // 適当な回数だけ加算し、ループを含めて正常に差分値が取得できることを確認する
        int time = 0;
        while (time < (0xFFFF * 2)) {
            int diff = (int) (10.0 + Math.random() * 1000);
            int oldTime = time;
            time += diff;

            assertTrue(BluetoothLeUtil.get16bitOffset(oldTime, time) > 0);
            assertEquals(BluetoothLeUtil.get16bitOffset(oldTime, time), diff);
        }
    }

    @Test
    public void 低精度時計が秒に変換できることを確認する() throws Exception {
        assertEquals(BluetoothLeUtil.sensorTimeToSeconds(1024), 1.0, 0.0001);
        assertEquals(BluetoothLeUtil.sensorTimeToSeconds(1024 * 60), 60.0, 0.0001);
        assertEquals(BluetoothLeUtil.sensorTimeToSeconds(1024 / 2), 0.5, 0.0001);
    }
}