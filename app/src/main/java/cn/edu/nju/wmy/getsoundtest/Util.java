package cn.edu.nju.wmy.getsoundtest;

/**
 * @author : VingtDylan
 * @date : 2020/3/3
 * @time : 20:17
 * @description : keep it simple and stupid!
 */


public class Util {
    public static int[] SubArray(int []data,int st,int ed){
        int []ret = new int [ed-st];
        int j=0;
        for(int i=st;i<ed;i++){
            ret[j] = data[i];
            // System.out.println(data[i]/32768.0);
            j++;
        }
        return ret;
    }

    public static double[] SubArray(double []data,int st,int ed){
        double []ret = new double [ed-st];
        int j=0;
        for(int i=st;i<ed;i++){
            ret[j] = data[i];
            j++;
        }
        return ret;
    }
}
