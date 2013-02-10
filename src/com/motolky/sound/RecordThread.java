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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.motolky.Common;
import com.motolky.communication.ISendHandler;


import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

/**
 * This class is a thread that creates an AudioRecord object.
 * It continuously reads data from this object and sends it
 * to its send handlers.
 *
 * @author Alexandru Sutii
 *
 */
public class RecordThread extends Thread {
    private AudioRecord mAudioRecord = null;
    private boolean mExit = false;
    private ISoundProcessor mSoundProcessor = null;
    private List<ISendHandler> mSendHandlers = null;
    private final Lock mLock = new ReentrantLock();
    private boolean mRecord = true;

    public RecordThread(int maxBufferLen) {
        mSendHandlers = new ArrayList<ISendHandler>();
        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                Common.SAMPLE_RATE, Common.CHANNEL_CONFIG, Common.AUDIO_FORMAT,
                AudioRecord.getMinBufferSize(Common.SAMPLE_RATE,
                        Common.CHANNEL_CONFIG, Common.AUDIO_FORMAT) + 4096);
        mSoundProcessor = new SoundProcessor(maxBufferLen);
    }

    public void addSendHandler(ISendHandler sendHandler) {
        mLock.lock();
        if (mSendHandlers != null) {
            mSendHandlers.add(sendHandler);
            if (mAudioRecord != null && mRecord && mAudioRecord.getRecordingState() == AudioRecord.RECORDSTATE_STOPPED)
                mAudioRecord.startRecording();
        }
        mLock.unlock();
    }

    public void removeSendHandler(ISendHandler sendHandler) {
        mLock.lock();
        if (mSendHandlers != null) {
            mSendHandlers.remove(sendHandler);
            if (mSendHandlers.isEmpty() && mAudioRecord != null &&
                    mAudioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING)
                mAudioRecord.stop();
        }
        mLock.unlock();
    }

    public void exit() {
        try {
            mLock.lock();
            if (mAudioRecord != null) {
                if (mAudioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING)
                    mAudioRecord.stop();
                mAudioRecord.release();
                mAudioRecord = null;
            }
            mExit = true;
            mSendHandlers = null;
            mLock.unlock();
            this.mSoundProcessor.exit();
        } catch (Exception e) {
        	Log.e(Common.TAG, e.getMessage());
        }
    }

    @Override
    public void run() {
        short[] buffer = new short[Common.AUDIO_BUFFER_LEN];
        byte[] procBuffer = new byte[buffer.length*2];
        try {
            while (!mExit) {
                while (true) {
                    mLock.lock();
                    if (mAudioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING || mExit) {
                        mLock.unlock();
                        break;
                    }
                    mLock.unlock();
                    sleep(500);
                }

                mLock.lock();
                int no = mAudioRecord.read(buffer, 0, Common.AUDIO_BUFFER_LEN);
                mLock.unlock();

                mSoundProcessor.addRawSound(buffer, no);
                no = mSoundProcessor.getProcessedSound(procBuffer, Common.AUDIO_BUFFER_LEN);
            
                if (no > 0)
                    sendTraffic(procBuffer, no);
            }
        } catch (Exception e) {
            Log.e(Common.TAG, "Exception reading audio record: " + e.getMessage());
            try {
                mLock.unlock();
            } catch (Exception e1) {}
            exit();
        }
    }

    private void sendTraffic(byte[] data, int no) {
        mLock.lock();
        for (ISendHandler sendHandler : mSendHandlers)
            sendHandler.sendData(data, no);
        mLock.unlock();
    }

    public void setRecordState(boolean state) {
        mRecord = state;
        mLock.lock();
        if (mAudioRecord == null) {
            mLock.unlock();
            return;
        }
        if (!mRecord) {
            if (mAudioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING)
                mAudioRecord.stop();
        } else {
            if (mAudioRecord.getRecordingState() == AudioRecord.RECORDSTATE_STOPPED &&
                    !mSendHandlers.isEmpty())
                mAudioRecord.startRecording();
        }
        mLock.unlock();
    }
}
