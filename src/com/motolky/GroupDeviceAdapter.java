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
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.motolky.communication.Device;

/**
 * The adapter used by the list displaying the discovered bluetooth devices.
 * This list appears in the first activity of the app (GroupActivity).
 */
public class GroupDeviceAdapter extends DeviceAdapter {
    private int nameWidth = -1;

    /**
     * Constructor
     * @param context - the context to run in
     * @param items - the list with the items
     * @param notifiable - the handler that will display the texts
     */
    public GroupDeviceAdapter(Context context, List<Device> items, INotifiable notifiable,
            int listWidth) {
        super(context, R.layout.group_device, items, notifiable, listWidth);
    }

    /**
     * Returns a formated list item
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        GroupActivity.mDeviceListLock.lock();
        if (v == null) {
            // Get the layout
            LayoutInflater li = (LayoutInflater)mContext.
                    getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = li.inflate(R.layout.group_device, null);
            // The checkbox stating whether the device was picked to go in the
            // talk group
            CheckBox cbPicked = (CheckBox)v.findViewById(R.id.devicePicked);

            // Set the on click handler
            cbPicked.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

                /**
                 * On click handler for the check buttons
                 */
                 @Override
                 public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                     GroupActivity.mDeviceListLock.lock();
                     boolean picked = isChecked;
                     View parentView  = (View)buttonView.getParent();
                     String deviceName = ((TextView)parentView.findViewById(R.id.deviceName)).getText().toString();

                     // Look for the clicked device in the list of devices in the activity
                     Device changedDevice = null;
                     for (Device device : mItems)
                         if (device.getName().equals(deviceName)) {
                             changedDevice = device;
                             break;
                         }

                     if (picked) {
                         // Check whether there aren't too many members in the group
                         int nr = 0;
                         for (Device device : mItems)
                             if (device.getPicked() && !device.equals(changedDevice))
                                 nr++;
                         if (nr >= Common.MAX_GROUP_MEMBERS - 1) {
                             picked = false;
                             mNotifyHandler.showText("A group may contain maximum " +
                                     Common.MAX_GROUP_MEMBERS + " members");
                         }
                     }

                     // Mark device as picked or not
                     changedDevice.setPicked(picked);
                     buttonView.setChecked(picked);
                     GroupActivity.mDeviceListLock.unlock();
                 }
            });
        }

        // Populate the list member
        Device device = mItems.get(position);
        if (device != null) {
            TextView name = (TextView)v.findViewById(R.id.deviceName);
            TextView status = (TextView)v.findViewById(R.id.deviceStatus);
            CheckBox cbPicked = (CheckBox)v.findViewById(R.id.devicePicked);
            status.setText(device.getStatus());
            name.setText(device.getName());

            if (nameWidth == -1) {
                Rect bounds = new Rect();
                Paint textPaint = status.getPaint();
                textPaint.getTextBounds("Unpaired", 0, "Unpaired".length(), bounds);
                nameWidth = bounds.width();
                nameWidth = (mListWidth - nameWidth)/2 - 10;
            }

            name.setWidth(nameWidth);
            cbPicked.setChecked(device.getPicked());
        }
        GroupActivity.mDeviceListLock.unlock();
        return v;
    }
}
