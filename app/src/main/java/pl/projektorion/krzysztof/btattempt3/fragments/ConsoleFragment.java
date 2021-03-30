package pl.projektorion.krzysztof.btattempt3.fragments;


import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import pl.projektorion.krzysztof.btattempt3.R;
import pl.projektorion.krzysztof.btattempt3.activities.DeviceListActivity;
import pl.projektorion.krzysztof.btattempt3.bluetooth.BluetoothService;

/**
 * A simple {@link Fragment} subclass.
 */
public class ConsoleFragment extends Fragment {

    private static final String ERR_TAG = "ConsoleFragERR";

    BluetoothDevice btDevice;

    private BroadcastReceiver btDeviceBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            btDevice = intent.getParcelableExtra(DeviceListFragment.EXTRA_BT_DEVICE_SELECTED);
            sendToBTService(BluetoothService.ACTION_KILL_THREAD, null);
            Log.i("DEV RECEIVED", btDevice.toString());

            final Context ctx;
            try {
                ctx = getView().getContext();
            }catch (NullPointerException e) {
                Log.e(ERR_TAG, "Could not start a service.");
                return;
            }
            Intent service = new Intent(ctx,
                    BluetoothService.class);
            Messenger messenger = new Messenger(serviceHandler);
            service.putExtra(BluetoothService.EXTRA_SEND_MESSENGER, messenger);
            service.putExtra(BluetoothService.EXTRA_SEND_BT_DEVICE, btDevice);
            getActivity().startService(service);
        }
    };

    private Handler serviceHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            Bundle bundle = msg.getData();
            String received = bundle.getString(BluetoothService.EXTRA_RECEIVE_DATA);


            final View view = getView();
            EditText receivedConsole;
            try {
                receivedConsole =
                        (EditText) view.findViewById(R.id.edit_text_received_console);
            } catch (NullPointerException e) {
                return;
            }
            if(received != null)
                receivedConsole.append(received);
        }
    };

    private View.OnClickListener sendButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            final View view = getView();
            final EditText sendDataEditText = (EditText) view.findViewById(R.id.edit_text_input);
            String data = sendDataEditText.getText().toString();
            sendDataEditText.setText("");

            Bundle bundle = new Bundle();
            bundle.putString(BluetoothService.EXTRA_SEND_DATA, data);
            sendToBTService(BluetoothService.ACTION_SEND_TO_BT, bundle);
        }
    };

    public ConsoleFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_console, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        final View view = getView();
        final Context context;
        try {
            context = view.getContext();
        } catch (NullPointerException e) {
            return;
        }

        LocalBroadcastManager.getInstance(context).registerReceiver(
                btDeviceBroadcastReceiver,
                new IntentFilter(DeviceListFragment.ACTION_BT_DEVICE_SELECTED)
        );

        Button sendButton = (Button) view.findViewById(R.id.button_send);
        sendButton.setOnClickListener(sendButtonClickListener);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        final Context context;
        try {
            context = getView().getContext();
        }catch (NullPointerException e) {
            Log.e(ERR_TAG, "Could not get Context: onDestroy");
            return;
        }
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(context);
        localBroadcastManager.unregisterReceiver(btDeviceBroadcastReceiver);
        sendToBTService(BluetoothService.ACTION_KILL_THREAD, null);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_layout, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int elemId = item.getItemId();
        final View view = getView();
        switch (elemId)
        {
            case R.id.menu_action_search_bt:
                Intent deviceList = new Intent(view.getContext(), DeviceListActivity.class);
                getActivity().startActivity(deviceList);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void sendToBTService(String action, Bundle data)
    {
        final Context context;
        try {
            context = getView().getContext();
        }catch (NullPointerException e) {
            Log.e(ERR_TAG, "Could not get Context");
            return;
        }
        Intent intent = new Intent(action);
        if(data != null)
            intent.putExtras(data);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }
}
