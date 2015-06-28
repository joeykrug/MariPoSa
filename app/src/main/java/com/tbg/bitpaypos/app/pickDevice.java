package com.tbg.bitpaypos.app;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.util.Log;
import android.view.View;

/**
 * Used to pick the serial bluetooth device to communicate w/ existing PoS
 */
public class pickDevice extends Activity {
    private static final String DEBUG_TAG = "Bluetooth";

    BluetoothAdapter btAdapter = null;
    ArrayAdapter<String> deviceAdapter = null;
    String[] deviceNames;
    private HashMap<String, BluetoothDevice> discoveredDevices = new HashMap<String, BluetoothDevice>();

    private static final int ENABLE_BLUETOOTH_DISCOVERY = 2;
    private static final int BT_DISCOVERABLE_DURATION = 120; // max duration, useful for debuggin //120;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list_devices);

        btAdapter = BluetoothAdapter.getDefaultAdapter();

        final ListView lv = (ListView)findViewById(R.id.listDevices);

        discoveredDevices = (HashMap<String, BluetoothDevice>) getIntent().getSerializableExtra("hashmapDevices");
        deviceNames = discoveredDevices.keySet().toArray(new String[discoveredDevices.keySet().size()]);
        ArrayList<String> lst = new ArrayList<String>();
        lst.addAll(Arrays.asList(deviceNames));
        deviceAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, lst);
        lv.setAdapter(deviceAdapter);
        lv.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int pos,long id) {
                String name = (String) ((TextView) view).getText();
                BluetoothDevice device = discoveredDevices.get(name);
                Intent data = new Intent();
                data.putExtra(BluetoothDevice.EXTRA_DEVICE, device);
                setResult(RESULT_OK, data);
                finish();
            }
        });

    }

    public void discoverDevices(View v) {
        if ( !btAdapter.isDiscovering() ) {
            Log.v(DEBUG_TAG, "Enabling discoverable, user will see dialog...");
            Intent discoverMe = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverMe.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, BT_DISCOVERABLE_DURATION);
            startActivityForResult(discoverMe, ENABLE_BLUETOOTH_DISCOVERY);
        }
    }

    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);
    }

    protected void onPause() {
        unregisterReceiver(mReceiver);
        super.onPause();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ENABLE_BLUETOOTH_DISCOVERY){
            Log.v(DEBUG_TAG, "Bluetooth discovery requested");
            if (resultCode == RESULT_CANCELED ) {
                // Bluetooth discovery cancelled
                Log.v(DEBUG_TAG, "Bluetooth discovery cancelled");
                //discoveredDevices.clear();
            } else {
                if ( btAdapter.isEnabled() && !btAdapter.isDiscovering() ) {
                    btAdapter.startDiscovery();
                }
            }
        }
    }
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent
                        .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                discoveredDevices.put(device.getName(), device);

                deviceNames = discoveredDevices.keySet().toArray(new String[discoveredDevices.keySet().size()]);

                deviceAdapter.setNotifyOnChange(true);

                deviceAdapter.clear();


                for(String item: deviceNames) {
                    if(item!=null) {
                        deviceAdapter.add(item);
                    }
                }

            }
        }
    };
}