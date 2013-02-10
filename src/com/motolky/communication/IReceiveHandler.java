package com.motolky.communication;

import java.io.IOException;

/**
 * An interface for receiving data
 *
 * @author Alexandru Sutii
 *
 */
public interface IReceiveHandler {

    public void receiveData(byte[] buffer, int bytes) throws IOException;
}
