package com.eaglesakura.android.bluetooth.p2p;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.util.UUID;

@Deprecated
public class BluetoothClient extends BluetoothP2PConnector {

    BluetoothSocket socket;

    public BluetoothClient(BluetoothDevice device) {
        this.connectDevice = device;
    }

    @Override
    protected boolean requestConnecting(UUID protocol) {
        try {

            socket = connectDevice.createRfcommSocketToServiceRecord(protocol);
            socket.connect();

            startInputThread(socket);
            startOutputThread(socket);

            return true;
        } catch (Exception ioe) {
            ioe.printStackTrace();
            return false;
        }

    }

    @Override
    protected void requestDisconnecting() {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            socket = null;
        }
    }

}
