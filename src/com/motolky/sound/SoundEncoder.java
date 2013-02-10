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

import java.security.InvalidParameterException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SoundEncoder extends Codec
{
	private static boolean mLibraryLoaded;
	private static final Lock mLock = new ReentrantLock();

	static {
		mLibraryLoaded = false;
		checkLibrary();
	}

	public SoundEncoder() throws Exception {
		if (!createEncoder())
			throw new Exception("Could not creat a new encoder");
		this.mFrameSize = getEncoderFrameSize();
	}

	private static void checkLibrary() {
		mLock.lock();
		if (mLibraryLoaded)
		{
			mLock.unlock();
			return;
		}

		System.loadLibrary("speex");
		mLibraryLoaded = true;
		mLock.unlock();
	}

	public static void setComplexity(int complexity)
	{
		checkLibrary();
		mLock.lock();
		setEncoderComplexity(complexity);
		mLock.unlock();
	}

	public static void setDenoise(boolean denoise)
	{
		checkLibrary();
		mLock.lock();
		setEncoderDenoise(denoise);
		mLock.unlock();
	}

	public static void setPreproc(boolean enablePreprocessor)
	{
		checkLibrary();
		mLock.lock();
		setPreprocessorEnable(enablePreprocessor);
		mLock.unlock();
	}

	public static void setQuality(int quality)
	{
		checkLibrary();
		mLock.lock();
		setEncoderQuality(quality);
		mLock.unlock();
	}

	public static void setVAD(boolean enableVAD)
	{
		checkLibrary();
		mLock.lock();
		setEncoderVAD(enableVAD);
		mLock.unlock();
	}

	public byte[] encodeAndGetEncoded(short[] data, int start, int length)
			throws InvalidParameterException {
		verifyDataLen(length);
		byte[] bytes = new byte[2 * this.mFrameSize];
		short[] shorts = new short[length];
		System.arraycopy(data, start, shorts, 0, length);
		mLock.lock();
		int i = encode(shorts, bytes, bytes.length);
		mLock.unlock();
		if (i > 0)
		{
			byte[] tmp = new byte[i];
			System.arraycopy(bytes, 0, tmp, 0, i);
			return tmp;
		}
		return null;
	}

	public void exit()
	{
		checkLibrary();
		mLock.lock();
		destroyEncoder();
		mLock.unlock();
	}

	private static native void setPreprocessorEnable(boolean paramBoolean);

	private native boolean createEncoder();

	private native void destroyEncoder();

	private native int encode(short[] paramArrayOfShort, byte[] paramArrayOfByte, int paramInt);

	private static native int getEncoderComplexity();

	private native int getEncoderFrameSize();

	private static native void setEncoderComplexity(int paramInt);

	private static native void setEncoderDenoise(boolean paramBoolean);

	private static native void setEncoderQuality(int paramInt);

	private static native void setEncoderVAD(boolean paramBoolean);
}