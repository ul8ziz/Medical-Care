package medicalcare.com.medicalcare;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;

import static android.content.ContentValues.TAG;

public class GraphActivity extends AppCompatActivity {

    public BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice bDevice;
    private ConnectThread connectThread;
    private TextView text;

    private LineChart mChart;
    private Thread thread;
    private boolean plotData = true;
    private float o = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.graph_layout);

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

            text = (TextView)findViewById(R.id.reads);
            mChart = (LineChart) findViewById(R.id.heart_chart);

            // enable description text
            mChart.getDescription().setEnabled(true);

            // enable touch gestures
            mChart.setTouchEnabled(true);

            // enable scaling and dragging
            mChart.setDragEnabled(true);
            mChart.setScaleEnabled(true);
            mChart.setDrawGridBackground(false);

            // if disabled, scaling can be done on x- and y-axis separately
            mChart.setPinchZoom(true);

            // set an alternative background color
            mChart.setBackgroundColor(Color.WHITE);
            mChart.setDrawBorders(false);
            mChart.getDescription().setEnabled(false);
            LineData data = new LineData();
            data.setValueTextColor(Color.WHITE);

            mChart.setData(data);
            XAxis xl = mChart.getXAxis();
            xl.setTextColor(Color.WHITE);
            xl.setDrawGridLines(true);
            xl.setAvoidFirstLastClipping(true);
            xl.setEnabled(true);

            YAxis leftAxis = mChart.getAxisLeft();
            leftAxis.setTextColor(Color.WHITE);
            leftAxis.setDrawGridLines(false);
            leftAxis.setAxisMaximum(10f);
            leftAxis.setAxisMinimum(0f);
            leftAxis.setDrawGridLines(true);

            YAxis rightAxis = mChart.getAxisRight();
            rightAxis.setEnabled(false);

            mChart.getAxisLeft().setDrawGridLines(false);
            mChart.getXAxis().setDrawGridLines(false);
            mChart.setDrawBorders(false);

            feedMultiple();
        }

    }
    private void addEntry(float value) {

        LineData data = mChart.getData();

        if (data != null) {

            ILineDataSet set = data.getDataSetByIndex(0);
            // set.addEntry(...); // can be called as well

            if (set == null) {
                set = createSet();
                data.addDataSet(set);
            }

            data.addEntry(new Entry(set.getEntryCount(), value + 5), 0);
            data.notifyDataChanged();

            // let the chart know it's data has changed
            mChart.notifyDataSetChanged();

            // limit the number of visible entries
            mChart.setVisibleXRangeMaximum(150);
            // mChart.setVisibleYRange(30, AxisDependency.LEFT);

            // move to the latest entry
            mChart.moveViewToX(data.getEntryCount());

        }
    }

    private LineDataSet createSet() {

        LineDataSet set = new LineDataSet(null, "Dynamic Data");
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setLineWidth(3f);
        set.setColor(Color.MAGENTA);
        set.setHighlightEnabled(false);
        set.setDrawValues(false);
        set.setDrawCircles(false);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setCubicIntensity(0.2f);
        return set;
    }

    private void feedMultiple() {

        if (thread != null){
            thread.interrupt();
        }

        thread = new Thread(new Runnable() {

            @Override
            public void run() {
                while (true){
                    plotData = true;
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        });

        thread.start();
    }

    public class ConnectThread extends Thread {
        private Context context;

        private BluetoothSocket mmSocket;
        private BluetoothDevice mmDevice;
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
                                    float value = map(Float.parseFloat(0 + inf.replaceAll("[^0-9.]", "")), 350, 600, 0, 5);
                                    addEntry(value);
                                    text.setText(value + "");
                                    break;
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

    public float map(float r, float x1, float x2, float y1, float y2){
        return (r-x1) / (x2 - x1) * (y2 - y1) + y1;
    }

    @Override
    protected void onDestroy() {
        if(connectThread != null) {
            connectThread.Cancel();
        }
        super.onDestroy();
    }
}
