/**
 * 
 */
package com.antonio081014.androidbluetooth;

import android.bluetooth.BluetoothDevice;

/**
 * @author Antonio081014
 * @time: May 14, 2013, 2:03:37 PM
 */
class CustomizedBluetoothDevice {
    private String name;
    private String address;
    private boolean statusPaired;

    public CustomizedBluetoothDevice(BluetoothDevice device) {
	this.name = device.getName();
	this.address = device.getAddress();
	this.statusPaired = device.getBondState() == BluetoothDevice.BOND_BONDED;
    }

    @Override
    public int hashCode() {
	final int prime = 31;
	int result = 1;
	result = prime * result + ((address == null) ? 0 : address.hashCode());
	return result;
    }

    @Override
    public boolean equals(Object obj) {
	if (this == obj) {
	    return true;
	}
	if (obj == null) {
	    return false;
	}
	if (!(obj instanceof CustomizedBluetoothDevice)) {
	    return false;
	}
	CustomizedBluetoothDevice other = (CustomizedBluetoothDevice) obj;
	if (address == null) {
	    if (other.address != null) {
		return false;
	    }
	} else if (!address.equals(other.address)) {
	    return false;
	}
	return true;
    }

    /**
     * @return the name
     */
    public String getName() {
	return name;
    }

    /**
     * @param name
     *            the name to set
     */
    public void setName(String name) {
	this.name = name;
    }

    /**
     * @return the address
     */
    public String getAddress() {
	return address;
    }

    /**
     * @param address
     *            the address to set
     */
    public void setAddress(String address) {
	this.address = address;
    }

    /**
     * @return the statusPaired
     */
    public boolean isStatusPaired() {
	return statusPaired;
    }

    /**
     * @param statusPaired
     *            the statusPaired to set
     */
    public void setStatusPaired(boolean statusPaired) {
	this.statusPaired = statusPaired;
    }
}