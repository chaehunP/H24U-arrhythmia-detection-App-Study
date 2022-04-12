/*
 * Copyright (C) 2009 The Android Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package de.lme.heartnhealth4u;


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Set;

// SCAN을 눌렀을 때 블루투스 기기로써 검색되는 장치들의 목록을 보여주는 코드

/**
 * This Activity appears as a dialog. It lists any paired devices and devices detected in the area after discovery. When
 * a device is chosen by the user, the MAC address of the device is sent back to the parent Activity in the result
 * Intent.
 *
 * 이 활동은 대화 상자로 나타납니다. 검색 후 영역에서 감지된 페어링된 장치 및 장치를 나열합니다.
 * 사용자가 장치를 선택하면 장치의 MAC 주소가 결과 Intent의 상위 Activity로 다시 전송됩니다.
 */
public class DeviceListActivity extends Activity
{
	// Debugging
	private static final String		TAG						= "DeviceListActivity";
	private static final boolean	D						= true;

	// Return Intent extra
	public static String			EXTRA_DEVICE_ADDRESS	= "device_address";

	// Member fields
	private BluetoothAdapter		mBtAdapter;
	private ArrayAdapter< String >	mPairedDevicesArrayAdapter;
	private ArrayAdapter< String >	mNewDevicesArrayAdapter;


	@Override
	protected void onCreate (Bundle savedInstanceState)
	{
		super.onCreate( savedInstanceState );

		// Setup the window
		requestWindowFeature( Window.FEATURE_INDETERMINATE_PROGRESS );
		setContentView( R.layout.device_list );

		// Set result CANCELED in case the user backs out    사용자가 취소하는 경우 결과를 CANCELED로 설정합니다.
		setResult( Activity.RESULT_CANCELED );

		// Initialize the button to perform device discovery    버튼을 초기화하여 장치 검색을 수행합니다.
		Button scanButton = (Button) findViewById( R.id.button_scan );
		scanButton.setOnClickListener( new OnClickListener() {
			public void onClick (View v)
			{
				doDiscovery();
				v.setVisibility( View.GONE );
			}
		} );

		// Initialize array adapters. One for already paired devices and one for newly discovered devices
		// 배열 어댑터를 초기화합니다. 하나는 이미 페어링된 장치용이고 하나는 새로 검색된 장치용
		mPairedDevicesArrayAdapter = new ArrayAdapter< String >( this, R.layout.device_name );
		mNewDevicesArrayAdapter = new ArrayAdapter< String >( this, R.layout.device_name );

		// Find and set up the ListView for paired devices    페어링된 장치에 대한 ListView 찾기 및 설정
		ListView pairedListView = (ListView) findViewById( R.id.paired_devices );
		pairedListView.setAdapter( mPairedDevicesArrayAdapter );
		pairedListView.setOnItemClickListener( mDeviceClickListener );

		// Find and set up the ListView for newly discovered devices    새로 검색된 장치에 대한 ListView 찾기 및 설정
		ListView newDevicesListView = (ListView) findViewById( R.id.new_devices );
		newDevicesListView.setAdapter( mNewDevicesArrayAdapter );
		newDevicesListView.setOnItemClickListener( mDeviceClickListener );

		// Register for broadcasts when a device is discovered    장치가 발견되면 브로드캐스트 등록
		IntentFilter filter = new IntentFilter( BluetoothDevice.ACTION_FOUND );
		this.registerReceiver( mReceiver, filter );

		// Register for broadcasts when discovery has finished    검색이 완료되면 브로드캐스트 등록
		filter = new IntentFilter( BluetoothAdapter.ACTION_DISCOVERY_FINISHED );
		this.registerReceiver( mReceiver, filter );

		// Get the local Bluetooth adapter    로컬 블루투스 어댑터 받기
		mBtAdapter = BluetoothAdapter.getDefaultAdapter();

		// Get a set of currently paired devices    현재 페어링된 기기 세트 가져오기
		Set< BluetoothDevice > pairedDevices = mBtAdapter.getBondedDevices();

		// If there are paired devices, add each one to the ArrayAdapter    페어링된 장치가 있는 경우 각각을 ArrayAdapter에 추가합니다.
		if (pairedDevices.size() > 0)
		{
			findViewById( R.id.title_paired_devices ).setVisibility( View.VISIBLE );
			for (BluetoothDevice device : pairedDevices)
			{
				mPairedDevicesArrayAdapter.add( device.getName() + "\n" + device.getAddress()  );  // type: classic(1), LE(2), DUAL(3), UNKNOWN(4)
			}
		}
		else
		{
			String noDevices = "No Devices paired";
			mPairedDevicesArrayAdapter.add( noDevices );
		}
	}


	@Override
	protected void onDestroy ()
	{
		super.onDestroy();

		// Make sure we're not doing discovery anymore    더 이상 검색을 하지 않도록 하세요.
		if (mBtAdapter != null)
		{
			mBtAdapter.cancelDiscovery();
		}

		// Unregister broadcast listeners    브로드캐스트 리스너 등록 취소
		this.unregisterReceiver( mReceiver );
	}


	/**
	 * Start device discover with the BluetoothAdapter   BluetoothAdapter로 장치 검색 시작
	 */
	private void doDiscovery ()
	{
		if (D)
			Log.d( TAG, "doDiscovery()" );

		// Indicate scanning in the title    제목에 스캔 표시
		setProgressBarIndeterminateVisibility( true );
		setTitle( "Scanning..." );

		// Turn on sub-title for new devices    새 기기의 자막 켜기
		findViewById( R.id.title_new_devices ).setVisibility( View.VISIBLE );

		// If we're already discovering, stop it   이미 발견되었다면 멈추기
		if (mBtAdapter.isDiscovering())
		{
			mBtAdapter.cancelDiscovery();
		}

		// Request discover from BluetoothAdapter    BluetoothAdapter에서 검색 요청
		mBtAdapter.startDiscovery();
	}


	// The on-click listener for all devices in the ListViews
	private OnItemClickListener		mDeviceClickListener	= new OnItemClickListener() {
																public void onItemClick (AdapterView< ? > av, View v, int arg2,
																		long arg3)
																{
																	// Cancel discovery because it's costly and we're about to connect
																	// 비용이 많이 들고 연결하려고 하므로 검색을 취소합니다.
																	mBtAdapter.cancelDiscovery();

																	// Get the device MAC address, which is the last 17 chars in the View
																	// 뷰의 마지막 17자인 장치 MAC 주소를 가져옵니다.
																	String info = ((TextView) v).getText().toString();
																	String address = info.substring( info.length() - 17 );

																	// Create the result Intent and include the MAC address
																	// 결과 intent를 만들고 MAC 주소를 포함합니다.
																	Intent intent = new Intent();
																	intent.putExtra( EXTRA_DEVICE_ADDRESS, address );

																	// Set result and finish this Activity
																	// 결과를 설정하고 이 활동을 완료하십시오.
																	setResult( Activity.RESULT_OK, intent );
																	finish();
																}
															};

	// The BroadcastReceiver that listens for discovered devices and changes the title when discovery is finished
	// 검색된 장치를 수신 대기하고 검색이 완료되면 제목을 변경하는 BroadcastReceiver
	private final BroadcastReceiver	mReceiver				= new BroadcastReceiver() {
																@Override
																public void onReceive (Context context, Intent intent)
																{
																	String action = intent.getAction();

																	// When discovery finds a device   검색에서 장치를 찾을 때
																	if (BluetoothDevice.ACTION_FOUND.equals( action ))
																	{
																		// Get the BluetoothDevice object from the Intent
																		// 인텐트에서 BluetoothDevice 개체 가져오기
																		BluetoothDevice device = intent.getParcelableExtra( BluetoothDevice.EXTRA_DEVICE );

																		// If it's already paired, skip it, because it's been listed already
																		// 이미 페어링된 경우 이미 나열되어 있으므로 건너뜁니다.
																		if (device.getBondState() != BluetoothDevice.BOND_BONDED)
																		{
																			mNewDevicesArrayAdapter.add( device.getName() + "\n" + device.getAddress() );
																		}
																		// When discovery is finished, change the Activity title
																		// 검색이 완료되면 활동 제목을 변경합니다.
																	}
																	else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals( action ))
																	{
																		setProgressBarIndeterminateVisibility( false );
																		setTitle( "Select Device" );
																		if (mNewDevicesArrayAdapter.getCount() == 0)
																		{
																			String noDevices = "None Found";
																			mNewDevicesArrayAdapter.add( noDevices );
																		}
																	}
																}
															};

}
