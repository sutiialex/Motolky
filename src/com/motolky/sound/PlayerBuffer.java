package com.motolky.sound;

import com.motolky.Common;

public class PlayerBuffer
{
	private byte[] mBuffer;
	private int mFirst = 0;
	private int mLength = 0;

	public PlayerBuffer(int maxBufLen) {
		mBuffer = new byte[maxBufLen];
	}

	public byte[] getFrame() {
		if (mLength <= 2)
			return null;
		
		int i = 0;
		while (!((mBuffer[(i + mFirst) % mBuffer.length] == Common.SEPARATOR) && 
				(mBuffer[(1 + i + mFirst) % mBuffer.length] == Common.SEPARATOR))) {
			i++;
			if (i >= mLength - 1) {
				mLength = 0;
				return null;
			}
		}
		
		byte[] data = null;
		
		if (i > 0) {
			data = new byte[i];
			if (i + mFirst <= mBuffer.length)
				System.arraycopy(mBuffer, mFirst, data, 0, data.length);
			else {
				System.arraycopy(mBuffer, mFirst, data, 0, mBuffer.length - mFirst);
				System.arraycopy(mBuffer, 0, data, mBuffer.length - mFirst, i - (mBuffer.length - mFirst));
			}
		}
		
		mFirst = (2 + (i + mFirst)) % mBuffer.length;
		mLength -= i + 2;
		
		return data;
	}

	public void insertData(byte[] data, int noBytes) {
		if (noBytes + mLength > mBuffer.length)
			return;
		
		for (int i = 0; i < noBytes; i++)
			mBuffer[(i + mFirst + mLength) % mBuffer.length] = data[i];
		
		mLength = noBytes + mLength;
	}
}