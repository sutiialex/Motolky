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
import com.motolkyadfree.R;

/**
 * The adapter for the list contained by the Talk Activity
 *
 * @author Alexandru Sutii
 *
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
