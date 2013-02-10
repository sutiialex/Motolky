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
import com.motolkyadfree.R;

/**
 * The adapter used by the list from the Group selecting Activity
 *
 * @author Alexandru Sutii
 *
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
     * Gets a list item
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        GroupActivity.mDeviceListLock.lock();
        if (v == null) {
            LayoutInflater li = (LayoutInflater)mContext.
                                    getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = li.inflate(R.layout.group_device, null);
            CheckBox cbPicked = (CheckBox)v.findViewById(R.id.devicePicked);

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
                        if (nr >= Common.MAX_GROUP_MEMBERS) {
                            picked = false;
                            mNotifyHandler.showText("A group may contain maximum " +
                                    (Common.MAX_GROUP_MEMBERS+1) + " members");
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
                textPaint.getTextBounds("Unpaired",0,"Unpaired".length(),bounds);
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
