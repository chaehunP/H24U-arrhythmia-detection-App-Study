package de.lme.heartnhealth4u;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import java.util.HashSet;
import java.util.Set;


public class ShimmerManager extends Object
{
	private final BluetoothAdapter	mmBluetoothAdapter;


	public ShimmerManager ()
	{
		mmBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
	}


	public Set< ShimmerDataSource > getShimmerDevices ()
	{
		Set< ShimmerDataSource > mmShimmerDevices;
		mmShimmerDevices = new HashSet< ShimmerDataSource >();
		mmShimmerDevices.clear();

		Set< BluetoothDevice > pairedDevices = mmBluetoothAdapter.getBondedDevices();

		if (pairedDevices.size() > 0)
		{
			for (BluetoothDevice device : pairedDevices)
			{
				mmShimmerDevices.add( new de.lme.heartnhealth4u.ShimmerConnectDevice( device ) );
			}
		}

		return mmShimmerDevices;
	}


	public ShimmerDataSource getShimmerDeviceByID (String id)
	{
		Set< BluetoothDevice > pairedDevices = mmBluetoothAdapter.getBondedDevices();

		// mBluetoothAdapter.getRemoteDevice(address);

		if (pairedDevices.size() > 0)
		{
			for (BluetoothDevice device : pairedDevices)
			{
				if (device.getName().equals( id ))
					return new de.lme.heartnhealth4u.ShimmerConnectDevice( device );
			}
		}

		return null;
	}


	public ShimmerDataSource getShimmerDeviceByAddress (String address)
	{
		Set< BluetoothDevice > pairedDevices = mmBluetoothAdapter.getBondedDevices();

		// mBluetoothAdapter.getRemoteDevice(address);

		if (pairedDevices.size() > 0)
		{
			for (BluetoothDevice device : pairedDevices)
			{
				if (device.getAddress().equals( address ))
					return new de.lme.heartnhealth4u.ShimmerConnectDevice( device );
			}
		}

		return null;
	}

}