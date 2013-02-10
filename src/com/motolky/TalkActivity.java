package com.motolky;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.UUID;
import java.util.Vector;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.motolky.communication.Device;
import com.motolky.communication.IConnectNotifiable;
import com.motolky.communication.IConnectable;
import com.motolky.communication.ServerThread;
import com.motolky.sound.Player;
import com.motolky.sound.RecordThread;
import com.motolkyadfree.R;

/**
 * The activity that is displayed when communication begins
 * It shows the group devices and the status for each of them(connected, disconnected, etc)
 *
 * @author Alexandru Sutii
 *
 */
public class TalkActivity extends ListActivity implements INotifiable, IConnectable,IConnectNotifiable {
    private static final int HELP_DIALOG = 1;

    private List<Device> mGroupDevices;
    private ArrayAdapter<Device> mAdapter;
    private BluetoothAdapter mBluetoothAdapter = null;
    private List<Peer> mConnectedToPeers = null;
    private List<ServerThread> mServerThreads = null;
    private RecordThread mRecordThread = null;
    private BroadcastReceiver mReceiver = null;
    private float mExVolumeValue = 0;
    private boolean mSpeakerOn = true;
    private boolean mMicOn = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.talk);
        mGroupDevices = new Vector<Device>();
        mAdapter = new TalkDeviceAdapter(this, mGroupDevices, this,
                getWindowManager().getDefaultDisplay().getWidth());
        setListAdapter(mAdapter);

        int groupSize;
        if (savedInstanceState == null) {
            // Get the group list from the intent
            Intent intent = getIntent();
            groupSize = intent.getIntExtra("groupSize", 0);
            for (Integer i = 0; i < groupSize; i++)
                mGroupDevices.add((Device)intent.getParcelableExtra("device" + i));
            if (groupSize > 0)
                mAdapter.notifyDataSetChanged();
        } else {    // The activity is being resumed
            groupSize = savedInstanceState.getInt("groupSize");
            for (Integer i = 0; i < groupSize; i++)
                mGroupDevices.add((Device)savedInstanceState
                                .getParcelable("device" + i));
            if (groupSize > 0)
                mAdapter.notifyDataSetChanged();
        }
        for (Device device : mGroupDevices)
            device.setConnected(false);
        if (groupSize > 0)
            mAdapter.notifyDataSetChanged();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Common.ENABLE_VAD = prefs.getBoolean("VAD Enable", false);
        try {
            Common.RECONNECT_MIN_TIMEOUT = Integer.parseInt(prefs.getString("Reconnect Timeout", "3")) * 1000;
        } catch (Exception e) {
            runOnUiThread(new ToastViewer(this, 
                    "Wrong reconnect timeout: " + prefs.getString("Reconnect Timeout", "3"), Toast.LENGTH_SHORT));
        }

        createServersAndConnect();
    }

    private void enableBluetooth() {
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, Common.REQUEST_ENABLE_BT);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case Common.REQUEST_ENABLE_BT :
                if (resultCode != RESULT_OK) {
                    showText("Bluetooth has to be enabled.");
                    this.finish();
                }
                break;
            case Common.REQUEST_CHANGE_PREFERENCES :
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                Common.ENABLE_VAD = prefs.getBoolean("VAD Enable", false);
                try {
                    Common.RECONNECT_MIN_TIMEOUT = Integer.parseInt(prefs.getString("Reconnect Timeout", "3")) * 1000;
                } catch (Exception e) {
                    runOnUiThread(new ToastViewer(this, 
                            "Wrong reconnect timeout: " + prefs.getString("Reconnect Timeout", "3"), Toast.LENGTH_SHORT));
                }
                break;
        }
    }

    private void createServersAndConnect() {
        mReceiver = new BroadcastReceiver() {
            // Get an unpaired device
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                    int newState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                    if (newState == BluetoothAdapter.STATE_OFF)
                        enableBluetooth();
                } else if (TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(action)) {
                    TelephonyManager manager = (TelephonyManager)getSystemService(TELEPHONY_SERVICE);

                    PhoneStateListener listener = new PhoneStateListener() {
                        @Override
                        public void onCallStateChanged(int state,
                                String incomingNumber) {
                            super.onCallStateChanged(state, incomingNumber);
                            switch (state) {
                                case TelephonyManager.CALL_STATE_OFFHOOK:
                                    suspend(true);
                                    break;

                                default:
                                    suspend(false);
                                    break;
                            }
                        }
                    };

                    manager.listen(listener, PhoneStateListener.LISTEN_CALL_STATE);
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mReceiver, intentFilter);
        intentFilter = new IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
        registerReceiver(mReceiver, intentFilter);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        String myDeviceName = mBluetoothAdapter.getName().toLowerCase();
        mServerThreads = new Vector<ServerThread>();
        mConnectedToPeers = new Vector<Peer>();

        mRecordThread = new RecordThread(Common.AUDIO_BUFFER_LEN);

        // Take each device in the group. Kind of, sort them lexicographically.
        // Connect to the ones that have smaller lexicographical names,
        // and listen for connections from the other ones.
        // For example say we have the following devices: bill, angie, jack and my
        // device name is bond. I will connect to angie and bill, but I will listen
        // for connections from jack.
        for (Device device : mGroupDevices)
            if (device.getName().toLowerCase().compareTo(myDeviceName) < 0) { // Connect to it
                connectTo(device);
            } else if (device.getName().toLowerCase().compareTo(myDeviceName) > 0) { // Listen for connection
                listenFor(device);
            } else { // The have the same name -> Sort by MAC address
                if (device.getBluetoothDevice().getAddress().
                        compareTo(mBluetoothAdapter.getAddress()) < 0)
                    connectTo(device);
                else
                    listenFor(device);
            }
        mRecordThread.start();

        ((SeekBar)findViewById(R.id.volumeSeekbar)).setOnSeekBarChangeListener(
                new OnSeekBarChangeListener() {

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {}

                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress,
                            boolean fromUser) {
                        float volume = (float)seekBar.getProgress()/seekBar.getMax();
                        ToggleButton speakerButton = (ToggleButton)findViewById(R.id.speakerButton);
                        Player.setVolume(volume);
                        if (volume == 0) {
                            speakerButton.setChecked(false);
                        } else {
                            if (!speakerButton.isChecked())
                                speakerButton.setChecked(true);
                        }
                    }
                });
    }

    private void suspend(boolean state) {
        setMicState(!state);
        setSpeakerState(!state);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        AlertDialog.Builder builder;
        switch (id) {
            case HELP_DIALOG:
                builder = new AlertDialog.Builder(this);
                builder.setMessage("This list displays the peers you have selected to speak with. " +
                        "For each of them you can see the status of the connection.")
                .setPositiveButton("OK", null);
                return builder.create();
        }
        return null;
    }

    private void listenFor(Device device) {
        device.setUUID(createUUID(device.getName(), mBluetoothAdapter.getName()));
        ServerThread serverThread = new ServerThread(mRecordThread, mBluetoothAdapter,
                                                    device, this);
        serverThread.start();
        mServerThreads.add(serverThread);
    }

    private void connectTo(Device device) {
        device.setUUID(createUUID(mBluetoothAdapter.getName(), device.getName()));
        Peer peer = new Peer(mRecordThread, this, this, device, null, true);
        peer.start();
        mConnectedToPeers.add(peer);
    }

    private UUID createUUID(String connectingDeviceName, String serverDeviceName) {
        String concatenatedString = connectingDeviceName + serverDeviceName;
        UUID uuid = null;

        try {
            // Creat MD5 hash
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(concatenatedString.getBytes());
            byte[] hash = digest.digest();

            // Create the uuid bytes by XOR-ing the hash by 16 bytes
            byte[] result = new byte[16];
            for (int i = 0; i < hash.length; i += 16)
                for (int j = 0; j < 16; j++)
                    result[j] ^= hash[i+j];

            // Create the uuid
            uuid = UUID.nameUUIDFromBytes(result);
            String uuidString = uuid.toString();
            Log.d(Common.TAG, connectingDeviceName + "->" + serverDeviceName + ": uuid = " + uuidString);
        } catch (NoSuchAlgorithmException ae) {
            Log.
            e(Common.TAG, ae.getMessage());
        }
        return uuid;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);

        // Stop listening threads
        if (mServerThreads != null) {
            for (ServerThread serverThread : mServerThreads)
                serverThread.exit();
            mServerThreads = null;
        }
        // Stop connecting peers
        if (mConnectedToPeers != null) {
            for (Peer peer : mConnectedToPeers)
                peer.exit();
            mConnectedToPeers = null;
        }
        if (mRecordThread != null) {
            mRecordThread.exit();
            mRecordThread = null;
        }
        //finish();
    }

    @Override
    public void showText(String text) {
        runOnUiThread(new ToastViewer(this, text, Toast.LENGTH_LONG));
    }

    @Override
    public BluetoothSocket getSocket(Peer peer, Device device)
            throws IOException {
        BluetoothSocket socket = null;
        try {
            socket = device.getBluetoothDevice().
                                    createRfcommSocketToServiceRecord(device.getUUID());
            socket.connect();

            Log.d(Common.TAG, "Connected to device.");
        } catch (IOException ioe) {
            Log.e(Common.TAG, "Connect to device error: " + ioe.getMessage());
            throw ioe;
        }
        return socket;
    }

    private void modifyDeviceConnectStatus(Peer peer, boolean connected) {
        if (peer.getRemoteDevice() != null)
            peer.getRemoteDevice().setConnected(connected);
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                mAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("groupSize", mGroupDevices.size());
        Integer i = 0;
        for (Device device : mGroupDevices) {
            outState.putParcelable("device" + i, device);
            i++;
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);
    }

    @Override
    public void connected(Peer peer) {
        modifyDeviceConnectStatus(peer, true);
    }

    @Override
    public void disconnected(Peer peer) {
        modifyDeviceConnectStatus(peer, false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.talk_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.help_menu_item :
                showDialog(HELP_DIALOG);
                return true;
            case R.id.preferences_menu_item :
                startActivityForResult(new Intent(this, PreferencesActivity.class),
                        Common.REQUEST_CHANGE_PREFERENCES);
                return true;
        }

        return false;
    }
    
    private void setMicState(boolean state) {
        if (mMicOn == state)
            return;
        mRecordThread.setRecordState(state);
        mMicOn = state;
    }

    private void setSpeakerState(boolean state) {
        if (mSpeakerOn == state)
            return;
        SeekBar volumeSeekbar = (SeekBar)findViewById(R.id.volumeSeekbar);
        if (state) {
            Player.setVolume(mExVolumeValue);
            volumeSeekbar.setProgress((int)(mExVolumeValue*volumeSeekbar.getMax()));
        } else {
            Player.setVolume(0);
            mExVolumeValue = (float)volumeSeekbar.getProgress()/volumeSeekbar.getMax();
            volumeSeekbar.setProgress(0);
        }
        mSpeakerOn = state;
    }

    public void micToggleClick(View view) {
        ToggleButton button = (ToggleButton)view;
        setMicState(button.isChecked());
    }

    public void speakerToggleClick(View view) {
        ToggleButton button = (ToggleButton)view;
        setSpeakerState(button.isChecked());
    }
}
