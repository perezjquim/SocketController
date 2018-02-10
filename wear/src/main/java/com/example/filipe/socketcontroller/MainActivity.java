package com.example.filipe.socketcontroller;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.wearable.view.drawer.WearableActionDrawer;
import android.support.wearable.view.drawer.WearableNavigationDrawer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import org.eazegraph.lib.charts.PieChart;
import org.eazegraph.lib.models.PieModel;
import org.w3c.dom.Text;

import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends Activity implements MessageApi.MessageListener, SensorEventListener , GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "Main Activity Watch";
    private TextView _x_acc;
    private TextView _y_acc;
    private TextView _z_acc;
    private TextView _tms;
    private CheckBox _leftHanded;
    private SensorManager _sensorManager;
    private Sensor _sensor;

    private Timer timer;
    private TimerTask checkSecond;


    //Done by Pedro to implement the schedule service
    private Button _buttonStart;
    private Button _buttonEnd;
    private Button _buttonSchedule;
    private TextView _StartTime;
    private TextView _EndTime;
    private TextView _consumo;
    private int seconds;


    private int Primeiroconsumo;
    private int consumo;
    private int primeiro;
    private int consumoTotal;
    private int count = 0;
    private TimePicker InitialTime;
    private TimePicker EndTime;
    private LinearLayout chooseStartTime;
    private LinearLayout chooseEndTime;
    private boolean changedStart;
    private boolean changedEnd;
    private String [] ChartColor = new String[4];

    private GoogleApiClient _client;

    private boolean _sensor_running = false;

    private Node _phone; // the connected device to send the message to
    //private int _count=0;

    public static final String WEAR_ACC_SERVICE = "acc";

    private long _last_push;
    private long _sampling_diff = 40;        // alterei o sampling rate aqui
    //private float _orientationVals[]={0,0,0};
    float[] _rotationMatrix = new float[16];
    float x;
    float z;
    private int _factor;
    private int vez = 0;

    PowerManager.WakeLock cpuWakeLock;
    private PieChart mPieChart;

    private PushThread pushThread;


    private WearableNavigationDrawer navDrawer;
    private WearableActionDrawer actDrawer;
    private Menu actMenu;
    private MenuInflater actMenuInflater;

    private MenuItem itemToggleSensor;
    private TextView textSensorState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.general_layout);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        ChartColor[0] = "#FE6DA8";
        ChartColor[1] = "#56B7F1";
        ChartColor[2] = "#CDA67F";
        ChartColor[3] = "#FED70E";

        seconds = 0;
        Primeiroconsumo=0;
        consumo = 0;
        primeiro = 0;
        consumoTotal = 0;

        _sensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
        _sensor = _sensorManager.getDefaultSensor( Sensor.TYPE_ORIENTATION);
        _last_push = System.currentTimeMillis();

        actDrawer = (WearableActionDrawer) findViewById(R.id.bottom_action_drawer);
        actMenu = actDrawer.getMenu();
        actMenuInflater = getMenuInflater();

        // Possibilita a navegação pelos tabs presentes no TabAdapter
        navDrawer = (WearableNavigationDrawer) findViewById(R.id.top_navigation_drawer);
        navDrawer.setAdapter(new TabAdapter(this));

        // Desenha o tab predefinido
        Tab initialTab = new Tab(WattappTabs.DEFAULT);
        draw(initialTab);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) { }
    @Override
    public void onConnectionSuspended(int i) { }

    @Override
    protected void onStop()
    {
        super.onStop();
        Wearable.MessageApi.removeListener(_client, this);
        _client.disconnect();
    }

    @Override
    public void onResume()
    {
        super.onResume();

        _client = new GoogleApiClient.Builder(this)
                    .addApi(Wearable.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();

        _client.connect();
        Wearable.MessageApi.addListener(_client, this);


        Log.i(TAG, "On resume called");

        PowerManager pm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
        cpuWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        cpuWakeLock.acquire();

       // _sensorManager.registerListener(this, _sensor, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    public void onStart()
    {
        super.onStart();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        //Log.wtf(TAG,event.toString());

/*
        SensorManager.getRotationMatrixFromVector(_rotationMatrix,
                event.values);
        SensorManager
                .remapCoordinateSystem(_rotationMatrix,
                        SensorManager.AXIS_X, SensorManager.AXIS_Z,
                        _rotationMatrix);
        SensorManager.getOrientation(_rotationMatrix, _orientationVals);

        // Optionally convert the result from radians to degrees
        _orientationVals[0] = (float) Math.toDegrees(_orientationVals[0]);
        _orientationVals[1] = (float) Math.toDegrees(_orientationVals[1]);
        _orientationVals[2] = (float) Math.toDegrees(_orientationVals[2]);

//        Yaw:  _orientationVals[0]
//        Pitch:  _orientationVals[1]
//        Roll:     _orientationVals[2]

        float x = _orientationVals[0];//event.values[0];
        float y = _orientationVals[1];//event.values[1];
        float z = _orientationVals[2];
        int val =4;*/

       // if(x>val||y>val||z>val) {
       // float y = event.values[1];





        //}
          //  Log.i(TAG,"Sending data");

          //  float[] data = {x,y};
            x = event.values[0];
           // _x_acc.setText(x+"");
            z = event.values[2];
            z = _factor*z;
           // _y_acc.setText(z+"");

//            Log.i("DEBUG",x+","+z);

    //Log.i(TAG,"sending data form watch");
    }

    public void ShowStartPicker(View v)
    {
        if(chooseStartTime.getVisibility() == LinearLayout.GONE)
        { chooseStartTime.setVisibility(LinearLayout.VISIBLE); }
        else
        { chooseStartTime.setVisibility(LinearLayout.GONE); }
    }

    public void ShowEndPicker(View v)
    {
        if(chooseEndTime.getVisibility() == LinearLayout.GONE)
        { chooseEndTime.setVisibility(LinearLayout.VISIBLE); }
        else
        { chooseEndTime.setVisibility(LinearLayout.GONE); }
    }

    public void handleSensorClick(MenuItem item)
    {
        if(!_sensor_running)
        {
            itemToggleSensor.setTitle(R.string.STOP_SENSOR);
            textSensorState.setText(R.string.SENSOR_ON);

            _factor = _leftHanded.isChecked()?-1:1;
            _sensorManager.registerListener(this, _sensor, SensorManager.SENSOR_DELAY_FASTEST);
            _sensor_running = true;
            pushThread = new PushThread();
            pushThread.start();
        }
        else
        {
            //cpuWakeLock.release();
            itemToggleSensor.setTitle(R.string.START_SENSOR);
            textSensorState.setText(R.string.SENSOR_OFF);

            _sensorManager.unregisterListener(this);
            _sensor_running = false;

            try
            { pushThread.join(); }
            catch (InterruptedException e)
            { e.printStackTrace(); }
        }
    }

    public void handleQuitClick(MenuItem item)
    {
        cpuWakeLock.release();
        _sensorManager.unregisterListener(this);
        _sensor_running = false;
        this.finish();
    }


    //
    // WEAR COMMUNICATION STUFF
    //
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Wearable.NodeApi.getConnectedNodes(_client)
                .setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>()
                {
                    @Override
                    public void onResult(NodeApi.GetConnectedNodesResult nodes)
                    {
                        for (Node node : nodes.getNodes())
                        { _phone = node; }
                        Log.i(TAG,"watch connected");
                    }
                });
            toast("Connected successfully!");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult)
    {
        toast("Connection failed! ("+connectionResult.toString()+")");
    }

    private void sendMessage(String key)
    {
        if (_phone != null && _client!= null && _client.isConnected())
        {
            //   Log.d(TAG, "-- " + _client.isConnected());
            Wearable.MessageApi.sendMessage(
                    _client, _phone.getId(), WEAR_ACC_SERVICE + "" + key, null).setResultCallback(

                    new ResultCallback<MessageApi.SendMessageResult>()
                    {
                        @Override
                        public void onResult(MessageApi.SendMessageResult sendMessageResult)
                        {

                            if (!sendMessageResult.getStatus().isSuccess())
                            {
                                Log.e(TAG, "Failed to send message with status code: "
                                        + sendMessageResult.getStatus().getStatusCode());
                            }
                            else
                            {
                              //  Log.i(TAG,"status "+sendMessageResult.getStatus().isSuccess());
                            }
                        }
                    }
            );
        }
        else
        {
            Log.d("SENDMESSAGE","Failed to send a message!");
        }
    }

    public void Schedule(View v)
    {
        if(v.getId() ==  R.id.buttonSchedule)
        {
            String StartTime = _StartTime.getText().toString()+"/";
            StartTime += _EndTime.getText().toString();
            //Log.i("HOUR",StartTime);
            if(vez == 0)
            {
                _buttonSchedule.setText("Confirm Start");
                vez++;
            }
            else if(vez == 1)
            {
                _buttonSchedule.setText("Confirm End");
                vez++;
            }
            else
            {
                _buttonSchedule.setText("Set Schedule");
                vez = 0;
            }
            sendMessage(StartTime);
        }
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent)
    {
        try
        {
            toast("Message received!");
            String [] valores = messageEvent.getPath().split("-");
            if(valores.length > 1)
            {
                mPieChart.clearChart();
                int tamanho = (valores.length - 1 )/ 2;
                for(int i = 0; i < tamanho; i++)
                {
                    mPieChart.addPieSlice(new PieModel(valores[i*2+1], Float.parseFloat(valores[i*2+2]), Color.parseColor(ChartColor[mPieChart.getChildCount()])));
                }
                mPieChart.startAnimation();
            }
            else
            {
                String power = messageEvent.getPath();
                _consumo.setText(power);
            }
        }
        catch(Exception e)
        {
            Log.i("Error",messageEvent.getPath());
            e.printStackTrace();
        }
    }

    private class PushThread extends Thread
    {
        public void run()
        {
            while(_sensor_running)
            {
                sendMessage(x+"#"+z);
                //Log.i("DEBUG",x+"#"+z);

                try
                { Thread.sleep(_sampling_diff); }
                catch (InterruptedException e)
                { e.printStackTrace(); }
            }
        }
    }


    private void toast(String s)
    { Toast.makeText(this, s, Toast.LENGTH_LONG).show(); }

    /* Separador da aplicação (com respetivo layout) */
    private class Tab extends Fragment
    {
        private WattappTabs choice;

        public Tab(final WattappTabs choice)
        {
            this.choice = choice;
            final Bundle arguments = new Bundle();
            arguments.putSerializable("TAB",choice);
            this.setArguments(arguments);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState)
        {
            if(choice.menu != -1)
            {
                actMenu.clear();
                actMenuInflater.inflate(choice.menu,actMenu);
            }

            return inflater.inflate(choice.layout, container, false);
        }

        @Override
        public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
        {
        	/* São atualizados os IDs dos elementos de cada view */
            _x_acc          = (TextView) view.findViewById(R.id.x_text_field);
            _y_acc          = (TextView) view.findViewById(R.id.y_text_field);
            _z_acc          = (TextView) view.findViewById(R.id.z_text_field);
            _tms            = (TextView) view.findViewById(R.id.tms_text_field);
            _leftHanded     = (CheckBox) view.findViewById(R.id.checkLeftHanded);
            _buttonSchedule = (Button) view.findViewById(R.id.buttonSchedule);
            _buttonStart    = (Button) view.findViewById(R.id.buttonStart);
            _buttonEnd      = (Button) view.findViewById(R.id.buttonEnd);
            _StartTime      = (TextView) view.findViewById(R.id.HoraInicio);
            _EndTime        = (TextView) view.findViewById(R.id.HoraFim);
            _consumo        = (TextView) view.findViewById(R.id.ConsumoInsert);
            InitialTime     = (TimePicker) view.findViewById(R.id.InitialPicker);
            EndTime         = (TimePicker) view.findViewById(R.id.EndPicker);
            chooseStartTime = (LinearLayout) view.findViewById(R.id.PrimeiroTempo);
            chooseEndTime   = (LinearLayout) view.findViewById(R.id.UltimoTempo);
            mPieChart = (PieChart) view.findViewById(R.id.piechart);

            itemToggleSensor = (MenuItem) actMenu.findItem(R.id.item_toggle_sensor);
            textSensorState = (TextView) findViewById(R.id.textSensorState);

            /* Respetivas configurações dos elementos de cada view */

            if(InitialTime != null)
            {
                InitialTime.setIs24HourView(true);
                InitialTime.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener()
                {
                    public void onTimeChanged(TimePicker view, int hourOfDay, int minute)
                    {
                        _StartTime.setText(hourOfDay + ":" + minute);
                        changedStart = true;
                    }
                });
            }

            if(EndTime != null)
            {
                EndTime.setIs24HourView(true);
                EndTime.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener()
                {
                    public void onTimeChanged(TimePicker view, int hourOfDay, int minute)
                    {
                        _EndTime.setText(hourOfDay + ":" + minute);
                        changedEnd = true;
                    }
                });
            }

            if(chooseStartTime != null)
            { chooseStartTime.setVisibility(LinearLayout.GONE); }

            if(chooseEndTime != null)
            { chooseEndTime.setVisibility(LinearLayout.GONE); }
        }
    }

    /* Fornece acesso aos tabs do enum */
    private final class TabAdapter extends WearableNavigationDrawer.WearableNavigationDrawerAdapter
    {
        private final Context context;
        private WattappTabs currentTab = WattappTabs.DEFAULT;

        public TabAdapter(final Context context)
        { this.context = context; }

        @Override
        public String getItemText(int index)
        { return context.getString(WattappTabs.values()[index].title); }

        @Override
        public Drawable getItemDrawable(int index)
        { return context.getDrawable(WattappTabs.values()[index].icon); }

        @Override
        public void onItemSelected(int index)
        {
        	// Busca o tab ao enum (correspondente ao índice)
            WattappTabs chosenTab = WattappTabs.values()[index];

            // Se for um tab diferente do atual
            if (chosenTab != currentTab)
            {
                Tab newTab = new Tab(chosenTab);
                draw(newTab);

                currentTab = chosenTab;

                if(chosenTab.menu != -1)
                { actDrawer.unlockDrawer(); }
                else
                { actDrawer.lockDrawerClosed(); }
            }
        }

        @Override
        public int getCount()
        { return WattappTabs.values().length; }
    }

    /* Desenha o novo tab */
    private void draw(Tab newTab)
    {
    	// Substitui o elemento 'fragment_container' pela view do tab
        getFragmentManager()
            .beginTransaction()
            .replace(R.id.fragment_container, newTab)
            .commit();
    }
}
