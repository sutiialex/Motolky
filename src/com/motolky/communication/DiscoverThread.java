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

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.util.Log;

import com.motolky.Common;

/**
 * This class is a thread that once a 5 minutes makes the local device
 * discoverable and initializes device discovery every 12 seconds.
 */
public class DiscoverThread extends Thread {
    private boolean mStop = false;
    private Activity mActivity;
    private BluetoothAdapter mBluetoothAdapter;
    private long mBeginDiscoverableTime;
    private boolean mRemakeDiscoverable = false;
    private boolean mDiscoverRequestEnded = true;

    /**
     * Constructor
     * @param activity - the activity that created this thread
     * @param bluetoothAdapter - the local bluetooth adapter
     * @param exBeginDiscoverableTime - the last time the adapter was made discoverable
     */
    public DiscoverThread(Activity activity,
                          BluetoothAdapter bluetoothAdapter,
                          long exBeginDiscoverableTime) {
        mActivity = activity;
        mBluetoothAdapter = bluetoothAdapter;
        mBeginDiscoverableTime = exBeginDiscoverableTime;
    }

    /**
     * Stops the thread
     */
    public void stopDiscovery() {
        try {
            mBluetoothAdapter.cancelDiscovery();
        } catch (Exception e) {}
        mStop = true;
    }

    public long getExBeginDiscoverableTime() {
        return mBeginDiscoverableTime;
    }

    /**
     * The thread runs in a loop. It makes sure that the phone is always discoverable
     * during the group selection phase. Moreover, it makes sure to continuously look
     * for devices in its range.
     */
    @Override
    public void run() {
        if (mBeginDiscoverableTime == -1)
            mBeginDiscoverableTime = System.currentTimeMillis() - Common.DISCOVERABLE_TIMEOUT*1000;

        while (!mStop) {
            // Enable bluetooth and make the local device discoverable
            long time = System.currentTimeMillis();
            if ((time - mBeginDiscoverableTime >= Common.DISCOVERABLE_TIMEOUT*1000 ||
            		mRemakeDiscoverable) && mDiscoverRequestEnded) {
                mRemakeDiscoverable = false;
                mDiscoverRequestEnded = false;

                Log.d(Common.TAG, "Intent to make device discoverable");

                Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 400);
                mActivity.startActivityForResult(discoverableIntent, Common.REQUEST_ENABLE_DISCOVERY);
                mBeginDiscoverableTime = time;
            }

            // Discover other devices
            if (!mBluetoothAdapter.startDiscovery()) {
                Log.e(Common.TAG, "Could not start discovery");
            } else {
                Log.d(Common.TAG, "Initiated device discovery.");
            }

            try {
                sleep(12000);
            } catch (Exception e) {}
        }
    }

    /**
     * Forcefully make the phone discoverable
     */
    public void remakeDiscoverable() {
        mRemakeDiscoverable = true;
    }

    /**
     * Notify that the discovery intent has been fulfilled
     */
    public void discoverRequestEnded() {
        mDiscoverRequestEnded = true;
    }
}
