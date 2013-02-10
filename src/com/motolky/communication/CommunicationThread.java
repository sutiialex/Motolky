package com.motolky.communication;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.motolky.Common;
import com.motolky.Peer;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

/**
 * This class is a thread that reads data from a given socket and
 * sends this data to a receive handler. Also it can receive data from a sender
 * and send that data through the socket. It throws exceptions whenever am
 * error occurs on the socket.
 *
 * @author Alexandru Sutii
 *
 */
public class CommunicationThread extends Thread implements ISendHandler {
    private int LAG_CUT_TIMEOUT = 200;
    private IReceiveHandler mReceiveHandler = null;
    private BluetoothSocket mSocket = null;
    private InputStream mInputStream = null;
    private OutputStream mOutputStream = null;
    private Peer mPeer = null;
    private boolean mStopped = false;

    public CommunicationThread(IReceiveHandler receiveHandler, BluetoothSocket socket, Peer peer) {
        mReceiveHandler = receiveHandler;
        mSocket = socket;
        mPeer = peer;
        try {
            mInputStream = mSocket.getInputStream();
            mOutputStream = mSocket.getOutputStream();
        } catch (IOException ioe) {
            Log.e(Common.TAG, "Error getting the input and the output stream: " + ioe.getMessage());
        }
    }

    @Override
    public void sendData(byte[] buffer, int bytes) {
        try {
            mOutputStream.write(buffer, 0, bytes);
        } catch (IOException ioe) {
            Log.e(Common.TAG, "Error sending data on socket: " + ioe.getMessage());
            cancel();
            mPeer.communicationErrorOccured();
        }
    }

    public void cancel() {
        mStopped = true;
        try {
            mSocket.close();
        } catch (IOException ioe) {
            Log.e(Common.TAG, "Error closing the socket: " + ioe.getMessage());
        }
    }

    @Override
    public void run() {
        byte[] buffer = new byte[1024];
        int bytes;
        int times = 0;

        while (!mStopped) {
            try {
                if (times == 0) {
                    try {
                        mInputStream.skip(1024000); // Remove the delay
                    } catch (Exception e) {}
                    times = LAG_CUT_TIMEOUT;
                }
                times--;

                bytes = mInputStream.read(buffer);
                mReceiveHandler.receiveData(buffer, bytes);
            } catch (IOException ioe) {
                Log.e(Common.TAG, "Error receiving from the socket: " + ioe.getMessage());
                mPeer.communicationErrorOccured();
                break;
            }
        }
    }

}
