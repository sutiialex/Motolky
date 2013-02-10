package com.motolky;

import android.content.Context;
import android.widget.Toast;

/**
 * A class that gets a message and displays it in a toast in
 * a specified context.
 *
 * @author Alexandru Sutii
 *
 */
public class ToastViewer implements Runnable {
    private Context mContext;
    private String mText;
    private int mToastTimeout;

    public ToastViewer(Context context, String text, int lengthShort) {
        mText = text;
        mContext = context;
        mToastTimeout = lengthShort;
    }


    @Override
    public void run() {
        Toast.makeText(mContext, mText, mToastTimeout).show();
    }

}
