package medicalcare.com.medicalcare;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;

import static android.content.ContentValues.TAG;

public class TerminalActivity extends AppCompatActivity {

    public BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice bDevice;
    private ConnectThread connectThread;

    private TextView heart;
    private TextView heat;
    private TextView oxgen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.terminal_layout);
        heart = (TextView)findViewById(R.id.heart_text);
        heat = (TextView)findViewById(R.id.heat_text);
        oxgen = (TextView)findViewById(R.id.oxgen_text);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter == null){
            Toast.makeText(this,"Your phone does not support Bluetooth.",Toast.LENGTH_LONG).show();
        }
        else {
            if (bluetoothAdapter.isEnabled()) {
                Intent enbleBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enbleBluetooth, 1); //Enables the bluetooth if its disabled
            }

            bDevice = null;

            Set<BluetoothDevice> devices = bluetoothAdapter.getBondedDevices(); //get all paired devices
            if (devices.size() > 0) {
                for (BluetoothDevice b : devices) {
                    if (b.getName().equals("HC-06")) {
                        bDevice = b;
                        connectThread = new ConnectThread(bDevice,getBaseContext());
                        connectThread.start();
                        break;
                    }
                }
                if(bDevice == null)
                    Toast.makeText(getBaseContext(), " HC-06 is not founded", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getBaseContext(), "No paired devices", Toast.LENGTH_LONG).show();
            }
        }
    }
    public class ConnectThread extends Thread {
        private Context context;

        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private ConnectedThread connectedThread;

        private final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

        public ConnectThread(BluetoothDevice bDevice,Context context){
            this.context = context;
            mmDevice =  bDevice;
            BluetoothSocket tmp = null;

            try{
                tmp = mmDevice.createRfcommSocketToServiceRecord(MY_UUID);
            }catch (IOException e){
                Toast.makeText(context,"Could not connect!",Toast.LENGTH_SHORT).show();
            }
            mmSocket = tmp;
            connectedThread = new ConnectedThread(mmSocket, context);
        }

        @Override
        public void run() {
            super.run();
            bluetoothAdapter.cancelDiscovery();
            try{
                mmSocket.connect();
            }catch(IOException e){
                try{
                    mmSocket.close();
                }catch(IOException es){
                    Toast.makeText(context, es.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
            try {

                connectedThread.start();
            }catch (Exception e){
                Log.d("",e.getMessage());
            }
        }

        public void Cancel(){
            try{
                mmSocket.close();
                connectedThread.cancel();
            }catch (IOException e){
                Toast.makeText(context,"could not close",Toast.LENGTH_SHORT).show();
            }
        }
    }
    public class ConnectedThread extends Thread {

        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        private byte[] mmBuffer;
        private Handler handler;

        private final Context context;
        public ConnectedThread(BluetoothSocket bSocket, Context context){
            mmSocket = bSocket;
            this.context = context;
            handler = new Handler(Looper.getMainLooper());
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try{
                tmpIn = mmSocket.getInputStream();
                tmpOut = mmSocket.getOutputStream();

            }catch (IOException e){
                Toast.makeText(context,e.getMessage(),Toast.LENGTH_SHORT).show();
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        @Override
        public void run() {
            mmBuffer = new byte[1024];
            while (true) {
                try {
                    mmInStream.read(mmBuffer);
                    handler.post(new Runnable() {
                        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
                        @Override
                        public void run() {
                            String str = new String(mmBuffer, StandardCharsets.US_ASCII);
                            String[] infos = str.split("[\n,]");
                            for(String inf : infos){
                                if(inf.toLowerCase().contains("r=")) {
                                    heart.setText(Float.parseFloat(0 + inf.replaceAll("[^0-9.]", "")) + "");
                                }
                                else  if(inf.toLowerCase().contains("c=")) {
                                    heat.setText(Float.parseFloat(0 + inf.replaceAll("[^0-9.]", "")) + "");
                                }
                                else  if(inf.toLowerCase().contains("o=")) {
                                    oxgen.setText(Float.parseFloat(0 + inf.replaceAll("[^0-9.]", "")) + "");
                                }
                            }
                        }
                    });
                } catch (IOException e) {
                    Log.d(TAG, "Input stream was disconnected", e);
                    break;
                }
            }
        }
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) { }
        }
        public void cancel() {
            try {
                mmSocket.close();
            }
            catch (IOException e) { }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(connectThread != null) {
            connectThread.Cancel();
        }
    }
}











