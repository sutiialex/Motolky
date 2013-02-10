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
