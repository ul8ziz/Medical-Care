package pl.projektorion.krzysztof.btattempt3.bluetooth;

import android.app.IntentService;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

import pl.projektorion.krzysztof.btattempt3.R;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions and extra parameters.
 */
public class BluetoothService extends IntentService {

    public static final String EXTRA_SEND_MESSENGER =
            "pl.projektorion.krzysztof.btattempt3.bluetooth.extra.SEND_MESSENGER";
    public static final String EXTRA_SEND_BT_DEVICE =
            "pl.projektorion.krzysztof.btattempt3.bluetooth.extra.SEND_BT_DEVICE";
    public static final String EXTRA_SEND_DATA =
            "pl.projektorion.krzysztof.btattempt3.bluetooth.extra.SEND_DATA";
    public static final String EXTRA_RECEIVE_DATA =
            "pl.projektorion.krzysztof.btattempt3.bluetooth.extra.RECEIVE_DATA";
    public static final String ACTION_KILL_THREAD =
            "pl.projektorion.krzysztof.btattempt3.bluetooth.action.KILL_THREAD";
    public static final String ACTION_SEND_TO_BT =
            "pl.projektorion.krzysztof.btattempt3.bluetooth.action.SEND_TO_BT";

    private static final UUID DEFAULT_UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private static final String TAG_ERR = "BTService";


    private BluetoothDevice btDevice;
    private BluetoothSocket btSocket;
    private Messenger messenger;
    private Handler handler;
    private InputStream btInStream;
    private OutputStream btOutStream;
    private Executor writeExecutor;
    private final ReentrantLock runningLock = new ReentrantLock();
    private volatile boolean isRunning = false;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if(ACTION_KILL_THREAD.equals(action)){
                synchronized (runningLock) {
                    isRunning = false;
                    try {
                        btSocket.close();
                    } catch (IOException e) {}
                }
            }else if(ACTION_SEND_TO_BT.equals(action))
            {
                final String data = intent.getStringExtra(EXTRA_SEND_DATA);
                Log.i("BTWRITE", "Try send: " + data);
                writeExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        write(data);
                    }
                });
            }
        }
    };

    private void write(String data)
    {
        if(btSocket == null || btOutStream == null)
            return;

        if(!btSocket.isConnected())
            return;

        final byte[] buffer = data.getBytes(Charset.defaultCharset());

        try {
            btOutStream.write(buffer);
            btOutStream.flush();
        }catch(IOException e) {
            Log.e(TAG_ERR, "Could not write to BT device");
        }
    }

    private void messageToast(String msg)
    {
        final Context context = getApplicationContext();
        final String message = msg;
        handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                } catch (NullPointerException e) {
                    Log.e(TAG_ERR, "Could not get Context for Toast");
                }
            }
        });
    }

    public BluetoothService() {
        super("BluetoothService");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        messenger = intent.getParcelableExtra(EXTRA_SEND_MESSENGER);
        btDevice = intent.getParcelableExtra(EXTRA_SEND_BT_DEVICE);
        handler = new Handler(Looper.getMainLooper());

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if(messenger == null || btDevice == null)
            return;

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_KILL_THREAD);
        intentFilter.addAction(ACTION_SEND_TO_BT);
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(
                broadcastReceiver, intentFilter
        );
        writeExecutor = Executors.newSingleThreadExecutor();

        createSocket();
        if(isConnected())
        {
            isRunning = true;
            setupIOStreams();

            final int BUFFER_SIZE = 4096;
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesReceived;
            messageToast(getString(R.string.toast_bt_connected));

            while (isRunning)
            {
                synchronized (this) {
                    try {
                        bytesReceived = btInStream.read(buffer);
                    } catch (IOException e) { break; }
                }

                Message msg = Message.obtain();
                Bundle bundle = new Bundle();
                String data = new String(buffer, 0, bytesReceived, Charset.defaultCharset());
                bundle.putString(EXTRA_RECEIVE_DATA, data);
                msg.setData(bundle);

                try {
                    messenger.send(msg);
                } catch (RemoteException e) { Log.e(TAG_ERR, "Could not send data to Activity"); }
            }
        }
        else
            messageToast(getString(R.string.toast_bt_not_connected));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            if (btSocket.isConnected()) btSocket.close();
        } catch (IOException e ) {}

        LocalBroadcastManager.getInstance(getApplicationContext())
                .unregisterReceiver(broadcastReceiver);
    }

    private synchronized void createSocket()
    {
        try {
            btSocket = btDevice.createInsecureRfcommSocketToServiceRecord(DEFAULT_UUID);
        } catch (IOException e) {
            messageToast(getString(R.string.toast_bt_could_not_create_socket));
            btSocket = null;
        }
    }

    private synchronized boolean isConnected()
    {
        try {
            btSocket.connect();
        } catch (IOException e) {
            messageToast(getString(R.string.toast_bt_could_not_connect));
            return false;
        }
        return true;
    }

    private synchronized boolean setupIOStreams()
    {
        try {
            btInStream = btSocket.getInputStream();
            btOutStream = btSocket.getOutputStream();
        } catch (IOException e) {
            isRunning = false;
            try {
                if (btSocket.isConnected()) btSocket.close();
            } catch (IOException ee) {}
            return false;
        }
        return true;
    }
}
