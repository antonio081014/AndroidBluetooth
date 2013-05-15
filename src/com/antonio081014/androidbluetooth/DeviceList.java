package com.antonio081014.androidbluetooth;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * @author Antonio081014
 * @time: May 14, 2013, 2:03:37 PM
 */

public class DeviceList extends Activity {

    private static final String TAG = "DeviceListActivity";
    private static final boolean D = false;

    private static final int REQUEST_ENABLE_BT = 2;
    protected static final String PREFS_NAME = "Antonio081014 Bluetooth Android";
    protected static final String PREFS_DEVICE_ADDR = "Antonio081014 Bluetooth Address";

    private ArrayList<CustomizedBluetoothDevice> mDeviceList;

    private BluetoothAdapter mBluetoothAdapter;
    private ListView mListViewDeviceList;
    private Button mButtonStartScan;
    private int currentPosition;

    private SharedPreferences settings;

    BaseAdapter mBaseAdapter = new BaseAdapter() {

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
	    LayoutInflater mInflater = (LayoutInflater) getApplicationContext()
		    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

	    CustomizedBluetoothDevice device = mDeviceList.get(position);

	    View rowView = mInflater.inflate(R.layout.macaddr1, parent, false);
	    if (device.isStatusPaired() == false)
		rowView = mInflater.inflate(R.layout.macaddr2, parent, false);

	    TextView name = (TextView) rowView.findViewById(R.id.tv_addr_Name);
	    TextView addr = (TextView) rowView.findViewById(R.id.tv_addr_ID);
	    String status = device.isStatusPaired() ? "Paired" : "Not Paired";

	    name.setText(status + ": " + device.getName() + ".");
	    addr.setText(device.getAddress());
	    return rowView;
	}

	@Override
	public long getItemId(int position) {
	    return 0;
	}

	@Override
	public Object getItem(int position) {
	    return mDeviceList.get(position);
	}

	@Override
	public int getCount() {
	    if (mDeviceList != null)
		return mDeviceList.size();
	    return 0;
	}
    };

    private void updateUI() {
	mBaseAdapter.notifyDataSetChanged();
    }

    private void getPairedDevice() {
	Set<BluetoothDevice> pairedDevices = mBluetoothAdapter
		.getBondedDevices();
	if (pairedDevices.size() > 0) {
	    for (BluetoothDevice device : pairedDevices) {
		CustomizedBluetoothDevice customizedDevice = new CustomizedBluetoothDevice(
			device);
		mDeviceList.add(customizedDevice);
	    }
	}
    }

    private void initialization() {
	mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
	if (mBluetoothAdapter == null) {
	    finish();
	    return;
	}

	mListViewDeviceList = (ListView) findViewById(R.id.listview_devicelist);
	mListViewDeviceList.setAdapter(mBaseAdapter);
	mListViewDeviceList.setOnItemClickListener(mDeviceClickListener);

	mButtonStartScan = (Button) findViewById(R.id.btn_startScan);
	mButtonStartScan.setOnClickListener(new View.OnClickListener() {
	    @Override
	    public void onClick(View v) {
		setup();
		doDiscovery();
	    }
	});

	settings = getSharedPreferences(DeviceList.PREFS_NAME, 0);

	// Register the BroadcastReceiver
	IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
	registerReceiver(mReceiver, filter);

	// Register for broadcasts when discovery has finished
	filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
	this.registerReceiver(mReceiver, filter);

	filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
	this.registerReceiver(mReceiver, filter);
    }

    private void startConnect(CustomizedBluetoothDevice device) {
	SharedPreferences.Editor editor = settings.edit();
	editor.putString(DeviceList.PREFS_DEVICE_ADDR, device.getAddress());
	editor.commit();
	startActivity(new Intent(getApplicationContext(), Console.class));
    }

    // The on-click listener for all devices in the ListViews
    // It will auto-connect with the device.
    private OnItemClickListener mDeviceClickListener = new OnItemClickListener() {
	public void onItemClick(AdapterView<?> av, View v, int position,
		long arg3) {
	    // Cancel discovery because it's costly and we're about to connect
	    mBluetoothAdapter.cancelDiscovery();
	    BluetoothDevice device = mBluetoothAdapter
		    .getRemoteDevice(mDeviceList.get(position).getAddress());

	    currentPosition = position;
	    if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
		unpairDevice(device);
	    } else {
		pairDevice(device);
	    }
	}
    };

    /**
     * Start device discover with the BluetoothAdapter
     */
    private void doDiscovery() {
	// If we're already discovering, stop it
	if (mBluetoothAdapter.isDiscovering()) {
	    mBluetoothAdapter.cancelDiscovery();
	}

	// Request discover from BluetoothAdapter
	mBluetoothAdapter.startDiscovery();
	// showDialog(BIND_ABOVE_CLIENT);
    }

    private void pairDevice(BluetoothDevice device) {
	try {
	    if (D)
		Log.d(TAG, "Start Pairing...");

	    Method m = device.getClass()
		    .getMethod("createBond", (Class[]) null);
	    m.invoke(device, (Object[]) null);
	} catch (Exception e) {
	    Log.e(TAG, e.getMessage());
	}
    }

    private void unpairDevice(BluetoothDevice device) {
	try {
	    Method m = device.getClass()
		    .getMethod("removeBond", (Class[]) null);
	    m.invoke(device, (Object[]) null);
	} catch (Exception e) {
	    Log.e(TAG, e.getMessage());
	}
    }

    // The BroadcastReceiver that listens for discovered devices and
    // changes the title when discovery is finished
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
	@Override
	public void onReceive(Context context, Intent intent) {
	    String action = intent.getAction();

	    // When discovery finds a device
	    if (BluetoothDevice.ACTION_FOUND.equals(action)) {
		// Get the BluetoothDevice object from the Intent
		BluetoothDevice device = intent
			.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
		// If it's already paired, skip it, because it's been listed
		// already
		CustomizedBluetoothDevice mDevice = new CustomizedBluetoothDevice(
			device);
		if (device.getBondState() != BluetoothDevice.BOND_BONDED) {

		    if (mDeviceList.contains(mDevice) == false) {
			mDeviceList.add(mDevice);
			updateUI();
		    }
		}
	    }
	    // When the device bond state changed.
	    else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
		int prevBondState = intent.getIntExtra(
			BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, -1);
		int bondState = intent.getIntExtra(
			BluetoothDevice.EXTRA_BOND_STATE, -1);

		if (prevBondState == BluetoothDevice.BOND_BONDED
			&& bondState == BluetoothDevice.BOND_NONE) {
		    BluetoothDevice device = intent
			    .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
		    if (currentPosition != -1
			    && currentPosition < mDeviceList.size()) {
			CustomizedBluetoothDevice mDevice = mDeviceList
				.get(currentPosition);
			if (device.getAddress().compareTo(mDevice.getAddress()) == 0) {
			    mDevice.setStatusPaired(false);
			    updateUI();
			    pairDevice(device);
			}
		    }
		} else if (prevBondState == BluetoothDevice.BOND_BONDING
			&& bondState == BluetoothDevice.BOND_BONDED) {
		    BluetoothDevice device = intent
			    .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
		    if (currentPosition != -1
			    && currentPosition < mDeviceList.size()) {
			CustomizedBluetoothDevice mDevice = mDeviceList
				.get(currentPosition);
			if (device.getAddress().compareTo(mDevice.getAddress()) == 0) {
			    mDevice.setStatusPaired(true);
			    updateUI();
			    startConnect(mDevice);
			}
		    }
		}
	    }
	}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	setContentView(R.layout.activity_device_list);
	initialization();
    }

    @Override
    protected void onStart() {
	super.onStart();

	if (!mBluetoothAdapter.isEnabled()) {
	    Intent enableIntent = new Intent(
		    BluetoothAdapter.ACTION_REQUEST_ENABLE);
	    startActivityForResult(enableIntent, DeviceList.REQUEST_ENABLE_BT);
	    // Otherwise, setup the chat session
	} else {
	    setup();
	}
    }

    private void setup() {
	mDeviceList = new ArrayList<CustomizedBluetoothDevice>();
	currentPosition = -1;
	getPairedDevice();
	updateUI();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
	getMenuInflater().inflate(R.menu.device_list, menu);
	return true;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
	switch (requestCode) {
	case DeviceList.REQUEST_ENABLE_BT:
	    // When the request to enable Bluetooth returns
	    if (resultCode == Activity.RESULT_OK) {
		// Bluetooth is now enabled, so set up a chat session
		setup();
	    } else {
		// User did not enable Bluetooth or an error occured
		if (D)
		    Log.d(TAG, "BT not enabled");
		Toast.makeText(this, R.string.bt_not_enabled_leaving,
			Toast.LENGTH_SHORT).show();
		finish();
	    }
	}
	super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onDestroy() {
	super.onDestroy();

	// Make sure we're not doing discovery anymore
	if (mBluetoothAdapter != null) {
	    mBluetoothAdapter.cancelDiscovery();
	}

	// Unregister broadcast listeners
	this.unregisterReceiver(mReceiver);
    }
}
