package com.motolky.communication;

import com.motolky.Peer;

/**
 * An interface for specifying methods to be run whenever a user connects/disconnects.
 *
 * @author Alexandru Sutii
 *
 */
public interface IConnectNotifiable {
    void connected(Peer peer);
    void disconnected(Peer peer);
}
