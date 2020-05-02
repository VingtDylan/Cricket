package cn.edu.nju.cyh.Cricket;

import android.app.Application;

public class ApplicationBuffer extends Application {
    private int buffer_number=521;

    @Override
    public void onCreate(){
        super.onCreate();
    }

    public int getBuffer_number(){
        return buffer_number;
    }

    public void setBuffer_number(int buffer_number){
        this.buffer_number=buffer_number;
    }
}
