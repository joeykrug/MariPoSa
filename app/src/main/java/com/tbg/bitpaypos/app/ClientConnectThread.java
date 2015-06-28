package com.tbg.bitpaypos.app;


import java.io.IOException;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

/**
 * This class is used for connecting to existing PoS over bluetooth/serial
 */
public class ClientConnectThread extends Thread {
    private static final String DEBUG_TAG = "Bluetooth";
    private final BluetoothDevice mRemoteDevice;
    private final BluetoothSocket clientSocket;
    private BluetoothSocket activeBluetoothSocket = null;

    private final Handler mHandler;

    public ClientConnectThread(BluetoothDevice remoteDevice, Handler handler) {
        mHandler = handler;
        mRemoteDevice = remoteDevice;
        BluetoothSocket clientSocket = null;

        try {
            final String status = "Status: Connecting";
            mHandler.obtainMessage(MainActivity.STATUS, status).sendToTarget();
            clientSocket = remoteDevice.createRfcommSocketToServiceRecord(MainActivity.BT_APP_UUID);
        } catch (IOException e) {
            Log.e(DEBUG_TAG, "Failed to open local client socket");
        }
        // finalize
        this.clientSocket = clientSocket;
    }

    public void run() {
        boolean success = false;
        try {
            // try to connect to the serial bluetooth socket
            clientSocket.connect();
            success = true;
        } catch (IOException e) {
            Log.e(DEBUG_TAG, "Client connect failed or cancelled");
            Log.d("Exception", e.toString());
            try {
                // close socket and "get out"
                clientSocket.close();
            } catch (IOException e1) {
                Log.e(DEBUG_TAG, "Failed to close socket on error", e);
            }
        }
        final String status;
        if (success) {
            status = "Status: Connected";
            activeBluetoothSocket = clientSocket;
        } else {
            status = "Status: Connection Failed";
            activeBluetoothSocket = null;
        }
        mHandler.post(new Runnable() {
            public void run() {
                mHandler.obtainMessage(MainActivity.STATUS, status).sendToTarget();
                Log.v(DEBUG_TAG,status);
                doStartDataCommThread();
            }
        });
    }

    public void stopConnecting() {
        try {
            clientSocket.close();
            final String status = "Status: Connection Closed";
            mHandler.obtainMessage(MainActivity.STATUS, status).sendToTarget();
        } catch (Exception e) {
            Log.e(DEBUG_TAG, "Failed to stop connecting", e);
        }
    }

    private void doStartDataCommThread() {
        if (activeBluetoothSocket == null) {
            Log.v(DEBUG_TAG,"Can't start datacomm");
            Log.w(DEBUG_TAG, "Something is wrong, shouldn't be trying to use datacomm when no socket");
        } else {
            Log.v(DEBUG_TAG, "Data comm thread starting");
            BluetoothDataCommThread bluetoothDataCommThread = new BluetoothDataCommThread(activeBluetoothSocket, mHandler);
            mHandler.obtainMessage(MainActivity.SOCKET_CONNECTED, bluetoothDataCommThread)
                    .sendToTarget();
            bluetoothDataCommThread.start();
        }
    }
}