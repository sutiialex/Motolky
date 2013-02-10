package com.motolky.communication;

import java.io.IOException;

import com.motolky.Peer;

import android.bluetooth.BluetoothSocket;

/**
 * The interface for getting a socket from the lower layer
 *
 * @author Alexandru Sutii
 *
 */
public interface IConnectable {
    BluetoothSocket getSocket(Peer peer, Device device) throws IOException;
}
