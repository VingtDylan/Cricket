package cn.edu.nju.wmy.getsoundtest;

/**
 * @author : VingtDylan
 * @date : 2020/3/3
 * @time : 22:19
 * @description : keep it simple and stupid!
 */



public class FFT {
    private int N_FFT = 0;
    private int M_of_N_FFT = 0;
    private int Npart2_of_N_FFT = 0;
    private int Npart4_of_N_FFT = 0;
    private double []SIN_TABLE_of_N_FFT = null;

    private static final double PI =  3.14159265358979323846264338327950288419716939937510;

    public FFT(int FFT_N) {
        Init_FFT(FFT_N);
    }

    public Complex[] complexLization(double []data) {
        Complex[] w = new Complex[data.length];
        for(int i=0;i<data.length;i++){
            w[i].real = data[i];
            w[i].imag = 0.0f;
        }
        return w;
    }

    public double[] magnitude(Complex[] data) {
        double[] r = new double[data.length];
        for(int i=0;i<data.length;i++) {
            r[i] = (double) Math.sqrt(data[i].real * data[i].real + data[i].imag * data[i].imag);
        }
        return r;
    }

    private void Init_FFT(int N_of_FFT) {
        int i=0;
        int temp_N_FFT=1;
        N_FFT = N_of_FFT;
        M_of_N_FFT = 0;
        for (i=0; temp_N_FFT<N_FFT; i++){
            temp_N_FFT = 2*temp_N_FFT;
            M_of_N_FFT++;
        }

        Npart2_of_N_FFT = N_FFT/2;
        Npart4_of_N_FFT = N_FFT/4;

        CREATE_SIN_TABLE();
    }

    private void CREATE_SIN_TABLE() {
        SIN_TABLE_of_N_FFT = new double[Npart4_of_N_FFT + 1];
        int i=0;
        for (i=0; i<=Npart4_of_N_FFT; i++){
            SIN_TABLE_of_N_FFT[i] = (double) Math.sin(PI*i/Npart2_of_N_FFT);
        }
    }

    private double Sin_find(double x) {
        int i = (int)(N_FFT*x);
        i = i>>1;
        if (i>Npart4_of_N_FFT) {
            i = Npart2_of_N_FFT - i;
        }
        return SIN_TABLE_of_N_FFT[i];
    }

    private double Cos_find(double x) {
        int i = (int)(N_FFT*x);
        i = i>>1;
        if (i<Npart4_of_N_FFT) {
            return SIN_TABLE_of_N_FFT[Npart4_of_N_FFT - i];
        }
        else {
            return -SIN_TABLE_of_N_FFT[i - Npart4_of_N_FFT];
        }
    }

    private void ChangeSeat(Complex DataInput[]) {
        int nextValue,nextM,i,k,j=0;
        Complex temp;
        nextValue=N_FFT/2;
        nextM=N_FFT-1;
        for (i=0;i<nextM;i++) {
            if (i<j) {
                temp=DataInput[j];
                DataInput[j]=DataInput[i];
                DataInput[i]=temp;
            }
            k=nextValue;
            while (k<=j) {
                j=j-k;
                k=k/2;
            }
            j=j+k;
        }
    }

    public void FFT(Complex []data) {
        int L=0,B=0,J=0,K=0;
        int step=0, KB=0;
        double angle;
        Complex W = new Complex();
        Complex Temp_XX = new Complex();

        ChangeSeat(data);
        for (L=1; L<=M_of_N_FFT; L++) {
            step = 1<<L;
            B = step>>1;
            for (J=0; J<B; J++) {
                angle = (double) J/B;
                W.imag =  -Sin_find(angle);
                W.real =   Cos_find(angle);
                for (K=J; K<N_FFT; K=K+step) {
                    KB = K + B;
                    Temp_XX.real = data[KB].real * W.real-data[KB].imag*W.imag;
                    Temp_XX.imag = W.imag*data[KB].real + data[KB].imag*W.real;
                    data[KB].real = data[K].real - Temp_XX.real;
                    data[KB].imag = data[K].imag - Temp_XX.imag;
                    data[K].real = data[K].real + Temp_XX.real;
                    data[K].imag = data[K].imag + Temp_XX.imag;
                }
            }
        }
    }

    public void IFFT(Complex []data){
        int L=0,B=0,J=0,K=0;
        int step=0, KB=0;
        double angle=0.0f;
        Complex W = new Complex();
        Complex Temp_XX = new Complex();

        ChangeSeat(data);
        for (L=1; L<=M_of_N_FFT; L++){
            step = 1<<L;
            B = step>>1;
            for (J=0; J<B; J++) {
                angle = (double) J/B;
                W.imag =   Sin_find(angle);
                W.real =   Cos_find(angle);
                for (K=J; K<N_FFT; K=K+step){
                    KB = K + B;
                    Temp_XX.real = data[KB].real * W.real-data[KB].imag*W.imag;
                    Temp_XX.imag = W.imag*data[KB].real + data[KB].imag*W.real;

                    data[KB].real = data[K].real - Temp_XX.real;
                    data[KB].imag = data[K].imag - Temp_XX.imag;

                    data[K].real = data[K].real + Temp_XX.real;
                    data[K].imag = data[K].imag + Temp_XX.imag;
                }
            }
        }
        for(int i=0;i<N_FFT;i++){
            data[i].real = data[i].real/N_FFT;
            //data[i].imag = 0.0f;
            // 由于最后并不需要复数部分，所以赋0也可以
            data[i].imag = data[i].imag/N_FFT;
        }
    }

    public double[] real(Complex []data){
        double []realPart = new double[N_FFT];
        for(int i = 0; i<N_FFT; i++){
            realPart[i] = data[i].real;
        }
        return realPart;
    }

    public double[] fftshift(double[] real){
        int length = real.length;
        double []fftshifted = new double[length];
        int m = length/2;
        for(int i=0;i<length;i++){
            fftshifted[i] = real[(i+m)%length];
        }
        return fftshifted;
    }
}

