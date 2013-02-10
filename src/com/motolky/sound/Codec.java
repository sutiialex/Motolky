package com.motolky.sound;

import java.security.InvalidParameterException;

public abstract class Codec
{
	public static int CODEC_COMPLEXITY = 1;
	protected int mFrameSize = 160;

	public short[] decodeAndGetDecoded(byte[] data, int nr) throws InvalidParameterException
	{
		return null;
	}

	public byte[] encodeAndGetEncoded(short[] data, int start, int end) throws InvalidParameterException
	{
		return null;
	}

	public abstract void exit();

	public int getSampleSize()
	{
		return this.mFrameSize;
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

	public void verifyDataLen(int nr) throws InvalidParameterException
	{
		if (nr != getSampleSize())
			throw new InvalidParameterException("Data length should equal " + getSampleSize());
	}
}