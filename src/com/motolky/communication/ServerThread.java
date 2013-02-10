package com.motolky.communication;

import java.io.IOException;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import com.motolky.Common;
import com.motolky.Peer;
import com.motolky.sound.RecordThread;

/**
 * This class is a thread that receives a remote device and
 * creates a server socket from it. It accepts connections on that
 * server socket. Whenever a connection is accepted, it creates a peer
 * for that connection. This peer does not reconnect. It closes when
 * the socket closes.
 *
 * @author Alexandru Sutii
 *
 */
public class ServerThread extends Thread implements IConnectable {

    private BluetoothAdapter mBtAdapter;
    private boolean mExit = false;
    private BluetoothServerSocket serverSocket = null;
    private RecordThread mRecordThread = null;
    private Peer mPeer = null;
    private Device mRemoteDevice = null;
    private IConnectNotifiable mConnectNotifiable = null;

    public ServerThread(RecordThread recordThread,
                        BluetoothAdapter bluetoothAdapter,
                        Device remoteDevice,
                        IConnectNotifiable connectNotifiable) {
        mBtAdapter = bluetoothAdapter;
        mRemoteDevice = remoteDevice;
        mRecordThread = recordThread;
        mConnectNotifiable = connectNotifiable;
    }

    public void exit() {
        mExit = true;
        if (mPeer != null) {
            mPeer.exit();
            mPeer = null;
        }

        try {
            if (serverSocket != null)
                serverSocket.close();
        } catch (IOException ioe) {
            Log.d(Common.TAG, "Failed to close the server socket.");
        }
    }

    @Override
    public void run() {
        UUID uuid = mRemoteDevice.getUUID();
        while (!mExit) {
            try {
                serverSocket = mBtAdapter.listenUsingRfcommWithServiceRecord(mBtAdapter.getName(), uuid);

                BluetoothSocket socket = serverSocket.accept();

                serverSocket.close();

                Log.d(Common.TAG, "Accepted a connection.");

                mPeer = new Peer(mRecordThread, this, mConnectNotifiable,
                                mRemoteDevice, socket, false);
                mPeer.start();

            } catch (IOException ioe) {
                Log.d(Common.TAG, "Accept error or closed.");
            }
        }
    }

    @Override
    public BluetoothSocket getSocket(Peer peer, Device device) throws IOException {
        return null;
    }
}
