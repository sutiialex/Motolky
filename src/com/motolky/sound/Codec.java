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

package com.motolky.sound;

import java.security.InvalidParameterException;

/**
 * This is an abstract class for an encoder or a decoder. Do not instantiate it directly.
 *
 * The data recorded from the microphone or sent to a player is raw. However, in
 * order to send it via bluetooth, we encoded it using an encoder.
 * This class defines two methods, one for encoding and one for decoding.
 * The descendends of this class need to implement these two methods.
 */
public abstract class Codec
{
    public static int CODEC_COMPLEXITY = 1;
    protected int mFrameSize = 160;

    /**
     * Given a buffer of data, decode it and return the result
     * @param data - the data to decode
     * @param nr - the number of bytes from buffer to decode
     * @return the decoded data
     * @throws InvalidParameterException
     */
    public short[] decodeAndGetDecoded(byte[] data, int nr) throws InvalidParameterException
    {
        return null;
    }

    /**
     * Given a buffer of data, encode it and return the result
     * @param data - the data to encode
     * @param start - where the data to encode starts in the given buffer
     * @param end - where the data to encode ends in the given buffer
     * @return the encoded data
     * @throws InvalidParameterException
     */
    public byte[] encodeAndGetEncoded(short[] data, int start, int end) throws InvalidParameterException
    {
        return null;
    }

    /**
     *  End the encoding/decoding
     */
    public abstract void exit();

    /**
     * The data is divided in chunks that are encoded/decoded.
     * @return the size of the codec's sample
     */
    public int getSampleSize()
    {
        return this.mFrameSize;
    }

    /**
     * Data sanity check
     * @param nr - this needs to be equal to the sample size
     * @throws InvalidParameterException
     */
    public void verifyDataLen(int nr) throws InvalidParameterException
    {
        if (nr != getSampleSize())
            throw new InvalidParameterException("Data length should equal " + getSampleSize());
    }

    protected void switchEndianness(byte[] data)
    {
        for (int i = 0; i < data.length; i += 2)
        {
            byte tmp = data[i];
            data[i] = data[i + 1];
            data[i + 1] = tmp;
        }
    }
}