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
import com.motolkyadfree.R;

/**
 * The activity that is displayed for selecting the devices that will
 * go into the group.
 *
 * @author Alexandru Sutii
 *
 */
public class GroupActivity extends ListActivity implements INotifiable {
    private static final int SAME_GROUP_WARNING_DIALOG = 1;
    private static final int KEEP_IN_RANGE_DIALOG = 2;
    private static final int HELP_DIALOG = 3;
    private static final int EMPTY_GROUP_DIALOG = 4;

    private List<Device> mDevices;
    private ArrayAdapter<Device> mAdapter;

    private BroadcastReceiver mReceiver;

    //private IConnectable thisConnectable = this;
    private BluetoothAdapter mBluetoothAdapter = null;
    private DiscoverThread mDiscoverThread = null;

    public static final Lock mDeviceListLock = new ReentrantLock();
    public static final Lock mDiscoverThreadLock = new ReentrantLock();
    public static final Lock mReceiverLock = new ReentrantLock();

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null)
            restoreState(savedInstanceState);
        else {
            mDevices = new Vector<Device>();
            basicSetup(-1);
        }
    }

    private boolean enableBluetooth() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {  // Has no bluetooth
            showText("Your device does not support bluetooth");
            finish();
            return false;
        }
        
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, Common.REQUEST_ENABLE_BT);
            mDiscoverThreadLock.lock();
            if (mDiscoverThread != null)
                mDiscoverThread.remakeDiscoverable();
            mDiscoverThreadLock.unlock();
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case Common.REQUEST_ENABLE_BT:
                if (resultCode != RESULT_OK && !mBluetoothAdapter.isEnabled()) {
                    showText("Bluetooth has to be enabled.");
                    this.finish();
                }
                break;
            case Common.REQUEST_ENABLE_DISCOVERY:
                mDiscoverThreadLock.lock();
                if (mDiscoverThread != null)
                    mDiscoverThread.discoverRequestEnded();
                mDiscoverThreadLock.unlock();
                break;
        }
    }

    private void basicSetup(long exBeginDiscoverableTime) {
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.group);
        setTitle(R.string.group_activity_label);

        setProgressBarIndeterminateVisibility(true);
    
        mAdapter = new GroupDeviceAdapter(this, mDevices, this,
                getWindowManager().getDefaultDisplay().getWidth());
        setListAdapter(mAdapter);

        // Check whether the phone has a bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Your device does not support bluetooth", Toast.LENGTH_LONG);
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

        mReceiverLock.lock();
        mReceiver = new BroadcastReceiver() {
            // Get an unpaired device
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    addDevice(device, false);
                } else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                    int newState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                    if (newState == BluetoothAdapter.STATE_OFF)
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

        mDiscoverThreadLock.lock();
        mDiscoverThread = new DiscoverThread(this, mBluetoothAdapter, exBeginDiscoverableTime);
        mDiscoverThread.start();
        mDiscoverThreadLock.unlock();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

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

    private void restoreState(Bundle state) {
        mDeviceListLock.lock();
        mDevices = new Vector<Device>();
        int groupSize = state.getInt("groupSize");
        long exBeginDiscoverableTime = state.getLong("discoverableBeginTime");
        for (Integer i = 0; i < groupSize; i++)
            mDevices.add((Device)state.getParcelable("device" + i));
        mDeviceListLock.unlock();

        basicSetup(exBeginDiscoverableTime);
    }

    private void stopDiscovery() {
        mDiscoverThreadLock.lock();
        if (mDiscoverThread != null) {
            mDiscoverThread.stopDiscovery();
        }
        mDiscoverThreadLock.unlock();
        mReceiverLock.lock();
        if (mReceiver != null) {
            unregisterReceiver(mReceiver);
            mReceiver = null;
        }
        mReceiverLock.unlock();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopDiscovery();
    }

    private void addDevice(BluetoothDevice device, boolean paired) {
        if (device.getName() == null)
            return;
        mDeviceListLock.lock();
        for (Device dev : mDevices)
            if (dev.getBluetoothDevice().getName().equals(device.getName())) {
                mDeviceListLock.unlock();
                return;
            }

        mDevices.add(new Device(device, paired));
        if (mAdapter != null)
            mAdapter.notifyDataSetChanged();

        mDeviceListLock.unlock();
    }

    public void onClickGroupCreated(View button) {
        groupCreationEnded();
        //showDialog(SAME_GROUP_WARNING_DIALOG);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        AlertDialog.Builder builder;
        switch (id) {
            case SAME_GROUP_WARNING_DIALOG:
                builder = new AlertDialog.Builder(this);
                builder.setMessage("Go to the next step only if all the phones have the same group set.")
                        .setCancelable(true)
                        .setPositiveButton("Next", new DialogInterface.OnClickListener() {
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
     * Called when the group was set. It proceeds to connecting group.
     */
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

        // Stop discovery and my discoverability
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

    @Override
    public void showText(String text) {
        runOnUiThread(new ToastViewer(this, text, Toast.LENGTH_SHORT));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.group_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.help_menu_item :
                showDialog(HELP_DIALOG);
                return true;
        }

        return false;
    }
}