package cn.edu.nju.cyh.Cricket.Core;

/**
 * @author : VingtDylan
 * @date : 2020/3/3
 * @time : 22:28
 * @description : keep it simple and stupid!
 */

public class Correlation {
    private Complex[] s1 = null;
    private Complex[] s2 = null;
    private int lag = 0;
    public Correlation(double []sig1 , double []sig2) {
        assert (sig1.length!=sig2.length);
        int len = sig1.length;
        //System.out.println(len);
        s1 = new Complex[2*len];
        s2 = new Complex[2*len];
        for(int i=0;i<2*len;i++) {
            s1[i] = new Complex();
            s2[i] = new Complex();
        }
        for(int i=0;i<sig1.length;i++) {
            s1[i].real = sig1[i];
        }
        for(int i=0;i<sig2.length;i++) {
            s2[i].real = sig2[i];
        }

        double[] rr = new double[2*len];
        FFT fft = new FFT(2*len);
        fft.FFT(s1);
        fft.FFT(s2);
        conj(s1);
        mul(s2,s1);
        fft.IFFT(s2);
        /*for(int i=0;i<s2.length;i++) {
            System.out.println("s2["+i+"] = "+s2[i].real+"+"+s2[i].imag+"i");
        }*/
        rr = fft.real(s2);
        rr = fft.fftshift(rr);
        /*for(int i=0;i<rr.length;i++) {
            System.out.println("rr["+i+"] = "+rr[i]+"\n");
        }*/
        setLag(rr);
    }

    public void setLag(double []rr){
        int len =  rr.length;
        double max = rr[len-1];
        int location=0;
        for(int i=0;i<len;i++){
            if(rr[i]>max){
                max=rr[i];
                location=i;
            }
        }
        lag = location-len/2;
    }

    public int getLag() {
        return lag;
    }

    public void mul(Complex[] s1,Complex[] s2)
    {
        double temp11=0,temp12=0;
        double temp21=0,temp22=0;
        for(int i=0;i<s1.length;i++)
        {
            temp11 = s1[i].real ; temp12 = s1[i].imag;
            temp21 = s2[i].real ; temp22 = s2[i].imag;
            s1[i].real = temp11 * temp21 - temp12 * temp22;
            s1[i].imag = temp11 * temp22 + temp21 * temp12;
        }
    }

    public void conj(Complex s[])
    {
        for(int i=0;i<s.length;i++)
        {
            s[i].imag = 0.0f - s[i].imag;
        }
    }
}