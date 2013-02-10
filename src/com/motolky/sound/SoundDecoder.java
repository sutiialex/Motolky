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
 * This class is used for decoding audio data. It is an interface to
 * the speex library. It uses the JNI interface, because we used the C
 * version of speex. It assumes that the data to decode was also encoded
 * with speex.
 */
public class SoundDecoder extends Codec
{
    private int mId = createDecoder();

    static {
        System.loadLibrary("speex");
    }

    public SoundDecoder() throws Exception {
        if (mId < 0)
            throw new Exception("There is no empty decoder slot");
        mFrameSize = getDecoderFrameSize(mId);
    }

    @Override
    public short[] decodeAndGetDecoded(byte[] data, int no)
            throws InvalidParameterException {
        short[] buf = new short[getSampleSize()];
        if (!decode(this.mId, data, no, buf))
            return null;
        return buf;
    }

    @Override
    public void exit() {
        destroyDecoder(this.mId);
    }

    // TODO: These functions need to be implemented in C
    private native int createDecoder();

    private native boolean decode(int paramInt1, byte[] paramArrayOfByte, int paramInt2, short[] paramArrayOfShort);

    private native void destroyDecoder(int paramInt);

    private native int getDecoderFrameSize(int paramInt);
}