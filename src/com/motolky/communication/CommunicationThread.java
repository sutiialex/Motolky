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
    * Neither the name of the <organization> nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

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
