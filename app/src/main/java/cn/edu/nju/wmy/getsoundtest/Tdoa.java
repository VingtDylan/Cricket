package cn.edu.nju.wmy.getsoundtest;

import java.util.*;

/**
 * @author : VingtDylan
 * @date : 2020/3/3
 * @time : 21:44
 * @description : keep it simple and stupid!
 */


public class Tdoa {
    private double deltaT = 0;
    private double sign = 0;
    private int size = 2000;

    public double []dataL = null;
    public double []dataR = null;

    public Tdoa(double []dataL,double []dataR,int size){
        this.dataL = dataL;
        this.dataR = dataR;
        this.size = size;
        runner();
    }

    public void runner(){
        int len = dataL.length;

        int ed = size;
        double []SampleL = new double[size];
        double []SampleR = new double[size];
        SampleL = Util.SubArray(dataL,ed-size,ed);
        SampleR = Util.SubArray(dataR,ed-size,ed);

        Map<Integer,Integer> freqMap = new HashMap<Integer, Integer>(len/size);

        while(len>=ed){
            SampleL = Util.SubArray(dataL,ed-size,ed);
            SampleR = Util.SubArray(dataR,ed-size,ed);
            Correlation correlation = new Correlation(SampleL,SampleR);
            System.out.println(correlation.getLag()+"\n");
            int tmplag = correlation.getLag();
            Integer v = freqMap.get(tmplag);
            freqMap.put(tmplag,v==null?1:v+1);
//            if(Math.abs(tmplag)>3){
//                Integer v = freqMap.get(tmplag);
//                freqMap.put(tmplag,v==null?1:v+1);
//            }
            ed += size;
        }

        List<Map.Entry<Integer, Integer>> entries = new ArrayList<>(freqMap.entrySet());
        Collections.sort(entries, new Comparator<Map.Entry<Integer, Integer>>() {
            @Override
            public int compare(Map.Entry<Integer, Integer> e1, Map.Entry<Integer, Integer> e2) {
                return e2.getValue() - e1.getValue();
            }
        });

        List<Integer> modalNums = new ArrayList<>();
        modalNums.add(entries.get(0).getKey());

        int size = entries.size();
        for (int i = 1; i < size; i++) {
            if (entries.get(i).getValue().equals(entries.get(0).getValue())) {
                modalNums.add(entries.get(i).getKey());
            } else {
                break;
            }
        }

        if(modalNums.size()==1){
            int lag = modalNums.get(0);
            System.out.println(lag);
            deltaT = lag / 44100.0 * 1.0;
            sign = Math.signum(deltaT);
        }else{
            boolean flag = true;
            double s = modalNums.get(0);
            int msize = modalNums.size();
            double presign = Math.signum(modalNums.get(0));
            for(int i=1;i<msize;i++){
                s += modalNums.get(i);
                double cursign = Math.signum(modalNums.get(i));
                if((presign>0&&cursign<0)||(presign<0&&cursign>0)){
                    flag = false;
                    break;
                }else{
                    presign = cursign;
                }
            }
            if(flag){
                System.out.println(s*1.0/msize);
                deltaT = s*1.0/msize/ 44100.0 * 1.0;
                sign = Math.signum(deltaT);
            }else{
                deltaT = 0;
                sign = 2;
            }
        }
    }
    public double getDeltaT(){
        return deltaT;
    }

    public double getSign(){
        return sign;
    }
}
