package com.example.kostas.heartratemonitor;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import me.aflak.bluetooth.Bluetooth;

import static com.example.kostas.heartratemonitor.R.id.bpm;
import static java.lang.Integer.parseInt;

public class CardioGraph extends AppCompatActivity implements Bluetooth.CommunicationCallback {
    private String name;
    private Bluetooth b;
    private Button send,results,save,history;
    private ScrollView scrollView;
    private boolean registered=false;
    GraphView graph;
    private LineGraphSeries<DataPoint> mSeries;

    DBHelper dbHelper;
    TextView bpmText;
    int counter=0;
    String values;
    int flagWait=0;
    //FlagValue 0 --> Ready to receive , Cant Show
    //          1 --> Cant Do anything
    //          2 --> Have received , ready to show/save
    int flagValue=1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cardio_graph);

        bpmText=(TextView)findViewById(bpm);
        history = (Button)findViewById(R.id.history);
        results = (Button) findViewById(R.id.results);
        save = (Button) findViewById(R.id.save);
        send = (Button)findViewById(R.id.send);
        scrollView = (ScrollView) findViewById(R.id.scrollView);
        graph = (GraphView) findViewById(R.id.graph);

        graph.getViewport().setScrollableY(true);
        graph.getViewport().setScalable(true);
        graph.getGridLabelRenderer().setVerticalLabelsVisible(false);

        send.setEnabled(false);

        b = new Bluetooth(this);
        b.enableBluetooth();

        b.setCommunicationCallback(this);

        int pos = getIntent().getExtras().getInt("pos");
        name = b.getPairedDevices().get(pos).getName();

        dbHelper = new DBHelper(this);

        Toast.makeText(this, "Connecting...", Toast.LENGTH_SHORT).show();
        b.connectToDevice(b.getPairedDevices().get(pos));

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                flagWait=0;
                values="";
                counter=0;
                String msg = "m";
                b.send(msg);
                flagValue=0;
                Toast.makeText(CardioGraph.this, "Taking Test...", Toast.LENGTH_LONG).show();
            }
        });
        results.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (flagValue==2) {
                    mSeries.resetData(generateData());
                    int bpm=Bpm();
                    calcDanger(bpm);
                }else{
                    Toast.makeText(CardioGraph.this, "You Haven't Taken a Heart Rate Test", Toast.LENGTH_SHORT).show();
                }
            }
        });

        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("flag","flagValue"+flagValue);
                if (flagValue==2){
                    DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                    Date date = new Date();
                    dbHelper.insertPerson(values,dateFormat.format(date));
                    Toast.makeText(CardioGraph.this, "Graph Successfully Saved", Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(CardioGraph.this, "Nothing to Save", Toast.LENGTH_SHORT).show();
                }
            }
        });

        history.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(CardioGraph.this, History.class);
                startActivity(i);
            }
        });

        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mReceiver, filter);
        registered=true;

        mSeries = new LineGraphSeries<>();
        graph.addSeries(mSeries);
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(1200);
        graph.getViewport().setXAxisBoundsManual(true);
    }

    private void calcDanger(int bpm){
        double min=60;
        double max=100;
        double dBpm=(double)bpm;
        if (dBpm<min){
            Toast.makeText(this, "Your heart rate is below normal", Toast.LENGTH_SHORT).show();
        }else if(dBpm>max){
            Toast.makeText(this, "Your heart rate is above normal", Toast.LENGTH_SHORT).show();
        }else{
            Toast.makeText(this, "Your heart rate is normal", Toast.LENGTH_SHORT).show();
        }
    }

    private int Bpm() {
        int beats=0;
        String[] parts=values.split(" ");
        int count = parts.length;
        //Log.d("Length", String.valueOf(count));
        for (int i=0; i<count; i++) {
            int max=0;
            while(Integer.parseInt(parts[i])>650){
                if(Integer.parseInt(parts[i])>max){
                    max=Integer.parseInt(parts[i]);
                }
                i++;
            }
            if (max!=0){
                beats++;
            }
        }
        //Log.d("Beats", String.valueOf(beats));
        float time = (float) (count*0.006);
        int bpm= (int) (beats*60/time);
        bpmText.setText(Integer.toString(bpm));
        return bpm;
    }

    private DataPoint[] generateData() {
        String[] parts=values.split(" ");
        //Log.d("Debug", "parts=" + values);
        int count = parts.length;
        DataPoint[] dataValues = new DataPoint[count];
        for (int i=0; i<count; i++) {
            double x = i;
            double y= parseInt(parts[i]);
            DataPoint v = new DataPoint(x, y);
            dataValues[i] = v;
        }
        return dataValues;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(registered) {
            unregisterReceiver(mReceiver);
            registered=false;
        }
    }

    @Override
    public void onConnect(BluetoothDevice device) {
        //Display("Connected to "+device.getName()+" - "+device.getAddress());
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                send.setEnabled(true);
            }
        });
    }

    @Override
    public void onDisconnect(BluetoothDevice device, String message) {
        Toast.makeText(this, "Disconnected!", Toast.LENGTH_SHORT).show();
        b.connectToDevice(device);
    }

    @Override
    public void onMessage(String message) {
        //Log.d("Debug","message="+message);
        flagWait++;
        if (flagWait>5 && flagValue==0) {
            if (counter < 1200) {
                if (counter == 0) {
                    values = message;
                    values += " ";
                } else {
                    values += message;
                    values += " ";
                }
                //Log.d("Debug", "parts=" + values);
                counter += 20;
                //Log.d("Flag","flagValue="+flagValue);
            }else{
                Log.d("Debug", "parts=" + values);
                flagValue=2;
            }
        }
    }

    @Override
    public void onError(String message) {
        Toast.makeText(this, "Error: "+message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnectError(final BluetoothDevice device, String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        b.connectToDevice(device);
                    }
                }, 2000);
            }
        });
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                Intent intent1 = new Intent(CardioGraph.this, SelectBluetooth.class);

                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        if(registered) {
                            unregisterReceiver(mReceiver);
                            registered=false;
                        }
                        startActivity(intent1);
                        finish();
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        if(registered) {
                            unregisterReceiver(mReceiver);
                            registered=false;
                        }
                        startActivity(intent1);
                        finish();
                        break;
                }
            }
        }
    };
}