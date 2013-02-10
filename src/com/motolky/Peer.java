package com.motolky;

import java.io.IOException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

import com.motolky.communication.CommunicationThread;
import com.motolky.communication.Device;
import com.motolky.communication.IConnectNotifiable;
import com.motolky.communication.IConnectable;
import com.motolky.sound.Player;
import com.motolky.sound.RecordThread;

/**
 * The class that defines a communication peer.
 * It basically gets a IConnectable object that gives it an open socket.
 * It creates the player for the received sound and adds it to the
 * recording thread. It also creates a communication thread that
 * receives data from the socket and send it to the player.
 *
 * This class's main purpose is to reconnect whenever a socket
 * closes.
 *
 * @author Alexandru Sutii
 *
 */
public class Peer extends Thread {
    private IConnectable mConnectable = null;
    private BluetoothSocket mSocket = null;
    private boolean mStopped = false;
    private Device mRemoteDevice = null;
    private boolean mError = false;
    private RecordThread mRecordThread = null;
    private boolean mAskNewSocket;
    private IConnectNotifiable mConnectNotifiable = null;
    private final Lock mLock = new ReentrantLock();
    private final Condition mErrorOrStopCondition = mLock.newCondition();

    public Peer(RecordThread recordThread,
                IConnectable connectable,
                IConnectNotifiable connectNotifiable,
                Device remoteDevice,
                BluetoothSocket socket,
                boolean askNewSocket) {
        mConnectable = connectable;
        mRemoteDevice = remoteDevice;
        mSocket = socket;
        mRecordThread = recordThread;
        mAskNewSocket = askNewSocket;
        mConnectNotifiable = connectNotifiable;
    }

    public String getRemoteDeviceName() {
        return mRemoteDevice.getName();
    }

    public Device getRemoteDevice() {
        return mRemoteDevice;
    }

    public void exit() {
        try {
            mRemoteDevice = null;

            mLock.lock();
            mStopped = true;
            mErrorOrStopCondition.signal();
            mLock.unlock();
        } catch (Exception e) {
        }
    }

    public void communicationErrorOccured() {
        mLock.lock();
        mError = true;
        mErrorOrStopCondition.signal();
        mLock.unlock();
    }

    @Override
    public void run() {
        while (!mStopped) {
            try {
                if (mAskNewSocket)
                    mSocket = mConnectable.getSocket(this, mRemoteDevice);
            } catch (IOException ioe) { // Probably device is not in range
                Log.e(Common.TAG, "Error getting the socket: " + ioe.getMessage());
                try {
                    sleep(Common.RECONNECT_TIMEOUT);
                } catch (InterruptedException ie) {
                }
                continue;
            } catch (Exception e) {
                Log.e(Common.TAG, e.getMessage());
                return;
            }

            if (mSocket == null) // This means I should not try to reconnect again
                return;


            mError = false;
            // Create the communication thread, the recorder and the track
            Player player = Player.getNewPlayer();
            CommunicationThread commThread = new CommunicationThread(player, mSocket, this);
            commThread.start();
            mRecordThread.addSendHandler(commThread);
            // Connected ok
            mConnectNotifiable.connected(this);

            // Wait while an socket exception occurs or a stop command is given
            while (!mStopped && !mError) {
                mLock.lock();
                try {
                    mErrorOrStopCondition.await();
                } catch (Exception e) {
                }
                mLock.unlock();
            }

            Player.deletePlayer(player);
            mRecordThread.removeSendHandler(commThread);
            commThread.cancel();
            if (!mStopped)
                mConnectNotifiable.disconnected(this);

            if (!mAskNewSocket)
                return;            // The peer is on a server. Do not try to reconnect.
                                // The remote device should reconnect to this server.
        }
    }

}
