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
    * Neither the name of the <organization> nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.motolky.sound;

import android.util.Log;

import com.motolky.Common;

/**
 * This class is intended to provide a sound filter. It's main purpose
 * is to detect voice, and when this occurs to provide it in order to send
 * it further. For this it performs a Fast Fourier Transform and detects
 * voice frequencies. It will also try to remove the noise.
 *
 * @author Alexandru Sutii
 *
 */
public class SoundProcessor implements ISoundProcessor {
	private final byte TERMINATION = 127;
    private final int MAX_BUFFER_LEN;

    private short[] inBuffer;
    private int firstIn = 0;
    private int inBufferLen = 0;
    private byte[] outBuffer;
    private int outBufferLen = 0;
    private int firstOut = 0;
    private int mSamplesLen = -1;
    
    private Codec mCodec;

    public SoundProcessor(int maxBufferLen) {
    	
    	this.MAX_BUFFER_LEN = maxBufferLen;
        this.inBuffer = new short[2 * MAX_BUFFER_LEN];
        this.outBuffer = new byte[4 * MAX_BUFFER_LEN];
        try
        {
          this.mCodec = new SoundEncoder();
          this.mSamplesLen = this.mCodec.getSampleSize();
        } catch (Exception e) {
            Log.e(Common.TAG, "Error creating the encoder");
            e.printStackTrace();
        }
    }

    private void processSamples(short[] samples)
    {
    	byte[] encoded = this.mCodec.encodeAndGetEncoded(samples, 0, samples.length);
    	if (encoded == null)
    		return;
    	
    	for (int i = 0; i < encoded.length; i++)
    		this.outBuffer[(this.firstOut + this.outBufferLen + i) % 
    				this.outBuffer.length] = encoded[i];
		this.outBufferLen += encoded.length;
		
		for (int j = 0; j < 2; j++)
			this.outBuffer[(this.firstOut + this.outBufferLen + j) % this.outBuffer.length] = TERMINATION;
    }

    @Override
    public void addRawSound(short[] data, int shorts) {
        for (int i = 0; i < shorts; i++)
            inBuffer[(i + firstIn + inBufferLen)%(2*inBuffer.length)] = data[i];
        inBufferLen += shorts;

        while (inBufferLen >= mSamplesLen) {
            short[] in = new short[mSamplesLen];
            for (int i = 0; i < mSamplesLen; i++)
                in[i] = inBuffer[(firstIn + i)%(2*MAX_BUFFER_LEN)];
            firstIn = (firstIn + mSamplesLen) % (2*MAX_BUFFER_LEN);
            inBufferLen -= mSamplesLen;
            processSamples(in);
        }
    }

    @Override
    public int getProcessedSound(byte[] data, int maxbytes) {
        int len = this.outBufferLen;
        if (len >= 0) {
        	int j = 0;
        	while (true) {
	            if ((j >= maxbytes) || (j >= len))
	            {
	              this.outBufferLen -= j;
	              return j;
	            }
	            data[j] = this.outBuffer[this.firstOut];
	            j++;
	            this.firstOut = (1 + this.firstOut) % this.outBuffer.length;
        	}
        }
        return 0;
    }
    
    @Override
    public void exit() {
   	    this.mCodec.exit();
    }
}
