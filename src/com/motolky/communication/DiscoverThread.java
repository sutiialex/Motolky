package com.motolky.communication;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.util.Log;

import com.motolky.Common;

/**
 * This class is a thread that once a 5 minutes makes the local device
 * discoverable and initializes device discovery every 12 seconds.
 *
 * @author Alexandru Sutii
 *
 */
public class DiscoverThread extends Thread {
    private boolean mStop = false;
    private Activity mActivity;
    private BluetoothAdapter mBluetoothAdapter;
    private long mBeginDiscoverableTime;
    private boolean mRemakeDiscoverable = false;
    private boolean mDiscoverRequestEnded = true;

    public DiscoverThread(Activity activity,
                          BluetoothAdapter bluetoothAdapter,
                          long exBeginDiscoverableTime) {
        mActivity = activity;
        mBluetoothAdapter = bluetoothAdapter;
        mBeginDiscoverableTime = exBeginDiscoverableTime;
    }

    public void stopDiscovery() {
        try {
            mBluetoothAdapter.cancelDiscovery();
        } catch (Exception e) {}
        mStop = true;
    }

    public long getExBeginDiscoverableTime() {
        return mBeginDiscoverableTime;
    }

    @Override
    public void run() {
        if (mBeginDiscoverableTime == -1)
            mBeginDiscoverableTime = System.currentTimeMillis() - Common.DISCOVERABLE_TIMEOUT*1000;
        while (!mStop) {
            // Enable bluetooth and make me discoverable
            long time = System.currentTimeMillis();
            if ((time - mBeginDiscoverableTime >= Common.DISCOVERABLE_TIMEOUT*1000 || mRemakeDiscoverable)
                && mDiscoverRequestEnded) {
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

    public void remakeDiscoverable() {
        mRemakeDiscoverable = true;
    }

    public void discoverRequestEnded() {
        mDiscoverRequestEnded = true;
    }
}
