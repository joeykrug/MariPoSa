package com.tbg.bitpaypos.app;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import android.os.SystemClock;


public class BluetoothDataCommThread extends Thread {
    private static final String DEBUG_TAG = "Bluetooth";

    private final BluetoothSocket dataSocket;
    private final OutputStream outData;
    private final InputStream inData;

    private final Handler mHandler;


    public BluetoothDataCommThread(BluetoothSocket dataSocket, Handler handler) {
        mHandler = handler;
        this.dataSocket = dataSocket;
        OutputStream outData = null;
        InputStream inData = null;
        try {
            outData = dataSocket.getOutputStream();
            inData = dataSocket.getInputStream();
        } catch (IOException e) {
            Log.e(DEBUG_TAG, "Failed to get iostream", e);
        }
        this.inData = inData;
        this.outData = outData;
    }

    public void run() {
        byte[] readBuffer = new byte[64];
        //byte ch;
        int readSize = 0;

        try {
            while (true) {
                readSize = inData.read(readBuffer);
                final String inStr = new String(readBuffer, 0, readSize);
                mHandler.post(new Runnable() {
                    public void run() {
                        //Log.v(DEBUG_TAG, inStr);
                        mHandler.obtainMessage(
                                MainActivity.DATA_RECEIVED,
                                inStr).sendToTarget();
                    }
                });

            }
        } catch (Exception e) {
            Log.e(DEBUG_TAG, "Socket failure or closed",e);
        }
    }

    public boolean send(String out) {
        boolean success = false;
        try {
            outData.write(out.getBytes(), 0, out.length());
            success = true;
        } catch (IOException e) {
            Log.e(DEBUG_TAG, "Failed to write to remote device", e);
            //show_message("Send failed");
        }
        return success;
    }

    public void disconnect() {
        try {
            dataSocket.close();
        } catch (Exception e) {
            Log.e(DEBUG_TAG, "Failed to close datacomm socket", e);
        }
    }


}
