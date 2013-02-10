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
import java.io.InputStream;
import java.io.OutputStream;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

import com.motolky.Common;
import com.motolky.Peer;

/**
 * This class is a thread that reads data from a given socket and
 * sends this data to a receive handler. Also it can receive data from a sender
 * and send that data through the socket. It throws exceptions whenever an
 * error occurs on the socket.
 */
public class CommunicationThread extends Thread implements ISendHandler {
    private int LAG_CUT_TIMEOUT = 200;
    private IReceiveHandler mReceiveHandler = null;
    private BluetoothSocket mSocket = null;
    private InputStream mInputStream = null;
    private OutputStream mOutputStream = null;
    private Peer mPeer = null;
    private boolean mStopped = false;

    /**
     * Constructor
     * @param receiveHandler - the data received from the socket will be sent to this object
     * @param socket - the socket to listen from
     * @param peer - this object needs to be notified when a communication error occurs
     */
    public CommunicationThread(IReceiveHandler receiveHandler,
                                BluetoothSocket socket,
                                Peer peer) {
        mReceiveHandler = receiveHandler;
        mSocket = socket;
        mPeer = peer;

        // Create the link to the socket
        try {
            mInputStream = mSocket.getInputStream();
            mOutputStream = mSocket.getOutputStream();
        } catch (IOException ioe) {
            Log.e(Common.TAG, "Error getting the input and the output stream: " + ioe.getMessage());
        }
    }

    /**
     * This method receives a buffer with data and sends it on the socket
     * @param buffer - where the data is
     * @param buffer - how many bytes of data to send from the buffer
     */
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

    /**
     * Stop the thread and close the communication channel
     */
    public void cancel() {
        mStopped = true;
        try {
            mSocket.close();
        } catch (IOException ioe) {
            Log.e(Common.TAG, "Error closing the socket: " + ioe.getMessage());
        }
    }

    /**
     * The thread continuously reads data from socket and sends it to the
     * notifiable object it received in the constructor
     */
    @Override
    public void run() {
        byte[] buffer = new byte[1024];
        int bytes;
        int times = 0;

        while (!mStopped) {
            try {
                if (times == 0) {
                    try {
                    	// Sometimes lag occurs. Therefore, from time to time
                    	// we just ignore a large chunk of data from input stream
                    	// in order to synchronize the communication.
                        mInputStream.skip(1024000);
                    } catch (Exception e) {}
                    times = LAG_CUT_TIMEOUT;
                }
                times--;

                // Read from socket and send to the handler
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
