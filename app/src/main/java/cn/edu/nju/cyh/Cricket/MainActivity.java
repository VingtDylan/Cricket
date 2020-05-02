package cn.edu.nju.cyh.Cricket;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    /**
     * 控件类
     */
    private Button bt_stream_recorder;
    private Button bt_file_recorder;
    private TextView tv_stream_msg;
    private TextView tv_acceleration;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private ExecutorService mExecutorService;
    /**
     * 录音相关
     */
    private AudioRecord mAudioRecord = null;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private volatile boolean mIsRecording = false;
    private volatile boolean mWaveFile = false;
    /**
     * 文件相关
     */
    private File mAudioFile = null, mAudioFile1 = null, mAudioFile2 = null;
    private File mAudioOutFile = null, mAudioOutFile1 = null, mAudioOutFile2 = null;
    private OutputStream outputStream = null, outputStream1 = null, outputStream2 = null;
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
    /**
     * 传感器
     */
    private final float temperature = 20.0f;
    private final float vSpeed = (331.3f + 0.606f * temperature) * 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle("Cricket");
        initView();
        mExecutorService = Executors.newSingleThreadExecutor();
        //动态权限
        String[] permissions = {
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO
        };
        for(String str : permissions){
            if (ContextCompat.checkSelfPermission(MainActivity.this,str) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, permissions,1);
            }
        }
    }
    /**
     * 初始化界面
     */
    public void initView() {
        bt_stream_recorder = (Button) findViewById(R.id.bt_stream_recorder);
        bt_file_recorder = (Button) findViewById(R.id.bt_file_recorder);
        tv_stream_msg = (TextView) findViewById(R.id.tv_stream_msg);
        tv_acceleration =(TextView) findViewById(R.id.tv_acceleration);
        surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(new Draw());
    }

    /**
     * 录音button绑定
     * @param view
     */
    public void record(View view) {
        if (mIsRecording) {
            bt_stream_recorder.setText("开始录音");
            mIsRecording = false;
        } else {
            bt_stream_recorder.setText("停止录音");
            mIsRecording = true;
            mExecutorService.submit(new Runnable() {
                @Override
                public void run() {
                    startRecorder();
                }
            });
        }
    }

    /**
     * 产生*.wav绑定
     * @param view
     */
    public void waveFileGenerate(View view){
        if(mWaveFile){
            bt_file_recorder.setText("不产生wav");
            mWaveFile = false;
        }else{
            bt_file_recorder.setText(" 产生wav");
            mWaveFile = true;
        }
    }

    /**
     * 切换到compass界面
     */
    public void changeUI(View view){
        Intent intent=new Intent();
        intent.setClass(MainActivity.this,CompassActivity.class);
        startActivity(intent);
    }

    /**
     * 绘制波形图
     * @param view
     */
    public void drawWave(View view){
        Canvas c = surfaceHolder.lockCanvas();
        Paint p =new Paint();
        p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        c.drawPaint(p);
        p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
        p.setColor(Color.rgb(128,0,128));
        c.drawLine(0,c.getHeight()/2,c.getWidth(),c.getHeight()/2,p);

        int ww = c.getWidth();
        int hh = c.getHeight();

        p.setColor(Color.RED);
        int len1 = LY.length/2;
        int step1 = ww /len1;
        if(step1 == 0) { step1 = 1;}
        int prex = 0, prey = 0;
        int x = 0, y = 0;
        double k = hh/2.0*2;
        for(int i = 0; i < len1; ++i){
            x = x + step1;
            y = hh-(int)(LY[i]*k+hh/2);
            if(i!=0){
                c.drawLine(x, y, prex , prey,p);
            }
            prex = x;
            prey = y;
            /*
            double z = LYList.get(LYList.size()-1);
            System.out.println("drawWave " + i + ":" + x + " "+y + " " +k + " " + z);
            */
        }

        p.setColor(Color.GREEN);
        int len2 = RY.length/2;
        int step2 = ww / len2;
        if(step2 == 0) { step2 = 1;}
        prex = 0; prey = 0;
        x = 0; y = 0;
        for(int i = 0; i <= len2; ++i){
            x = x + step2;
            y = hh-(int)(RY[i]*k+hh/2);
            if(i!=0){
                c.drawLine(x, y, prex , prey,p);
            }
            prex = x;
            prey = y;
        }
        surfaceHolder.unlockCanvasAndPost(c);
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
            String mAudioName = startRecorderTime + "_" + sampleRate + "";
            if(mWaveFile){
                mAudioFile = Util.createFile(mAudioName + ".pcm");
                mAudioFile1 = Util.createFile(mAudioName + "1.pcm");
                mAudioFile2 = Util.createFile(mAudioName + "2.pcm");
                mAudioOutFile = Util.createFile(mAudioName + ".wav");
                mAudioOutFile1 = Util.createFile(mAudioName + "1.wav");
                mAudioOutFile2 = Util.createFile(mAudioName + "2.wav");
                outputStream = new FileOutputStream(mAudioFile);
                outputStream1 = new FileOutputStream(mAudioFile1);
                outputStream2 = new FileOutputStream(mAudioFile2);
            }
            mAudioRecord.startRecording();
            while (mIsRecording) {
                int read = mAudioRecord.read(mBuffer, 0, BUFFER_SIZE);
                if(read <= 0){
                    return false;
                }else {
                    //录音
                    if(mWaveFile) outputStream.write(mBuffer);
                    int length = mBuffer.length / 2;
                    index = 0;
                    for(int i = 0; i < length; i++){
                        if(i%2 == 0){
                            System.arraycopy(mBuffer, i * 2, mBuffer1, 0, 2);
                            int res1 = (mBuffer1[0]&0x000000FF) | (((int)mBuffer1[1])<<8);
                            LY[index] = res1 / 32768.0 ;
                            LYList.add(LY[index]);
                            index = i / 2;
                            if(mWaveFile) outputStream1.write(mBuffer1);
                        }
                        else{
                            System.arraycopy(mBuffer, i * 2, mBuffer2, 0, 2);
                            int res2 = (mBuffer2[0]&0x000000FF) | (((int)mBuffer2[1])<<8);
                            RY[index] = res2 / 32768.0 ;
                            RYList.add((RY[index]));
                            if(mWaveFile) outputStream2.write(mBuffer2);
                        }
                    }
                }
            }
            onlineRecorder();
            if(mWaveFile) {
                outputStream.close();
                outputStream1.close();
                outputStream2.close();
                Util.pcmToWave(mAudioFile.getAbsolutePath(), mAudioOutFile.getAbsolutePath(), 2, BUFFER_SIZE);
                Util.pcmToWave(mAudioFile1.getAbsolutePath(), mAudioOutFile1.getAbsolutePath(), 1, BUFFER_SIZE);
                Util.pcmToWave(mAudioFile2.getAbsolutePath(), mAudioOutFile2.getAbsolutePath(), 1, BUFFER_SIZE);
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
                bt_stream_recorder.setText("开始录音");
                tv_stream_msg.setText("录取失败，请重新录入");
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
        final float send = (int) (stopRecorderTime - startRecorderTime) / 1000;
        if (send > 0.5) {
            Log.d("pcm", "doStop:here! ");
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    tv_stream_msg.setText("录音成功：" + send + "秒");
                    bt_stream_recorder.setText("开始录音");
                }
            });
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
        if (!audioRecordEnd()) recorderFail();
        else{
            int sizeLen = LYList.size();
            double []LYD = new double[sizeLen];
            double []RYD = new double[sizeLen];
            for (int i = 0; i < sizeLen; i++) {
                LYD[i] = LYList.get(i);
                RYD[i] = RYList.get(i);
            }
            int Unit = 400;int m = 10;
            Filter offlineFilter = new Filter(LYD,RYD,6,44100.0,4000);
            offlineFilter.filter(Unit, m);
            LYD = offlineFilter.getLY();
            RYD = offlineFilter.getRY();
            int size = 2048;
            Tdoa tdoa = new Tdoa(LYD,RYD,size);
            Log.d("TDOA","delay: " + tdoa.getDeltaT() * 44100);
            Log.d("TDOA","deltaT: " + tdoa.getDeltaT() + "秒");
            Log.d("TDOA","Distance: " + tdoa.getDeltaT() * 34000.0 +"cm");
            Log.d("TDOA","Distance: " + tdoa.getSign());

            Intent intent=new Intent();
            intent.setClass(MainActivity.this,CompassActivity.class);
            CompassActivity.mdegree = (float)(Math.signum(tdoa.getSign())*90.0 + Math.toDegrees(Math.atan(tdoa.getDeltaT() * vSpeed /16.0)));
            Log.d("TDOA","Angle: " + Math.abs(tdoa.getSign())*90.0 + " " + Math.toDegrees(Math.atan(tdoa.getDeltaT() * vSpeed /16.0)));
            Log.d("TDOA","Angle: " + CompassActivity.mdegree);
            startActivity(intent);
        }
    }

    /**
     * 麦克风权限
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            mAudioRecord.startRecording();
        }else {
            Toast.makeText(this,"用户拒绝了权限",Toast.LENGTH_SHORT).show();
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    /**
     * 麦克风参数测试 已经注释掉
     */
    private void TestRecorder(){
        int[] mSampleRates = new int[] { 8000, 11025, 22050, 44100 };
        for (int rate : mSampleRates) {
            for (short audioFormat : new short[] { AudioFormat.ENCODING_PCM_8BIT, AudioFormat.ENCODING_PCM_16BIT }) {
                for (short channelConfig : new short[] { AudioFormat.CHANNEL_IN_MONO, AudioFormat.CHANNEL_IN_STEREO }) {
                    try {
                        Log.d("recorder", "Attempting rate " + rate + "Hz, bits: " + audioFormat + ", channel: "
                                + channelConfig);
                        int bufferSize = AudioRecord.getMinBufferSize(rate, channelConfig, audioFormat);
                        if (bufferSize != AudioRecord.ERROR_BAD_VALUE) {
                            AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, rate, channelConfig, audioFormat, bufferSize);
                            if (recorder.getState() == AudioRecord.STATE_INITIALIZED){
                                Log.i("recorder","success");
                            }
                        }
                    } catch (Exception e) {
                        Log.e("a", rate + "Exception, keep trying.",e);
                    }
                }
            }
        }
    }

    /**
     *  波形绘制类
     */
    private class Draw implements SurfaceHolder.Callback{
        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width,
                                   int height) {
            System.out.println("surfaceChanged");
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder){
            new Thread(){
                public void run() {
                    Canvas c = surfaceHolder.lockCanvas(new Rect(0, 0, 200, 200));
                    Paint p = new Paint();
                    p.setColor(Color.rgb(128,0,128));
                    c.drawLine(0,c.getHeight()/2,c.getWidth(),c.getHeight()/2,p);
                    surfaceHolder.unlockCanvasAndPost(c);
                }
            }.start();
        }
        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            System.out.println("surfaceDestroyed==");
        }
    }

}






