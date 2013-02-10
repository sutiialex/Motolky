/*
The interface to speex needs to be defined

The following native functions need to be defined:


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


	private native int createDecoder();

	private native boolean decode(int paramInt1, byte[] paramArrayOfByte, int paramInt2, short[] paramArrayOfShort);

	private native void destroyDecoder(int paramInt);

	private native int getDecoderFrameSize(int paramInt);
*/