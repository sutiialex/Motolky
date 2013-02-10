package com.motolky.communication;

/**
 * An interface for methods for sending data.
 *
 * @author Alexandru Sutii
 *
 */
public interface ISendHandler {
    public void sendData(byte[] buffer, int bytes);
}
