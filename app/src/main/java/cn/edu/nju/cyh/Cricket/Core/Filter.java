package cn.edu.nju.cyh.Cricket.Core;

import biz.source_code.dsp.filter.FilterPassType;
import biz.source_code.dsp.filter.IirFilterCoefficients;
import biz.source_code.dsp.filter.IirFilterDesignExstrom;
import cn.edu.nju.cyh.Cricket.Tools.Util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author : VingtDylan
 * @date : 2020/3/3
 * @time : 18:37
 * @description : keep it simple and stupid!
 */

public class Filter {
    private double []LY = null;
    private double []RY = null;
    private int level = 6;
    private double FS = 44100;
    private double Fs1 = 900;
    private double Fs2 = 1100;
    private IirFilterCoefficients iirFilterCoefficients;

    public  Filter(double []LY, double []RY, int level, double FS, double Fs1,double Fs2){
        this.LY = LY;
        this.RY = RY;
        this.level = level;
        this.FS = FS;
        this.Fs1 = Fs1;
        this.Fs2 = Fs2;
    }

    public void filter(int Unit,int m){
        iirFilterCoefficients = IirFilterDesignExstrom.design(FilterPassType.highpass,this.level,this.Fs1/this.FS,this.Fs1/this.FS);
        this.LY = IIRFilter(this.LY, iirFilterCoefficients.a, iirFilterCoefficients.b);
        this.RY = IIRFilter(this.RY, iirFilterCoefficients.a, iirFilterCoefficients.b);
        iirFilterCoefficients = IirFilterDesignExstrom.design(FilterPassType.lowpass,this.level,this.Fs2/this.FS,this.Fs2/this.FS);
        this.LY = IIRFilter(this.LY, iirFilterCoefficients.a, iirFilterCoefficients.b);
        this.RY = IIRFilter(this.RY, iirFilterCoefficients.a, iirFilterCoefficients.b);
        for (int i=0;i<iirFilterCoefficients.a.length;i++) {
            System.out.println("A["+i+"]:"+iirFilterCoefficients.a[i]);
        }
        for (int i=0;i<iirFilterCoefficients.b.length;i++) {
            System.out.println("B["+i+"]:"+iirFilterCoefficients.b[i]);
        }

        int []res1 = VSD(this.LY,Unit,m);
        int []res2 = VSD(this.RY,Unit,m);
        /*Bug*/
        /*
        for (int i=0;i<2;i++) {
            System.out.println("res1["+i+"]:"+res1[i]);
            System.out.println("res2["+i+"]:"+res2[i]);
        }
        */
        int []index = Mixed(res1,res2);
        this.LY = Util.SubArray(this.LY,index[0],index[1]);
        this.RY = Util.SubArray(this.RY,index[0],index[1]);
    }

    public synchronized double[] IIRFilter(double[] signal, double[] a, double[] b) {
        double[] in = new double[b.length];
        double[] out = new double[a.length-1];
        double[] outData = new double[signal.length];
        for (int i = 0; i < signal.length; i++) {
            System.arraycopy(in, 0, in, 1, in.length - 1);
            in[0] = signal[i];
            float y = 0;
            for(int j = 0 ; j < b.length ; j++){
                y += b[j] * in[j];
            }
            for(int j = 0;j < a.length-1; j++){
                y -= a[j+1] * out[j];
            }
            System.arraycopy(out, 0, out, 1, out.length - 1);
            out[0] = y;
            outData[i] = y;
        }
        return outData;
    }

    public synchronized int[] VSD(double []SD,int Unit,int m){
        int []res = new int[2];
        int SDLen = SD.length;
        int slideCount = SDLen / Unit;
        List<Double> VarianceList = new ArrayList<Double>();
        double []sample = new double[Unit];
        for(int i = 1; i<=slideCount; i++){
            for(int j = 0; j<Unit;j++){
                sample[j] = SD[(i-1)*Unit+j];
            }
            VarianceList.add(Variance(sample));
            //System.out.print("slideCount "+i+" : "+Variance(sample)+"\n");
        }

        double threshold = 0.0;
        /*忘记排序的bug....*/
        Collections.sort(VarianceList);
        if(slideCount%2==0){
            threshold=(VarianceList.get(slideCount/2-1)+VarianceList.get(slideCount/2))/2.0;
        }else{
            threshold=VarianceList.get((slideCount-1)/2);
        }
        //System.out.print(slideCount+" numbers 'median is: "+threshold+"\n");

        int st = Unit - 1;
        sample = Util.SubArray(SD,0,Unit);
        double samplevar = Variance(sample);
        // int t = 0;
        while(samplevar < threshold){
            st += m;
            sample = Util.SubArray(SD,st - Unit + 1,st +1);
            samplevar = Variance(sample);
            // t += 1;
        }
        // System.out.print(samplevar+"\n");
        // System.out.print(t+"\n");
        res[0] = st;
        int ed = SDLen-Unit;
        sample = Util.SubArray(SD,ed,ed+Unit);
        samplevar = Variance(sample);
        while(samplevar < threshold){
            ed -= m;
            sample = Util.SubArray(SD,ed,ed+Unit);
            samplevar = Variance(sample);
        }
        res[1] = ed;
        return res;
    }

    public synchronized double Variance(double []data){
        double average = 0.0;
        for(double d : data){
            average += d;
        }
        average /= data.length;
        double variance = 0.0;
        for(double d : data){
            variance += Math.pow((d-average),2);
        }
        return variance/(data.length);
    }

    public int[] Mixed(int []res1,int []res2){
        int []ret = new int[2];
        ret[0] = res1[0] > res2[0] ? res1[0] : res2[0];
        ret[1] = res1[1] < res2[1] ? res1[1] : res2[1];
        return ret;
    }

    public double[] getLY(){
        return this.LY;
    }

    public double[] getRY(){
        return this.RY;
    }
}
