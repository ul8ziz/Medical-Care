package pl.projektorion.krzysztof.btattempt3.fragments;


import android.app.Activity;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import pl.projektorion.krzysztof.btattempt3.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class DeviceListFragment extends Fragment {

    private static final String TAG_ERR = "DevListFrag";
    public static final int BT_ENABLE_REQUEST_CODE = 6343;
    public static final String ACTION_BT_DEVICE_SELECTED =
            "pl.projektorion.krzysztof.btattempt3.DeviceListFragment.ACTION_BT_DEVICE_SELECTED";
    public static final String EXTRA_BT_DEVICE_SELECTED =
            "pl.projektorion.krzysztof.btattempt3.DeviceListFragment.EXTRA_BT_DEVICE_SELECTED";

    private BluetoothAdapter btAdapter;
    private ArrayAdapter<String> pairedDeviceAdapter;
    private ArrayAdapter<String> foundDeviceAdapter;
    private Set<String> foundDevicesSet;

    private final BroadcastReceiver btDeviceDiscovered = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if(BluetoothDevice.ACTION_FOUND.equals(action))
            {
                Log.i("DEV", "New device found");
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String label = createDeviceLabel(device.getName(), device.getAddress());

                if (isNewDevice(label))
                    foundDeviceAdapter.add(label);
            }
        }

        private boolean isNewDevice(String label)
        {
            final int beforeSetSize = foundDevicesSet.size();
            foundDevicesSet.add(label);
            final int afterSetSize = foundDevicesSet.size();
            return afterSetSize > beforeSetSize;
        }
    };

    private final BroadcastReceiver btAdapterEvents = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if(BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action))
            {
                messageToast(getString(R.string.toast_bt_discovery_started));
            }else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action))
            {
                messageToast(getString(R.string.toast_bt_discovery_stopped));
            }
        }
    };

    private final Runnable cancelDeviceDiscovery = new Runnable() {
        @Override
        public void run() {
            if ( btAdapter.isDiscovering() )
            {
                btAdapter.cancelDiscovery();
                messageToast(getString(R.string.toast_bt_discovery_stopped));
            }
        }
    };

    private final View.OnClickListener scanButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.i("BTN", "clicked");
            if(btAdapter == null)
                return;
            if( !btAdapter.isEnabled() )
                return;
            if( btAdapter.isDiscovering() )
                return;

            messageToast(getString(R.string.toast_bt_discovery_started));
            foundDeviceAdapter.clear();
            foundDevicesSet.clear();
            btAdapter.startDiscovery();
        }
    };

    private final AdapterView.OnItemClickListener
            onBTDeviceSelectClickListener = new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    final TextView deviceTextView = (TextView) view;
                    String deviceName = deviceTextView.getText().toString();

                    Map<String, String> deviceData = getDataFromDeviceLabel(deviceName);
                    final BluetoothDevice btDevice =
                            btAdapter.getRemoteDevice(deviceData.get("ADDRESS"));
                    Intent sendDeviceIntent = new Intent(ACTION_BT_DEVICE_SELECTED);
                    sendDeviceIntent.putExtra(
                            EXTRA_BT_DEVICE_SELECTED,
                            btDevice
                    );

                    sendDeviceConnectionIntent(sendDeviceIntent);
                    exitCurrentActivity();
                }

                private void sendDeviceConnectionIntent(Intent intent)
                {
                    final Context appContext;
                    try {
                        appContext = getView().getContext();
                    } catch (java.lang.NullPointerException e) {
                        Log.e(TAG_ERR, "Could not send a broadcast intent.");
                        return;
                    }
                    LocalBroadcastManager.getInstance(appContext)
                            .sendBroadcast(intent);
                }

                private void exitCurrentActivity()
                {
                    if(btAdapter.isDiscovering())
                        btAdapter.cancelDiscovery();
                    getActivity().finish();
                }
            };

    public DeviceListFragment() {
        // Required empty public constructor
        this.btAdapter = BluetoothAdapter.getDefaultAdapter();
        this.foundDevicesSet = new HashSet<>();
    }

    private void messageToast(String msg)
    {
        final Context context;
        try {
            context = getView().getContext();
        } catch (NullPointerException e) {
            Log.e(TAG_ERR, "Could not get Context for Toast");
            return;
        }
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }

    private String createDeviceLabel(String name, String address)
    {
        return name + '\n' + address;
    }

    private Map<String, String> getDataFromDeviceLabel(String label)
    {
        final String NAME = "NAME";
        final String ADDRESS = "ADDRESS";
        final int separatorAt = label.indexOf('\n');
        Map<String, String> device = new HashMap<>();
        device.put(NAME, label.substring(0, separatorAt));
        device.put(ADDRESS, label.substring(separatorAt+1));
        return device;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_device_list, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.i("btAdapter is discover", btAdapter.isDiscovering() ? "True" : "It is not");
        initializeVariables();
        initializeDeviceDiscovering();
        checkBluetoothExistence();
        enableBluetooth();
        printBoundedDevices();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (btAdapter != null)
            if (btAdapter.isDiscovering()) cancelDeviceDiscovery.run();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        final Activity activity = getActivity();
        if (btAdapter != null)
            if (btAdapter.isDiscovering()) btAdapter.cancelDiscovery();

        if (activity != null)
        {
            activity.unregisterReceiver(btDeviceDiscovered);
            activity.unregisterReceiver(btAdapterEvents);
        }
    }

    private void initializeVariables()
    {
        Log.i("INIT", "start");
        View view = getView();
        Context context;
        try {
            context = view.getContext();
        } catch (NullPointerException e) {
            Log.e(TAG_ERR, "Could not get Context in initializeVariables");
            return;
        }

        pairedDeviceAdapter = new ArrayAdapter<>(
                context, android.R.layout.simple_list_item_1);
        foundDeviceAdapter = new ArrayAdapter<>(
                context, android.R.layout.simple_list_item_1);

        ListView pairedDevicesListView = (ListView) view.findViewById(R.id.listview_paired_devices);
        ListView foundDevicesListView = (ListView) view.findViewById(R.id.listview_found_devices);
        Button scanButton = (Button) view.findViewById(R.id.button_scan_bt);

        pairedDevicesListView.setAdapter(pairedDeviceAdapter);
        foundDevicesListView.setAdapter(foundDeviceAdapter);
        pairedDevicesListView.setOnItemClickListener(onBTDeviceSelectClickListener);
        foundDevicesListView.setOnItemClickListener(onBTDeviceSelectClickListener);
        scanButton.setOnClickListener(scanButtonClickListener);
        Log.i("INIT", "end");
    }

    private void initializeDeviceDiscovering()
    {
        final Activity activity = getActivity();
        IntentFilter filterDevice = new IntentFilter();
        filterDevice.addAction(BluetoothDevice.ACTION_FOUND);
        activity.registerReceiver(btDeviceDiscovered, filterDevice);

        IntentFilter filterAdapter = new IntentFilter();
        filterAdapter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filterAdapter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        activity.registerReceiver(btAdapterEvents, filterAdapter);
    }

    /**
     * Checks whether a device supports bluetooth.
     * Terminates an activity if id does not.
     */
    private void checkBluetoothExistence()
    {
        if( btAdapter == null ) {
            messageToast(getString(R.string.toast_bt_not_available));
            getActivity().finish();
        }
    }

    /**
     * If BT device is present and not active
     * try to turn it on.
     */
    private void enableBluetooth()
    {
        if( !btAdapter.isEnabled() )
        {
            Intent enableBT = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBT, BT_ENABLE_REQUEST_CODE);
        }
    }

    private void printBoundedDevices()
    {
        if ( !btAdapter.isEnabled() )
            return;

        Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
        if(pairedDevices.size() > 0)
        {
            for( BluetoothDevice device : pairedDevices )
            {
                String label = createDeviceLabel(device.getName(), device.getAddress());
                pairedDeviceAdapter.add(label);
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode)
        {
            case BT_ENABLE_REQUEST_CODE:
                onActionRequestBtEnable(resultCode);
                break;
            default:
                break;
        }
    }

    private void onActionRequestBtEnable(int resultCode)
    {
        switch (resultCode)
        {
            case Activity.RESULT_OK:
                printBoundedDevices();
                break;
            case Activity.RESULT_CANCELED:
                messageToast(getString(R.string.toast_bt_action_bt_not_active));
                getActivity().finish();
                break;
            default:
                break;
        }
    }
}
