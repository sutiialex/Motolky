/*
Copyright (c) 2013, Alexandru Sutii
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name of the author nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

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
 * server socket. Every time a connection is accepted, it creates a peer
 * for that connection. This peer does not reconnect. It closes when
 * its communication socket closes.
 */
public class ServerThread extends Thread implements IConnectable {

    private BluetoothAdapter mBtAdapter;
    private boolean mExit = false;
    private BluetoothServerSocket serverSocket = null;
    private RecordThread mRecordThread = null;
    private Peer mPeer = null;
    private Device mRemoteDevice = null;
    private IConnectNotifiable mConnectNotifiable = null;

    /**
     * Constructor
     * @param recordThread - the thread that is recording from the microphone
     * @param bluetoothAdapter
     * @param remoteDevice - the device from which connections will be listened
     * @param connectNotifiable - object that will be notified when a connection
     *          was created or destroyed
     */
    public ServerThread(RecordThread recordThread,
                        BluetoothAdapter bluetoothAdapter,
                        Device remoteDevice,
                        IConnectNotifiable connectNotifiable) {
        mBtAdapter = bluetoothAdapter;
        mRemoteDevice = remoteDevice;
        mRecordThread = recordThread;
        mConnectNotifiable = connectNotifiable;
    }

    /**
     * End the thread. Close the server socket and remove the peer if it exists.
     */
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

    /**
     * Runs in a loop. At each step accepts a connection and from the remote
     * device and creates a peer for this connection.
     */
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

    /**
     * Normally the peer calls this method in order to get a new socket after
     * the connection failed. However, the server cannot do that because it can only
     * wait for other connections. That's why a null is returned.
     */
    @Override
    public BluetoothSocket getSocket(Peer peer, Device device) throws IOException {
        return null;
    }
}
