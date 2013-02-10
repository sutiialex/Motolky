package com.motolky.sound;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

import com.motolky.Common;
import com.motolky.communication.IReceiveHandler;

/**
 * This class creates a AudioTrack object and sends sound data
 * to it whenever it receives some data.
 *
 * @author Alexandru Sutii
 *
 */
public class Player extends Thread implements IReceiveHandler {
    private static final List<Player> createdPlayers = new LinkedList<Player>();
    private static float volume = (float)0.5;

    private AudioTrack mAudioTrack = null;
    private float mMaxVolume;
    private float mMinVolume;
    private final Lock mLock = new ReentrantLock();
    private final Lock mAudioLock = new ReentrantLock();
    
    private PlayerBuffer mBuffer;
    private Condition mCondition = mLock.newCondition();
    private Codec mDecoder = null;
    private boolean mExit = false;

    protected Player() {
        int minLen = AudioTrack.getMinBufferSize(Common.SAMPLE_RATE, Common.CHANNEL_CONFIG,
                Common.AUDIO_FORMAT);

        mAudioTrack = new AudioTrack(AudioManager.STREAM_VOICE_CALL,
                Common.SAMPLE_RATE, Common.CHANNEL_CONFIG, Common.AUDIO_FORMAT,
                minLen, AudioTrack.MODE_STREAM);
        mAudioTrack.play();
        mMaxVolume = AudioTrack.getMaxVolume();
        mMinVolume = AudioTrack.getMinVolume();
        float initialVolume = mMinVolume + (mMaxVolume - mMinVolume) * volume;
        mAudioTrack.setStereoVolume(initialVolume, initialVolume);
        
        try
        {
        	mDecoder = new SoundDecoder();
        	mBuffer = new PlayerBuffer(2 * mDecoder.getSampleSize());
        } catch (Exception e) {
        	while (true)
        	{
        		Log.e(Common.TAG, "Error creating the encoder");
        		e.printStackTrace();
        	}
        }
    }

    @Override
    public void receiveData(byte[] buffer, int bytes) throws IOException {
    	this.mAudioLock.lock();
        if (this.mAudioTrack == null) {
          this.mAudioLock.unlock();
          throw new IOException("The audio track was closed already");
        }
        this.mAudioLock.unlock();
        
        this.mLock.lock();
        this.mBuffer.insertData(buffer, bytes);
        this.mCondition.signal();
        this.mLock.unlock();
    }

    private void exit() {
        this.mAudioLock.lock();
        
        try {
        	this.mAudioTrack.stop();
        	this.mAudioTrack.release();
        	this.mAudioTrack = null;
        	this.mAudioLock.unlock();
        	
        	this.mLock.lock();
        	this.mDecoder.exit();
        	this.mExit = true;
        	this.mCondition.signal();
        	this.mLock.unlock();
        } catch (IllegalStateException localIllegalStateException) {
        	this.mAudioTrack.release();
        }
    }

    public static Player getNewPlayer() {
        Player player = new Player();
        createdPlayers.add(player);
        player.start();
        return player;
    }

    public static void deletePlayer(Player player) {
        createdPlayers.remove(player);
        player.exit();
    }

    private void adjustVolume() {
        this.mAudioLock.lock();
        float v = mMinVolume + (mMaxVolume - mMinVolume) * volume;
        mAudioTrack.setStereoVolume(v, v);
        this.mAudioLock.unlock();
    }

    public static void setVolume(float progress) {
        volume = progress;
        for (Player player : createdPlayers)
            player.adjustVolume();
    }
    
    @Override
    public void run() {
    	while (true)
    	{
    		if (mExit)
    			return;
    		
    		try
    		{
    			mCondition.await();
    			mLock.lock();
        		byte[] frame = mBuffer.getFrame();
        		if (frame == null) {
        			mLock.unlock();
        			continue;
        		}
        		mLock.unlock();
    			
        		short[] decoded = mDecoder.decodeAndGetDecoded(frame, frame.length);
    			if (decoded == null)
    				continue;
    			
    			mAudioLock.lock();
    			if (mAudioTrack != null)
    				mAudioTrack.write(decoded, 0, decoded.length);
    			mAudioLock.unlock();
    		}
    		catch (InterruptedException e)
    		{
    			mLock.unlock();
    		}
    	}
    }
}
