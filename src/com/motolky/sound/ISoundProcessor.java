package com.motolky.sound;

/**
 * An interface that a sound processor should implement.
 * It has to be able to receive raw sound and to have
 * a method that will provide the processed sound if any.
 *
 * @author Alexandru Sutii
 *
 */
public interface ISoundProcessor {
	  public abstract void addRawSound(short[] buffer, int nr);

	  public abstract void exit();

	  public abstract int getProcessedSound(byte[] buffer, int nr);
}
