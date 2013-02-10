package com.motolky.sound;

import java.security.InvalidParameterException;

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

	public short[] decodeAndGetDecoded(byte[] data, int no)
			throws InvalidParameterException {
		short[] buf = new short[getSampleSize()];
		if (!decode(this.mId, data, no, buf))
			return null;
		return buf;
	}

	public void exit() {
		destroyDecoder(this.mId);
	}
	
	private native int createDecoder();

	private native boolean decode(int paramInt1, byte[] paramArrayOfByte, int paramInt2, short[] paramArrayOfShort);

	private native void destroyDecoder(int paramInt);

	private native int getDecoderFrameSize(int paramInt);
}