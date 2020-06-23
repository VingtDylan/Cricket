package cn.edu.nju.cyh.Cricket.UI;

/**
 * Created by ASUS on 2020/3/18.
 */

import android.view.SurfaceView;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;

public class MySurfaceView extends SurfaceView implements Runnable, Callback {

    private SurfaceHolder mHolder;
    private Thread t;
    private boolean flag;
    private Canvas mCanvas;
    private Paint p;
    private int x = 50, y = 700, r = 100;
    public MySurfaceView(Context context) {
        super(context);
        mHolder = getHolder();
        mHolder.addCallback(this);
        p = new Paint();
        p.setColor(Color.WHITE);
        setFocusable(true);
    }

    /**
     * 自定义一个方法，在画布上画一个圆
     */
    public void Draw() {
        mCanvas = mHolder.lockCanvas();
        mCanvas.drawRGB(0, 0, 0);
        mCanvas.drawCircle(x, y, r, p);
        mHolder.unlockCanvasAndPost(mCanvas);
    }

    /**
     * 当SurfaceView创建的时候，调用此函数
     */
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        t = new Thread(this);
        flag = true;
        t.start();
    }

    /**
     * 当SurfaceView的视图发生改变的时候，调用此函数
     */
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
    }

    /**
     * 当SurfaceView销毁的时候，调用此函数
     */
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        flag = false;
    }

    /**
     * 当屏幕被触摸时调用
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        x = (int) event.getX();
        y = (int) event.getY();
        return true;
    }

    /**
     * 当用户按键时调用
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_DPAD_UP){    //当用户点击↑键时
            y--;    //设置Y轴坐标减1
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void run() {
        while (flag) {
            Draw();
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}