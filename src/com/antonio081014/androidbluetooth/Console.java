/**
 * 
 */
package com.antonio081014.androidbluetooth;

import java.util.ArrayList;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * @author Antonio081014
 * @time: May 14, 2013, 2:44:35 PM
 */
public class Console extends Activity {

    // Debugging
    private static final String TAG = "BluetoothMesseging";
    private static final boolean D = false;

    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    private ListView mListviewMesseges;
    private Button mButtonSend;
    private EditText mEditTextMessege;
    private TextView mTextViewTitle;
    private TextView mTextViewStatus;

    // Store the current connected Device Address;
    private String mCurrentDeviceAddress = null;
    // Name of the connected device
    private String mConnectedDeviceName = null;
    private ConnectThread mConnectThread;
    private SharedPreferences settings;
    // private String bufferMessege;

    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the chat services
    private BluetoothMeterService mChatService = null;

    class CustomizedMessage {
	public String message;
	public boolean received;

	public CustomizedMessage(String m, boolean r) {
	    this.message = m;
	    this.received = r;
	}
    }

    private ArrayList<CustomizedMessage> mMessage;

    BaseAdapter mBaseAdapter = new BaseAdapter() {

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
	    LayoutInflater mInflater = (LayoutInflater) getApplicationContext()
		    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

	    CustomizedMessage message = mMessage.get(position);

	    View rowView = mInflater.inflate(R.layout.macaddr1, parent, false);
	    if (message.received == false)
		rowView = mInflater.inflate(R.layout.macaddr2, parent, false);

	    TextView name = (TextView) rowView.findViewById(R.id.tv_addr_Name);
	    TextView addr = (TextView) rowView.findViewById(R.id.tv_addr_ID);
	    String status = message.received ? mConnectedDeviceName : "Me";

	    name.setText(status);
	    addr.setText(message.message);
	    return rowView;
	}

	@Override
	public long getItemId(int position) {
	    return position;
	}

	@Override
	public Object getItem(int position) {
	    return mMessage.get(position);
	}

	@Override
	public int getCount() {
	    if (mMessage != null)
		return mMessage.size();
	    return 0;
	}
    };

    private void updateUI() {
	if (mBaseAdapter != null)
	    mBaseAdapter.notifyDataSetChanged();
    }

    // The Handler that gets information back from the BluetoothMeterService
    private final Handler mHandler = new Handler() {
	@Override
	public void handleMessage(Message msg) {
	    switch (msg.what) {
	    case MESSAGE_STATE_CHANGE:
		switch (msg.arg1) {
		case BluetoothMeterService.STATE_CONNECTED:
		    mTextViewTitle.setText("Device: " + mConnectedDeviceName);
		    mTextViewStatus.setText(R.string.title_connected_to);
		    break;
		case BluetoothMeterService.STATE_CONNECTING:
		    mTextViewStatus.setText(R.string.title_connecting);
		    break;
		case BluetoothMeterService.STATE_NONE:
		    mTextViewStatus.setText(R.string.title_not_connected);
		    break;
		}
		break;
	    case MESSAGE_WRITE:
		break;
	    case MESSAGE_READ:
		byte[] readBuf = (byte[]) msg.obj;
		// construct a string from the valid bytes in the buffer
		String readMessage = new String(readBuf, 0, msg.arg1);
		// bufferMessege += readMessage;
		if (mMessage != null) {
		    mMessage.add(new CustomizedMessage(readMessage, true));
		    updateUI();
		}
		// bufferMessege = "";
		break;
	    case MESSAGE_DEVICE_NAME:
		// save the connected device's name
		mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
		if (D)
		    Toast.makeText(getApplicationContext(),
			    "Connected to " + mConnectedDeviceName,
			    Toast.LENGTH_SHORT).show();
		break;
	    case MESSAGE_TOAST:
		Toast.makeText(getApplicationContext(),
			msg.getData().getString(TOAST), Toast.LENGTH_SHORT)
			.show();
		break;
	    }
	}
    };

    /**
     * Sends a message.
     * 
     * @param message
     *            A string of text to send.
     */
    private void sendMessages(String message) {
	// Check that we're actually connected before trying anything
	if (mChatService != null
		&& mChatService.getState() != BluetoothMeterService.STATE_CONNECTED) {
	    return;
	}

	// Check that there's actually something to send
	if (mChatService != null && message.length() > 0) {
	    // Get the message bytes and tell the BluetoothMeterService to write
	    // message += "\n";
	    // byte[] send = message.getBytes();
	    String send = message;
	    mChatService.write(send);
	}
    }

    // Automatically try to connec with the known mac address;
    private class ConnectThread extends Thread {

	private final BluetoothDevice device;

	public ConnectThread(BluetoothDevice d) {
	    this.device = d;
	}

	public void run() {
	    while (mConnectThread == Thread.currentThread()) {
		if (mChatService.getState() == BluetoothMeterService.STATE_CONNECTED) {
		    if (D)
			Log.d(TAG, "Device Connected");
		    // if (dialogref.isShowing())
		    // dismissDialog(Dialog_Connect);

		    // Ready to use;
		    break;
		} else if (mChatService.getState() == BluetoothMeterService.STATE_CONNECTING) {
		    try {
			if (D)
			    Log.d(TAG, "Connecting...");
			Thread.sleep(2000);
		    } catch (Exception e) {
			// Log.e(TAG, e.getMessage());
		    }
		} else
		    try {
			if (D)
			    Log.d(TAG, "Started to Connect");
			mChatService.connect(device);
			Thread.sleep(3000);
		    } catch (Exception e) {
			// Log.e(TAG, e.getMessage());
			Thread.currentThread().interrupt();
		    }
	    }
	}
    }

    // create the bluetooth device object, and try to connect with it
    // consistantly and automatically.
    private void connectDevie() {
	if (mCurrentDeviceAddress == null) {
	    Toast.makeText(this, "Bluetooth MAC address is not assigned.",
		    Toast.LENGTH_SHORT).show();
	    finish();
	    return;
	}
	BluetoothDevice device = mBluetoothAdapter
		.getRemoteDevice(mCurrentDeviceAddress);
	// showDialog(Dialog_Connect);
	mConnectThread = new ConnectThread(device);
	mConnectThread.start();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	setContentView(R.layout.activity_console);

	mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

	mListviewMesseges = (ListView) findViewById(R.id.listview_messages);
	mListviewMesseges.setAdapter(mBaseAdapter);
	mListviewMesseges.setClickable(false);

	mEditTextMessege = (EditText) findViewById(R.id.et_message_input);
	mTextViewTitle = (TextView) findViewById(R.id.device_name);
	mTextViewStatus = (TextView) findViewById(R.id.device_status);

	mButtonSend = (Button) findViewById(R.id.btn_send);
	mButtonSend.setOnClickListener(new View.OnClickListener() {
	    @Override
	    public void onClick(View v) {
		String text = mEditTextMessege.getText().toString();
		mEditTextMessege.setText("");
		if (text.length() == 0) {
		    Toast.makeText(getApplicationContext(),
			    "More words required.", Toast.LENGTH_LONG).show();
		    return;
		}
		sendMessages(text);
		mMessage.add(new CustomizedMessage(text, false));
		updateUI();
	    }
	});
    }

    @Override
    public void onDestroy() {
	super.onDestroy();
	if (mConnectThread != null) {
	    if (mConnectThread.isAlive())
		mConnectThread.interrupt();
	    mConnectThread = null;
	}
	if (mChatService != null) {
	    mChatService.stop();
	    mChatService = null;
	}
    }

    @Override
    protected void onStart() {
	super.onStart();
	settings = getSharedPreferences(DeviceList.PREFS_NAME, 0);
	mChatService = new BluetoothMeterService(this, mHandler);
	mMessage = new ArrayList<Console.CustomizedMessage>();
	mCurrentDeviceAddress = settings.getString(
		DeviceList.PREFS_DEVICE_ADDR, null);

	connectDevie();
    }

    @Override
    public synchronized void onResume() {
	super.onResume();
	if (mChatService != null) {
	    if (mChatService.getState() == BluetoothMeterService.STATE_NONE) {
		mChatService.start();
	    }
	}
    }

    @Override
    public synchronized void onPause() {
	super.onPause();
	if (mChatService != null)
	    mChatService.stop();
    }

    @Override
    public synchronized void onStop() {
	if (D)
	    Log.d(TAG, "-- ON STOP --");
	super.onStop();

	if (mConnectThread != null) {
	    if (mConnectThread.isAlive())
		mConnectThread.interrupt();
	    mConnectThread = null;
	}

	mMessage = null;

	if (mChatService != null) {
	    mChatService.stop();
	    mChatService = null;
	}

    }
}
