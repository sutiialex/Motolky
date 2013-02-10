package com.motolky;

import java.util.List;

import com.motolky.communication.Device;

import android.content.Context;
import android.widget.ArrayAdapter;

/**
 *
 * @author Alexandru Sutii
 *
 * Adapter for a list containing devices (a class describing a bluetooth device)
 */
public class DeviceAdapter extends ArrayAdapter<Device> {
    protected final List<Device> mItems;
    protected final INotifiable mNotifyHandler;
    protected Context mContext;
    protected int mListWidth;

    /**
     * Constructor
     * @param context - the context to run in
     * @param textViewResourceId - item type
     * @param items - the list with the items
     * @param notifiable - the handler that will display the texts
     */
    public DeviceAdapter(Context context, int textViewResourceId, List<Device> items,
            INotifiable notifiable, int listWidth) {
        super(context, textViewResourceId, items);
        mContext = context;
        this.mItems = items;
        this.mListWidth = listWidth;
        mNotifyHandler = notifiable;
    }
}
