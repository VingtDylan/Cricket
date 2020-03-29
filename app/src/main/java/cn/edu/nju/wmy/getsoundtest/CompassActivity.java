package cn.edu.nju.wmy.getsoundtest;

import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.util.Random;

public class CompassActivity extends AppCompatActivity {
    private SensorManager mSensorManager;
    private SensorEventListener mSensorEventListener;
    private ChaosCompassView chaosCompassView;
    private Button mbutton;
    private float val;
    public static float mdegree=0.0f;
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compass);

        chaosCompassView = (ChaosCompassView) findViewById(R.id.ccv);
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mbutton = (Button) findViewById(R.id.mbutton);
        mSensorEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                ApplicationBuffer applicationBuffer=(ApplicationBuffer)getApplicationContext();
                Log.d("Application buffer","number: "+applicationBuffer.getBuffer_number());
                val = event.values[0];
                /*
                float changeDegree=new Random().nextFloat();
                changeDegree=2*changeDegree-1;
                float tmpdegree=mdegree+changeDegree;
                chaosCompassView.setVal(val,tmpdegree);
                */
                chaosCompassView.setVal(val,mdegree);
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };

        mSensorManager.registerListener(mSensorEventListener,mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                SensorManager.SENSOR_DELAY_GAME);
    }

    public void onClick(View view){
        if(view.getId()==R.id.mbutton)
            startActivity(new Intent(CompassActivity.this,MainActivity.class));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSensorManager.unregisterListener(mSensorEventListener);
    }
}
