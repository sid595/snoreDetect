package com.acad_example.nilea.snoredetect;

import android.annotation.SuppressLint;
import android.app.FragmentTransaction;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class getData extends AppCompatActivity {

    // ACTUALLY  all the variable of class can be accessed by subclass
    //At public static final variables can be accessed by other activities using Intent

    public static boolean shouldRecord = false, canPlotAmplitude = true,isProcessing = false,
                    canPlotFrequency = false, canPlot = true;
    public static short[] audioBuffer;
    public static long max=0;
    public static final int BUFFER_SIZE = 512, SAMPLING_RATE = 44100;


    public static int stages = 0,detectTimes=0;
    Button start,selectGraph;
    ProgressBar progress ;
    TextView status,result;
    GraphView graphView;
    public LineGraphSeries<DataPoint> mSeries1;
    ArrayList twiddleFactors = new ArrayList();
    int[] inputInitial ;
    ArrayList inputPosition = new ArrayList();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_data);
        selectGraph = (Button) findViewById(R.id.button2);
        selectGraph.setEnabled(false);
        selectGraph.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(canPlotAmplitude){
                    canPlotAmplitude = false;
                    canPlotFrequency = true;
                    selectGraph.setText("AMPLITUDE");
                    status.setText("Plotting Frequency now");
                    return;
                }
                if(canPlotFrequency){
                    canPlotFrequency = false;
                    canPlotAmplitude = true;
                    selectGraph.setText("FREQUENCY");
                    status.setText("Plotting Amplitude now");
                    return;
                }
            }
        });

        start = (Button) findViewById(R.id.button);
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (shouldRecord==false){
                    selectGraph.setEnabled(true);
                    status.setText("Reading Audio");
                    shouldRecord = true;
                    startRecording();
                    start.setText("STOP");
                }else{
                    selectGraph.setEnabled(false);
                    shouldRecord = false;
                    start.setText("START");
                    status.setText("Not Reading Audio");
                }
            }
        });


        progress = (ProgressBar) findViewById(R.id.progressBar);
        progress.setVisibility(View.VISIBLE);
        progress.setMax(0);
        max = 0;

        status = (TextView) findViewById(R.id.textView);
        result = (TextView) findViewById(R.id.textView2);
        graphView = (GraphView) findViewById(R.id.graph);
        mSeries1 = new LineGraphSeries<>(new DataPoint[] {new DataPoint(0,1)});
        graphView.addSeries(mSeries1);
        //graphView.getViewport().setMaxX(512);
        //graphView.getViewport().setMinX(0);
        //graphView.getViewport().setMaxY(12000);
        //graphView.getViewport().setMinY();



        result.setText("AUDIO LEVEL DISPLAYED ABOVE");
        inputInitial = new int[BUFFER_SIZE];
        for(int i =0;i<BUFFER_SIZE;i++) inputInitial[i] = i;
        prepareFFTgenerator();
        status.setText("Ready to Go");

    }

    public void startRecording() {
        getAudioLevel levelPrint = new getAudioLevel();
        //levelPrint.execute();
        levelPrint.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        //AsyncTaskTools.execute(new getAudioLevel().execute());
    }





    public class getAudioLevel extends AsyncTask<Void,Long,Void>{

        @Override
        protected Void doInBackground(Void... voids) {
            long totalValuesRead,avgVal;
            int i,count,k=0;
            //int bufferSize = AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            int bufferSize = BUFFER_SIZE;
            AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.DEFAULT,
                    SAMPLING_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);

            //if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            //    Log.e("Simple Tag", "Audio Recorder could not initialize");
            //    return;
            //}
            audioBuffer = new short[bufferSize];
            count = audioBuffer.length;
            audioRecord.startRecording();
            totalValuesRead = 0;
            while (shouldRecord) {
                //status.setText("RECORDING NOW");
                int readThisTime = audioRecord.read(audioBuffer, 0, audioBuffer.length);
                //totalValuesRead += readThisTime;
                avgVal = 0;
                for (i = 0; i <count ; i++) {
                    avgVal+= audioBuffer[i];
                }
                avgVal/=count;
                publishProgress(avgVal);
                //status.append(": Current average " + Long.toString(avgVal));
                //mSeries1.resetData(new DataPoint[]{new DataPoint(0,0), new DataPoint(1,avgVal), new DataPoint(2,0)});
                //graphView.removeSeries(mSeries1);
                //mSeries1.appendData(new DataPoint(k,avgVal),true,1,false);
                //graphView.addSeries(mSeries1);
                //k++;
            }
            audioRecord.stop();
            audioRecord.release();
            return null;
        }


        protected void onProgressUpdate(Long... values) {
            //TextView tView = (TextView) findViewById(R.id.textView);
            //status.setText("Current Average "+ Long.toString(Math.abs(values[0])) );
            if(max<values[0]){
                max = values[0];
                progress.setMax(values[0].intValue());
            }
            progress.setProgress(values[0].intValue());
            if(!isProcessing){
                isProcessing = true;
                processData sp = new processData(audioBuffer);
                sp.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            //    sp.execute();
            }
           if(canPlot && canPlotAmplitude) {
                canPlot = false;
                plotData plot = new plotData(audioBuffer);
                plot.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
           }
        }
    }

    public class processData extends AsyncTask<Void,Void,Void>{

        short[] tempBuffer;
        double[] X = new double[BUFFER_SIZE];
        complex[] y = new complex[BUFFER_SIZE];
        ComplexOperator complexOperator = new ComplexOperator();
        DataPoint[] freqData = new DataPoint[BUFFER_SIZE];
        double lfLowerCount,lfHigherCount,hfLowerCount,hfHigherCount,eLow,eHigh,decidingFactor = 0;


        public processData(short[] audioBuffer) {
            tempBuffer = audioBuffer;
        }

        @Override
        protected void onPreExecute() {

            //With reference to: http://ieeexplore.ieee.org/stamp/stamp.jsp?arnumber=7056317&tag=1
            //we define lower frequency region  to be say 250 hz, Higher frequency 1300Hz
            //Bandwidth around the region to be 200 Hz
            double lf,hf,bw,lfLower,hfLower,hfHigher,lfHigher;
            lf = 350; hf = 1300; bw = 200;
            lfLower = lf - bw; lfHigher = lf + bw;
            hfLower = hf - bw; hfHigher = hf + bw;
            lfLowerCount = Math.ceil((lfLower/SAMPLING_RATE)*BUFFER_SIZE);
            lfHigherCount = Math.ceil((lfHigher/SAMPLING_RATE)*BUFFER_SIZE);
            hfLowerCount = Math.ceil((hfLower/SAMPLING_RATE)*BUFFER_SIZE);
            hfHigherCount = Math.ceil((hfHigher/SAMPLING_RATE)*BUFFER_SIZE);

        }

        @Override
        protected Void doInBackground(Void... voids) {
            complex temp1,temp2;

            //This secion is where we do FFT:-------------------------------------------------------
            //Stage 1 initialization: to get proper complex values
            //Note that there will be complex multiplications
            for(int j = 0;j<BUFFER_SIZE;j++){
                y[j] = complexOperator.cMul(tempBuffer[(int)inputPosition.get(j)],
                        (complex)((ArrayList)twiddleFactors.get(0)).get(j));
            }
            //Will start butterfly operations here now
            for(int i=0; i<BUFFER_SIZE;i+=2){
                temp1 = y[i];
                temp2 = y[i+1];
                y[i] = complexOperator.cAdd(temp1,temp2);
                y[i+1] = complexOperator.cSub(temp1,temp2);
            }

            //Skipper is used to tell that the next operations will be performed among the group
            //of those values

            int skipper = 4,tempSkipper;
            for(int i =1; i <stages;i++,skipper*=2){

                //Multiply with twiddle factors
                for(int j = 0; j <BUFFER_SIZE;j++){
                    y[j] = complexOperator.cMul(y[j],(complex)((ArrayList)twiddleFactors.get(i)).get(j));
                }

                //Do the butterfly operations but depending upon the data
                //Twiddle factors already multiplied in previous step
                tempSkipper = skipper/2;
                for(int j =0;j<BUFFER_SIZE;j+=skipper){
                    for(int k = 0; k <tempSkipper; k++){
                        temp1 = y[j+k];
                        temp2 = y[j+k+tempSkipper];
                        y[j+k] = complexOperator.cAdd(temp1,temp2);
                        y[j+k+tempSkipper] = complexOperator.cSub(temp1,temp2);
                    }
                }
            }
            //It is expected that the FFT of the data is now available in y-------------FFT Finishes---------------

            //Each sample from 0 to N/2, say i corresponds to frequency: [i*SAMPLING_RATE/N]
            //We only want magnitude, so we will not concern ourselves with the phase plot
            //And only take half of the values of X


            for(int i =0; i<BUFFER_SIZE;i++) X[i] = (y[i]).abs();

            eLow = 0; eHigh = 0;
            for(int i = (int) lfLowerCount; i <= (int) lfHigherCount;i++ ){
                eLow += (X[i])*(X[i]);
            }
            for(int i = (int) hfLowerCount; i <= (int) hfHigherCount;i++ ){
                eHigh += (X[i])*(X[i]);
            }

            //Deciding factor is being optimized upon many different test cases
            decidingFactor = eLow /(eLow + eHigh);

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if(decidingFactor >= 0.8 ){
                detectTimes++;
                if (detectTimes>5) result.setText("SNORING DETECTED\nFACTOR IS " + decidingFactor);
                else result.setText("SNORING NOT DETECTED\nFACTOR IS " + decidingFactor);

            }
            else {
                detectTimes = 0;
                result.setText("SNORING NOT DETECTED\nFACTOR IS " + decidingFactor);
            }
            if(canPlotFrequency){

                //These Datapoints are not in shifted format
                //for(int i =0; i<BUFFER_SIZE;i++) {
                //    freqData[i] = new DataPoint(i,X[i]);
                //}

                //Now , we shift the data
                //Generally Callled FFTshift

                for(int i =0; i<BUFFER_SIZE/2;i++) {
                    freqData[i + (BUFFER_SIZE/2)] = new DataPoint(i + (BUFFER_SIZE/2),X[i]);
                }

                for(int i =(BUFFER_SIZE/2); i<BUFFER_SIZE;i++) {
                    freqData[i-(BUFFER_SIZE/2)] = new DataPoint(i-(BUFFER_SIZE/2),X[i]);
                }
                mSeries1.resetData(freqData);
            }
            isProcessing = false;

        }
    }

    public class plotData extends AsyncTask<Void,Void,Void>{

        short[] tempBuffer;

        public plotData(short[] audioBuffer) {
            tempBuffer = audioBuffer;
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected Void doInBackground(Void... voids) {
            DataPoint[] values = new DataPoint[tempBuffer.length];
            for(int i =0; i< tempBuffer.length;i++){
                DataPoint V = new DataPoint(i,tempBuffer[i]);
                values[i] = V;
            }
            mSeries1.resetData(values);
            //mSeries1.resetData(values);
            try{
                Thread.sleep(50);
            }catch (InterruptedException e){
                e.printStackTrace();
            }
            canPlot = true;
            return null;
        }

        @Override
        protected void onProgressUpdate(Void... values) {

        }
    }





 //--------------------Doing all this to analyze the data using FFT---------------

    public static class complex {
        double x;
        double y;

        public complex(){
            x =0; y = 0;
        }

        public complex(double a, double b ){
            x = a; y = b;
        }

        public double re(){
            return x;
        }

        public double im(){
            return y;
        }


        public double abs(){
            double val = (x)*(x) + (y)*(y);
            val = Math.sqrt(val);
            return val;
        }

        public complex getConjugate(){
            complex res = new complex();
            res.x = x;
            res.y = (-1)*y;
            return res;
        }

        public void doConjugate(){
            y *= (-1);
        }

        public double phase(){
            double val = Math.atan((y)/(x));
            return val;
        }


        public complex reciprocal(){
            complex res = new complex(0,0);
            double val = (x)*(x) + (y)*(y);
            val = Math.sqrt(val);
            res.x = (x / val );
            res.y = -(y / val );
            return res;
        }

    }

    public static class ComplexOperator{
        complex x = new complex();
        complex y = new complex();

        public complex cMul(complex a, complex b){
            complex res = new complex(0,0);
            res.x = ((a.x)*(b.x)) - ((a.y)*(b.y));
            res.y = ((a.x)*(b.y)) + ((a.y)*(b.x));
            return res;
        }

        public complex cAdd(complex a, complex b){
            complex res = new complex(0,0);
            res.x = a.x + b.x ;
            res.y = a.y + b.y;
            return res;
        }


        public complex cSub(complex a, complex b){
            complex res = new complex(0,0);
            res.x = a.x - b.x ;
            res.y = a.y - b.y;
            return res;
        }

        public complex cMul(int b, complex a){
            complex res = new complex(0,0);
            res.x = (a.x) * (double)b;
            res.y = (a.y) * (double)b;
            return res;
        }


        public complex cDiv(complex a, complex b){
            complex res = cMul(a,b.reciprocal());
            return res;
        }

        public complex cDiv(int a, complex b){
            complex res = cMul(a,b.reciprocal());
            return res;
        }


        public complex cDiv(complex a, int b){
            complex res = new complex(0,0);
            res.x = a.x / (double)b;
            res.y = a.y / (double)b;
            return res;
        }

    }

    public complex cis(double mod,double arg){
        complex res = new complex();
        res.x = mod*(Math.cos(arg));
        res.y = mod*(Math.sin(arg));
        return res;
    }

    public complex twiddle(int n,int k,int order){
        complex res = new complex();
        double x = (double)(n*k); double N = (double) order;
        res.x = Math.cos(2*22*x/(N*7));
        res.y = (-1) * Math.sin(2*22*x/(N*7));
        return res;
    }

    public void prepareFFTgenerator(){
        stages = 0;
        int size = BUFFER_SIZE;
        int i,j,k=0,skipper=1,inputSize = BUFFER_SIZE;

        //Calculate stages of the FFT: Assumes Buffer size is power of 2
        for(;size!=1;stages++) size/=2;
        size = BUFFER_SIZE;
        int alter;
        //Generating the twiddle multiplier for each stage of FFT
        for(i=0;i<stages;i++,skipper*=2){
            List<complex> factors = new ArrayList();
            twiddleFactors.add(factors);
            k = 0; alter = skipper*2;
            for(j=0;j<inputSize;j++){
                 if(j%skipper == 0 && j!=0 && k==0 && alter<=skipper){
                    k = skipper;
                 }
                 if(k!=0){
                     complex t = twiddle((size/(2*skipper)),(skipper-k),BUFFER_SIZE);
                     ((ArrayList)twiddleFactors.get(i)).add(t);
                     k--;
                    //     if(alter==1) alter = skipper*2;
                 }
                 else{
                     ((ArrayList)twiddleFactors.get(i)).add(new complex(1,0));
                 }
                if(alter==1) alter = skipper*2;
                else alter--;

            }
        }
        int temp = BUFFER_SIZE;
        //Finding bit reversal
        inputPosition.clear();;
        recurse(inputInitial);

    }

    public void recurse(int[] value){
        int n = value.length;
        if( n == 1){
            inputPosition.add(value[0]);
            return;
        }
        int[] valueEven = new int[n/2];
        int[] valueOdd = new int[n/2];

        for(int k = 0; k<(n/2);k++) {
            valueEven[k] = value[2*k];
        }
        recurse(valueEven);
        for(int k = 0; k<(n/2);k++) {
            valueOdd[k] = value[2*k + 1];
        }

        recurse(valueOdd);
    }

}