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

package com.motolky;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.motolky.communication.Device;
import com.motolky.communication.DiscoverThread;

/**
 * This activity is the first one to be displayed to the user. It shows
 * the surrounding devices and it allows to choose the devices to talk to.
 *
 * It displays a list of the devices found in the phone's range.
 * A thread is created that once in 12 seconds restarts discovery of devices
 * and makes the phone discoverable once in 5 minutes.
 */
public class GroupActivity extends ListActivity implements INotifiable {
    // The ids of the messages that are to be displayed as a dialog in this activity
    private static final int SAME_GROUP_WARNING_DIALOG = 1;
    private static final int KEEP_IN_RANGE_DIALOG = 2;
    private static final int HELP_DIALOG = 3;
    private static final int EMPTY_GROUP_DIALOG = 4;

    // The found devices, paired or unpaired
    private List<Device> mDevices;
    // The adapter for the device list to be displayed in the activity
    private ArrayAdapter<Device> mAdapter;
    // The object notified when a new bluetooth device has been discovered
    private BroadcastReceiver mReceiver;

    private BluetoothAdapter mBluetoothAdapter = null;
    // The thread that once a certain period of time makes the phone visible
    // for other bluetooth devices
    private DiscoverThread mDiscoverThread = null;

    // Locks for mutual exclusion
    public static final Lock mDeviceListLock = new ReentrantLock();
    public static final Lock mDiscoverThreadLock = new ReentrantLock();
    public static final Lock mReceiverLock = new ReentrantLock();

    /**
     * Called when the activity is created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null)
            // The activity has been saved and closed -> restore it
            restoreState(savedInstanceState);
        else {
            // The activity is new -> initiate it
            mDevices = new Vector<Device>();
            basicSetup(-1);
        }
    }

    /**
     * The callback for the click event for the 'Done' button
     * @param button - the clicked button
     */
    public void onClickGroupCreated(View button) {
        groupCreationEnded();
    }

    /**
     * Displays a Toast message
     */
    @Override
    public void showText(String text) {
        runOnUiThread(new ToastViewer(this, text, Toast.LENGTH_SHORT));
    }

    /**
     * Sets the menu layout
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.group_menu, menu);
        return true;
    }

    /**
     * Method called when a menu entry was clicked
     */
    @SuppressWarnings("deprecation")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.help_menu_item :
            showDialog(HELP_DIALOG);
            return true;
        }

        return false;
    }

    /**
     * Callback for when a message dialog is displayed
     */
    @Override
    protected Dialog onCreateDialog(int id) {
        AlertDialog.Builder builder;

        switch (id) {
        case SAME_GROUP_WARNING_DIALOG:
            builder = new AlertDialog.Builder(this);
            builder.setMessage("Go to the next step only if all the phones have the same group set.")
            .setCancelable(true)
            .setPositiveButton("Next", new DialogInterface.OnClickListener() {
                @SuppressWarnings("deprecation")
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    showDialog(KEEP_IN_RANGE_DIALOG);
                }
            })
            .setNegativeButton("Cancel", null);
            return builder.create();

        case KEEP_IN_RANGE_DIALOG:
            builder = new AlertDialog.Builder(this);
            builder.setMessage("Please stay in range with all the group devices in order to pair to all of them.")
            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    groupCreationEnded();
                }
            });
            return builder.create();

        case HELP_DIALOG:
            builder = new AlertDialog.Builder(this);
            builder.setMessage("• This list displays both the paired devices and " +
                    "the ones that have just been discovered.\n" +
                    "• Please select the people you want to speak with. " +
                    "They also have to select you so you could successfully " +
                    "connect to them.")
                    .setPositiveButton("OK", null);
            return builder.create();

        case EMPTY_GROUP_DIALOG:
            builder = new AlertDialog.Builder(this);
            builder.setMessage("You have to select at least one device in order to proceed.")
            .setPositiveButton("OK", null);
            return builder.create();
        }

        return null;
    }

    /**
     * The callback called when an intent has finished
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
        case Common.REQUEST_ENABLE_BT:
            // Bluetooth enable intent
            if (resultCode != RESULT_OK && !mBluetoothAdapter.isEnabled()) {
                showText("Bluetooth has to be enabled.");
                this.finish();
            }
            break;

        case Common.REQUEST_ENABLE_DISCOVERY:
            // Intent to make the phone visible
            mDiscoverThreadLock.lock();
            if (mDiscoverThread != null)
                mDiscoverThread.discoverRequestEnded();
            mDiscoverThreadLock.unlock();
            break;
        }
    }

    /**
     * The function called when the activity is to be destroyed and
     * its state needs to be saved.
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // Save the devices that have been selected to be in the
        // group to talk with
        mDeviceListLock.lock();
        outState.putInt("groupSize", mDevices.size());
        mDiscoverThreadLock.lock();
        outState.putLong("discoverableBeginTime",
                mDiscoverThread.getExBeginDiscoverableTime());
        mDiscoverThreadLock.unlock();
        Integer i = 0;
        for (Device device : mDevices) {
            outState.putParcelable("device" + i, device);
            i++;
        }
        mDeviceListLock.unlock();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopDiscovery();
    }

    /**
     * Called when the group was set. It proceeds to connecting group.
     * A new activity is being created and the group is sent to it.
     */
    @SuppressWarnings("deprecation")
    private void groupCreationEnded() {
        // Create the group list
        List<Device> groupDevices = new ArrayList<Device>();
        mDeviceListLock.lock();
        for (Device device : mDevices)
            if (device.getPicked())
                groupDevices.add(device);
        mDeviceListLock.unlock();

        if (groupDevices.isEmpty()) {
            showDialog(EMPTY_GROUP_DIALOG);
            return;
        }

        // Stop device discovery and make the phone non-discoverable
        stopDiscovery();

        // Create the intent and add the group list to it
        Intent intent = new Intent(this, TalkActivity.class);
        intent.putExtra("groupSize", groupDevices.size());
        Integer i = 0;
        for (Device device : groupDevices) {
            intent.putExtra("device" + i, device);
            i++;
        }
        groupDevices = null;

        // Start the activity
        startActivity(intent);
    }

    /**
     * This function adds a device to the list displayed in the activity
     * @param device
     * @param paired - indicates whether the device is already paired
     */
    private void addDevice(BluetoothDevice device, boolean paired) {
        if (device.getName() == null)
            return;

        // Check whether the device is already in the list
        mDeviceListLock.lock();
        for (Device dev : mDevices)
            if (dev.getBluetoothDevice().getName().equals(device.getName())) {
                mDeviceListLock.unlock();
                return;
            }

        // Add the device in the list and notify the adapter
        mDevices.add(new Device(device, paired));
        if (mAdapter != null)
            mAdapter.notifyDataSetChanged();
        mDeviceListLock.unlock();
    }

    /**
     * Restores the activity from the state it had before it was destroyed
     * the last time
     * @param state
     */
    private void restoreState(Bundle state) {
        // Restores the device list from the state
        mDeviceListLock.lock();
        mDevices = new Vector<Device>();
        int groupSize = state.getInt("groupSize");
        long exBeginDiscoverableTime = state.getLong("discoverableBeginTime");
        for (Integer i = 0; i < groupSize; i++)
            mDevices.add((Device)state.getParcelable("device" + i));
        mDeviceListLock.unlock();

        basicSetup(exBeginDiscoverableTime);
    }

    /**
     * Stops the thread that continuously makes the phone discoverable
     */
    private void stopDiscovery() {
        // Stop the thread
        mDiscoverThreadLock.lock();
        if (mDiscoverThread != null) {
            mDiscoverThread.stopDiscovery();
        }
        mDiscoverThreadLock.unlock();

        // Unregister the callback called when a device was discovered
        mReceiverLock.lock();
        if (mReceiver != null) {
            unregisterReceiver(mReceiver);
            mReceiver = null;
        }
        mReceiverLock.unlock();
    }

    /**
     * Enable the bluetooth adapter
     * @return true if the enabling was successful
     */
    private boolean enableBluetooth() {
        // Get the bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Has no bluetooth
            showText("Your device does not support bluetooth");
            finish();
            return false;
        }

        // Enable the bluetooth
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, Common.REQUEST_ENABLE_BT);

            // Make the phone visible
            mDiscoverThreadLock.lock();
            if (mDiscoverThread != null)
                mDiscoverThread.remakeDiscoverable();
            mDiscoverThreadLock.unlock();
        }
        return true;
    }

    /**
     * Sets up the activity after it was just created
     * @param exBeginDiscoverableTime - the last time the phone was set
     * to be discoverable
     */
    @SuppressWarnings("deprecation")
    private void basicSetup(long exBeginDiscoverableTime) {
        // Set the layout, title, etc
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.group);
        setTitle(R.string.group_activity_label);
        setProgressBarIndeterminateVisibility(true);

        // Create the adapter for the list of devices displayed in the activity
        mAdapter = new GroupDeviceAdapter(this, mDevices, this,
                getWindowManager().getDefaultDisplay().getWidth());
        setListAdapter(mAdapter);

        // Check whether the phone has a bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Your device does not support bluetooth", Toast.LENGTH_LONG).show();
            this.finish();
            return;
        }

        if (!enableBluetooth())
            return;

        // Get paired devices
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        for (BluetoothDevice bd : pairedDevices) {
            addDevice(bd, true);
        }

        // Register a receiver method that is called when the state of the
        // bluetooth adapter has changed.
        mReceiverLock.lock();
        mReceiver = new BroadcastReceiver() {
            // Get an unpaired device
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    // A new device was found
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    addDevice(device, false);

                } else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                    int newState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                    if (newState == BluetoothAdapter.STATE_OFF)
                        // The bluetooth adapter got disabled
                        if (!enableBluetooth())
                            return;
                }
            }
        };
        IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, intentFilter);
        intentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mReceiver, intentFilter);
        mReceiverLock.unlock();

        // Create a thread that will remake the phone discoverable once in
        // a while
        mDiscoverThreadLock.lock();
        mDiscoverThread = new DiscoverThread(this, mBluetoothAdapter, exBeginDiscoverableTime);
        mDiscoverThread.start();
        mDiscoverThreadLock.unlock();
    }
}