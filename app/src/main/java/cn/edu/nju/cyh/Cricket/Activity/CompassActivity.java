package cn.edu.nju.cyh.Cricket.Activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cn.edu.nju.cyh.Cricket.UI.ChaosCompassView;
import cn.edu.nju.cyh.Cricket.Core.Filter;
import cn.edu.nju.cyh.Cricket.R;
import cn.edu.nju.cyh.Cricket.Core.Tdoa;

public class CompassActivity extends AppCompatActivity {
    /**
     * 界面控件
     */
    private ChaosCompassView chaosCompassView;
    private ExecutorService mExecutorService;
    /**
     * 录音相关
     */
    private AudioRecord mAudioRecord = null;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private volatile boolean mIsRecording = true;
    /**
     * 数据相关
     */
    private static final int BUFFER_SIZE = 2048;
    private long startRecorderTime, stopRecorderTime;
    private byte[] mBuffer = new byte[BUFFER_SIZE];
    private byte[] mBuffer1 = new byte[2];
    private byte[] mBuffer2 = new byte[2];
    /**
     * 画波形图与信息处理
     */
    private ArrayList<Double> LYList = new ArrayList<>();
    private ArrayList<Double> RYList = new ArrayList<>();
    private double [] LY = new double[512];
    private double [] RY = new double[512];
    private int index = 0;
    private float val;
    public static float mdegree=0.0f;
    /**
     * 传感器
     */
    private SensorManager mSensorManager;
    private Sensor mGyroscope, mAccelerometer, mMagneticField;
    private SensorEventListener mSensorListener;

    private final float temperature = 20.0f;
    private final float vSpeed = (331.3f + 0.606f * temperature) * 100.0f;

    private boolean isMove = false;
    private final float threshold = 0.1f;
    private float[] gyroValue = new float[3];
    private float[] rotation = new float[9];
    private float[] orientation = new float[3];
    private float[] accelerometerValues = new float[3];
    private float[] magneticFieldValues = new float[3];

    private float degree = 90.0f;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compass);
        chaosCompassView = (ChaosCompassView) findViewById(R.id.ccv);

        mExecutorService = Executors.newSingleThreadExecutor();

        //动态权限
        String[] permissions = {
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO
        };
        for(String str : permissions){
            if (ContextCompat.checkSelfPermission(CompassActivity.this,str) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(CompassActivity.this, permissions,1);
            }
        }

        //传感器注册
        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagneticField = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mSensorListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                switch (event.sensor.getType()){
                    case Sensor.TYPE_ORIENTATION : {
                        break;
                    }
                    case Sensor.TYPE_GYROSCOPE:{
                        gyroValue = event.values;
                        if (!isMove && Math.abs(gyroValue[0]) > threshold){
                            isMove = true;
                        } else if (!isMove && Math.abs(gyroValue[1]) > threshold){
                            isMove = true;
                        } else if (!isMove && Math.abs(gyroValue[2]) > threshold){
                            isMove = true;
                        }else{
                            isMove = false;
                        }
                        if(isMove){
                            LYList.clear();
                            RYList.clear();
                        }
                        break;
                    }
                    case Sensor.TYPE_ACCELEROMETER:{
                        accelerometerValues = event.values;
                        break;
                    }
                    case Sensor.TYPE_MAGNETIC_FIELD:{
                        magneticFieldValues = event.values;
                        break;
                    }
                    default:break;
                }
                Log.d("sadaa", "onSensorChanged: " + gyroValue[0] + " " + LYList.size());
                SensorManager.getRotationMatrix(rotation, null, accelerometerValues, magneticFieldValues);
                SensorManager.getOrientation(rotation, orientation);
                orientation[0] = (float) Math.toDegrees(orientation[0]);
                //chaosCompassView.setVal(orientation[0], gyroValue[0] + 90.0f);
                chaosCompassView.changeVal(orientation[0],degree);
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
                Log.i("Sensor", "onAccuracyChanged");
            }
        };
        //传感器监听
        mSensorManager.registerListener(mSensorListener, mGyroscope, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(mSensorListener, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(mSensorListener, mMagneticField, SensorManager.SENSOR_DELAY_NORMAL);

        //启动录音
        mExecutorService.submit(new Runnable() {
            @Override
            public void run() {
                startRecorder();
            }
        });
    }

    /**
     * 返回主界面
     * @param view
     */
    public void onClick(View view){
        if(view.getId()==R.id.mbutton){
            mIsRecording = false;
            onDestroy();
            startActivity(new Intent(CompassActivity.this,MainActivity.class));
        }
    }

    /**
     * 启动录音recorder
     */
    private void startRecorder() {
        realeseRecorder();
        if (!audioRecordStart()) recorderFail();
    }

    /**
     * 录音主程序
     * @return 返回是否正确录音状态，否则录音失败
     */
    private boolean audioRecordStart() {
        startRecorderTime = System.currentTimeMillis();
        int audioSource = MediaRecorder.AudioSource.MIC;
        int sampleRate = 44100;
        int channelConfig = AudioFormat.CHANNEL_IN_STEREO;
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
        int minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
        mAudioRecord = new AudioRecord(audioSource, sampleRate, channelConfig, audioFormat, Math.max(minBufferSize, BUFFER_SIZE));

        try {
            mAudioRecord.startRecording();
            while (mIsRecording) {
                int read = mAudioRecord.read(mBuffer, 0, BUFFER_SIZE);
                if(read <= 0){
                    return false;
                }else {
                    //硬阈值需要根据实际情况设定
                    //安静室内环境可取消
                    //录音
                    int length = mBuffer.length / 2;
                    index = 0;
                    for(int i = 0; i < length; i++){
                        if(i%2 == 0){
                            System.arraycopy(mBuffer, i * 2, mBuffer1, 0, 2);
                            int res1 = (mBuffer1[0]&0x000000FF) | (((int)mBuffer1[1])<<8);
                            LY[index] = res1 / 32768.0 ;
                            LYList.add(LY[index]);
                            index = i / 2;
                        }
                        else{
                            System.arraycopy(mBuffer, i * 2, mBuffer2, 0, 2);
                            int res2 = (mBuffer2[0]&0x000000FF) | (((int)mBuffer2[1])<<8);
                            RY[index] = res2 / 32768.0 ;
                            RYList.add((RY[index]));
                        }
                    }
                }
                onlineRecorder();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            if (mAudioRecord.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) {
                System.out.println("mic 无权限 ");
            }else{
                System.out.println("mic 有权限 ");
            }
            if (mAudioRecord != null) {
                mAudioRecord.release();
                mAudioRecord = null;
            }
        }
        return true;
    }

    /**
     *释放recorder资源
     */
    private void realeseRecorder() {
        if(mAudioRecord != null) {
            mAudioRecord.release();
            mAudioRecord = null;
        }
    }

    /**
     * 录音异常
     * @return 录音异常
     */
    private boolean recorderFail() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mIsRecording = false;;
            }
        });
        return false;
    }

    /**
     * destory进程
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mExecutorService != null) {
            mExecutorService.shutdownNow();
        }
        if (mAudioRecord != null) {
            mAudioRecord.stop();
            mAudioRecord.release();
            mAudioRecord = null;
        }
        mSensorManager.unregisterListener(mSensorListener);
    }


    /**
     * 录音截止 处理数据
     * @return 是否发生异常
     * @throws IOException
     */
    private boolean audioRecordEnd()throws IOException {
        mAudioRecord.stop();
        mAudioRecord.release();
        mAudioRecord = null;
        stopRecorderTime = System.currentTimeMillis();
        float send = (int) (stopRecorderTime - startRecorderTime) / 1000;
        if (Math.abs(send - 0.5) < 1e-9) {
        } else {
            recorderFail();
            return false;
        }
        return true;
    }

    /**
     * online处理切换
     * @throws Exception
     */
    private void onlineRecorder() throws Exception{
        if(!isMove && LYList.size() >= 44100 * 3){
            //流程简化
            //1s时间窗->5s计算一次
            int sizeLen = LYList.size();
            double []LYD = new double[sizeLen];
            double []RYD = new double[sizeLen];
            for (int i = 0; i < sizeLen; i++) {
                LYD[i] = LYList.get(i);
                RYD[i] = RYList.get(i);
            }
            int Unit = 400;int m = 10;
            Filter offlineFilter = new Filter(LYD,RYD,6,44100.0,990,1010);
            offlineFilter.filter(Unit, m);
            LYD = offlineFilter.getLY();
            RYD = offlineFilter.getRY();
            int size = 2048;
            Tdoa tdoa = new Tdoa(LYD,RYD,size);
            Log.d("TDOA","delay: " + tdoa.getDeltaT() * 44100);
            Log.d("TDOA","deltaT: " + tdoa.getDeltaT() + "秒");
            Log.d("TDOA","Distance: " + tdoa.getDeltaT() * vSpeed +"cm");
            Log.d("TDOA","Sign: " + tdoa.getSign());
            Log.d("TDOA","Angle: " + Math.toDegrees(Math.atan(tdoa.getDeltaT() * vSpeed /16.0)));
            LYList.clear();
            RYList.clear();
            float sign = 0;
            if(tdoa.getSign()>0)sign = 1.0f;
            else if(tdoa.getSign()<0)sign = -1.0f;
            else sign = 0.0f;

            degree = (float)Math.toDegrees(Math.atan(tdoa.getDeltaT() * vSpeed / 16.0f));
            if(degree <= 0)degree = -degree + 90.0f;
            else degree = -degree + 270.0f;
            chaosCompassView.setVal(orientation[0],degree);

            /*
            degree = (float)Math.toDegrees(Math.atan(tdoa.getDeltaT() * vSpeed / 16.0f));
            if(degree >= 0)degree = degree + 90.0f;
            else degree = degree + 270.0f;
            chaosCompassView.setVal(orientation[0],degree);
            */
        }
    }

}

