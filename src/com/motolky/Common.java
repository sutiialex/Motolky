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

package com.motolky;

import android.media.AudioFormat;

/**
 * The class containing the constants used by the application
 */
public class Common {
    public static final String TAG = "motolky";
    public static final int SAMPLE_RATE = 8000;
    public static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    @SuppressWarnings("deprecation")
    public static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_CONFIGURATION_MONO;
    public static final int AUDIO_BUFFER_LEN = 320;
    public static final int MAX_GROUP_MEMBERS = 7;
    public static final int DISCOVERABLE_TIMEOUT = 300; // Seconds
    public static       int RECONNECT_TIMEOUT = 3000;   // miliseconds
    public static final int REQUEST_ENABLE_BT = 1;
    public static final int REQUEST_ENABLE_DISCOVERY = 2;
    public static final int REQUEST_CHANGE_PREFERENCES = 3;
    public static int RECONNECT_MIN_TIMEOUT = 3000;   // miliseconds
    public static int RECONNECT_MAX_TIMEOUT = 10000;  // miliseconds
    public static final int TIMES_TO_USE_MIN_RECONNECT_TIMEOUT = 100;

    public static boolean ENABLE_VAD = false;
    public static final byte SEPARATOR = 127;
}
