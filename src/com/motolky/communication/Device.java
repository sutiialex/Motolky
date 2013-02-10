package com.motolky.communication;

import java.util.UUID;

import android.bluetooth.BluetoothDevice;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * This class encapsulates a bluetooth device. It also contains
 * informations about it's status.
 *
 * @author Alexandru Sutii
 *
 */
public class Device implements Parcelable {
    public static final Parcelable.Creator<Device> CREATOR =
        new Creator<Device>() {

            @Override
            public Device[] newArray(int size) {
                return new Device[size];
            }

            @Override
            public Device createFromParcel(Parcel source) {
                return new Device(source);
            }
        };

    private BluetoothDevice mBtDevice;
    private boolean mPaired;
    private boolean mPicked;
    private boolean mConnected = false;
    private UUID mUUID = null;

    public Device(BluetoothDevice btDevice, boolean paired) {
        mBtDevice = btDevice;
        mPaired = paired;
    }

    public Device(Parcel parcel) {
        mBtDevice = parcel.readParcelable(Device.class.getClassLoader());
        boolean[] b = new boolean[3];
        parcel.readBooleanArray(b);
        mPaired = b[0];
        mConnected = b[1];
        mPicked = b[2];
    }

    @Override
    public String toString() {
        return getName() + "    " + getStatus();
    }

    public String getName() {
        return mBtDevice.getName();
    }

    public BluetoothDevice getBluetoothDevice() {
        return mBtDevice;
    }

    public String getStatus() {
        return (mPaired ? "Paired" : "Unpaired");
    }

    public boolean getPicked() {
        return mPicked;
    }

    public void setPicked(boolean picked) {
        mPicked = picked;
    }

    public boolean getConnected() {
        return mConnected;
    }

    public void setConnected(boolean connected) {
        mConnected = connected;
    }

    public void setUUID(UUID uuid) {
        mUUID = uuid;
    }

    public UUID getUUID() {
        return mUUID;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(mBtDevice, flags);
        dest.writeBooleanArray(new boolean[] { mPaired, mConnected, mPicked });
    }
}
