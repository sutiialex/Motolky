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
 * This class creates an AudioTrack object and sends sound data
 * to it whenever it receives some audio data.
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

        // Crate the audio track and start it
        mAudioTrack = new AudioTrack(AudioManager.STREAM_VOICE_CALL,
                Common.SAMPLE_RATE, Common.CHANNEL_CONFIG, Common.AUDIO_FORMAT,
                minLen, AudioTrack.MODE_STREAM);
        mAudioTrack.play();
        mMaxVolume = AudioTrack.getMaxVolume();
        mMinVolume = AudioTrack.getMinVolume();
        float initialVolume = mMinVolume + (mMaxVolume - mMinVolume) * volume;
        mAudioTrack.setStereoVolume(initialVolume, initialVolume);

        // Create a decoder for the data the player receives from the outside
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

    /**
     * This method is called by the peer when data has been received from the
     * socket.
     */
    @Override
    public void receiveData(byte[] buffer, int bytes) throws IOException {
        this.mAudioLock.lock();
        if (this.mAudioTrack == null) {
          this.mAudioLock.unlock();
          throw new IOException("The audio track was closed already");
        }
        this.mAudioLock.unlock();

        // Insert the data to the buffer. The thread will late pick it up,
        // decode it and play it.
        this.mLock.lock();
        this.mBuffer.insertData(buffer, bytes);
        this.mCondition.signal();
        this.mLock.unlock();
    }

    /**
     * Factory for creating a player. Internally this class
     * remembers the players that are currently active in the system.
     * @return the created player
     */
    public static Player getNewPlayer() {
        Player player = new Player();
        createdPlayers.add(player);
        player.start();
        return player;
    }

    /**
     * Removes a player from the list of created players.
     * @param player - player to remove
     */
    public static void deletePlayer(Player player) {
        createdPlayers.remove(player);
        player.exit();
    }

    public static void setVolume(float progress) {
        volume = progress;
        for (Player player : createdPlayers)
            player.adjustVolume();
    }

    /**
     * This loop continuously reads data from the buffer of the player,
     * decodes it and sends it to the audio track object.
     */
    @Override
    public void run() {
        while (true)
        {
            if (mExit)
                return;

            try
            {
                // Ask for a frame from the buffer
                mLock.lock();
                mCondition.await();
                byte[] frame = mBuffer.getFrame();
                if (frame == null) {
                    mLock.unlock();
                    continue;
                }
                mLock.unlock();

                // Decode the frame
                short[] decoded = mDecoder.decodeAndGetDecoded(frame, frame.length);
                if (decoded == null)
                    continue;

                // Send the decoded frame to the audio track
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

    /**
     * Stops the thread
     */
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

    private void adjustVolume() {
        this.mAudioLock.lock();
        float v = mMinVolume + (mMaxVolume - mMinVolume) * volume;
        mAudioTrack.setStereoVolume(v, v);
        this.mAudioLock.unlock();
    }
}
