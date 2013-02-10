package com.motolky;

import android.media.AudioFormat;

/**
 *
 * @author Alexandru Sutii
 *
 * The class containing the constants used by the application
 */
public class Common {
    public static final String TAG = "motolky";
    public static final int SAMPLE_RATE = 8000;
    public static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    public static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_CONFIGURATION_MONO;
    public static final int AUDIO_BUFFER_LEN = 320;
    public static final int MAX_GROUP_MEMBERS = 6;
    public static final int DISCOVERABLE_TIMEOUT = 300; // Seconds
    public static       int RECONNECT_TIMEOUT = 3000;   // miliseconds
    public static final int REQUEST_ENABLE_BT = 1;
    public static final int REQUEST_ENABLE_DISCOVERY = 2;
    public static final int REQUEST_CHANGE_PREFERENCES = 3;

    public static int RECONNECT_MIN_TIMEOUT = 3000;   // miliseconds
    public static int RECONNECT_MAX_TIMEOUT = 10000;
    public static final int TIMES_TO_USE_MIN_RECONNECT_TIMEOUT = 100;
    
    public static boolean ENABLE_VAD = false;
    
    public static final byte SEPARATOR = 127;
}
