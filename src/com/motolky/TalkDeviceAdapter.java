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

import java.util.List;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.motolky.communication.Device;

/**
 * The adapter for the list contained by the Talk Activity
 */
public class TalkDeviceAdapter extends DeviceAdapter {
    private int nameWidth = -1;

    /**
     * Constructor
     * @param context - the context to run in
     * @param items - the list with the items
     * @param notifiable - the handler that will display the texts
     */
    public TalkDeviceAdapter(Context context, List<Device> items, INotifiable notifiable,
            int listWidth) {
        super(context, R.layout.talk_device, items, notifiable, listWidth);
    }

    /**
     * Gets a list item
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        if (v == null) {
            LayoutInflater li = (LayoutInflater)mContext.
                                    getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = li.inflate(R.layout.talk_device, null);
        }

        // Populate the list member
        Device device = mItems.get(position);
        if (device != null) {
            TextView name = (TextView)v.findViewById(R.id.deviceName);
            TextView status = (TextView)v.findViewById(R.id.deviceStatus);
            ProgressBar progressBar = (ProgressBar)v.findViewById(R.id.deviceProgressBar);
            status.setVisibility(!device.getConnected() ? View.INVISIBLE : View.VISIBLE);
            progressBar.setVisibility(device.getConnected() ? View.INVISIBLE : View.VISIBLE);
            name.setText(device.getName());

            if (nameWidth == -1) {
                Rect bounds = new Rect();
                Paint textPaint = status.getPaint();
                textPaint.getTextBounds("Connected",0,"Connected".length(),bounds);
                nameWidth = mListWidth - bounds.width() - 20;
            }
            name.setWidth(nameWidth);
        }
        return v;
    }
}
