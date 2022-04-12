package de.lme.heartnhealth4u;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import java.util.HashSet;
import java.util.Set;

// 이미 스마트폰에 페어링된 블루투스 기기가 있는지 보여주는 코드

public class ShimmerManager extends Object
{
	private final BluetoothAdapter	mmBluetoothAdapter;
	private BluetoothDevice device;

	public ShimmerManager ()
	{
		mmBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
	}


	private final String DEVICE_ADDRESS1="0C:8C:DC:38:93:7E";
	boolean found;

	public Set< ShimmerDataSource > getShimmerDevices ()
	{
		Set< ShimmerDataSource > mmShimmerDevices;
		mmShimmerDevices = new HashSet< ShimmerDataSource >();
		mmShimmerDevices.clear();

		Set< BluetoothDevice > pairedDevices = mmBluetoothAdapter.getBondedDevices();

		/*
		for (BluetoothDevice iterator : pairedDevices)
		{
			if(iterator.getAddress().equals(DEVICE_ADDRESS1))
			{
				device=iterator;
				found=true;
				break;
			}
		}
		*/

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