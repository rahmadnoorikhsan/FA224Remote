package com.litron.litronremote;

import android.app.Activity;
import android.content.Context;
import android.hardware.ConsumerIrManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;
import androidx.annotation.RequiresApi;
import android.widget.Toast;

public class IrController {

    private ConsumerIrManager irService;

    private AudioTrack irAudioTrack = null;
    private static final int SAMPLERATE = 48000;

    private Activity activity;
    private Context mContext;


    IrController(Activity activity, Context context) {
        this.activity = activity;
        mContext = context;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            irService = (ConsumerIrManager) context.getSystemService(Context.CONSUMER_IR_SERVICE);
        }
    }

    public boolean haveEmitter(){
        return irService.hasIrEmitter();
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void sendCode(int[] code) {
        if (irService.hasIrEmitter()){
            for (int i:code){
                int[] codeTransmit = new int[34];
                for (int c=0;c<codeTransmit.length;c=c+2){
                    codeTransmit[c] = 1;
                    codeTransmit[c+1] = 3333;
                }
                int is = 2;
                codeTransmit[0]=3333;
                codeTransmit[1]=1;
                for (int ix=0;ix<=7;ix++){
                    if ((i&1)==1){
                        codeTransmit[is] = 1;
                        is++;
                        codeTransmit[is] = 3333;
                    }else{
                        codeTransmit[is] = 3333;
                        is++;
                        codeTransmit[is] = 1;
                    }
                    is++;
                    i = (byte) (i >> (byte)1);
                }
                irService.transmit(38000, codeTransmit);
            }
        }else{
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(mContext, "Tidak terdapat Infrared dalam device anda", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }


    public void sendUsingAudio(byte[] hexa){

        int bufSize;
        int[] p = new int[]{3333,3333,3333,3333,3333,3333,3333,3333,3333};
//        int cf = 100000;
        int cf = 1000;

        byte[] pAudio = new byte[8000 * 9];
        double cfFactor = 0.5d;
        bufSize = msToAudio((int) Math.round(cfFactor * ((double) cf)), p, pAudio, hexa);
        if (this.irAudioTrack != null) {
            this.irAudioTrack.flush();
            this.irAudioTrack.release();
        }
        this.irAudioTrack = new AudioTrack(3, SAMPLERATE, AudioFormat.CHANNEL_OUT_MONO, 3, bufSize, 0);
        this.irAudioTrack.write(pAudio, 0, bufSize);
        this.irAudioTrack.setStereoVolume(1000.0f, 1000.0f);
        this.irAudioTrack.play();
    }
    private int msToAudio(int cf, int[] signals, byte[] outptr, byte[] datas) {
        int j = 0;
        for (byte dt : datas){
            boolean signalPhase = false;
            double carrierPosRad = 0.0d;
            double carrierStepRad = (((((double) cf) * 3.141592653589793d) * 1.0d) / 44100.0d) / 4.0d;
            while (j < 4800) {
                outptr[j] = Byte.MIN_VALUE;
                j++;
            }
            int i = 0;

            while (i < signals.length) {
                if (i == 0){
                    signalPhase = true;
                }else{
                    if ((dt & 1) == 1){
                        signalPhase = false;
                    }else{
                        signalPhase = true;
                    }
                    dt = (byte) (dt >> (byte)1);
                }
                int j2 = j;
                for (int currentSignal = signals[i];
                     currentSignal > 0;
                     currentSignal = (int) (((double) currentSignal) - 20.833333333333332d)) {
                    int out;
                    if (signalPhase) {
                        out = (int) Math.round((Math.sin(carrierPosRad) * 127.0d) + 128.0d);
                    } else {
                        out = 128;
                    }
//                    j = j2 + 1;
                    outptr[j2] = (byte) out;
//                    j2++;
                    j2 = j + 1;
//                    outptr[j] = (byte) (256 - out);
                    carrierPosRad += carrierStepRad;
                }
                i++;
                j = j2;
            }
            i = 0;
            while (i < signals.length) {
                int j2 = j;
                for (int currentSignal = signals[i];
                     currentSignal > 0;
                     currentSignal = (int) (((double) currentSignal) - 20.833333333333332d)) {
                    int out;
                    out = 128;
//                    j = j2 + 1;
                    outptr[j2] = (byte) out;
//                    j2++;
                    j2 = j + 1;
//                    outptr[j] = (byte) (256 - out);
                    carrierPosRad += carrierStepRad;
                }
                i++;
                j = j2;
            }
        }
        return j;
    }

    private byte JumlahByte = 40;   //0xAA,jeda,jam,jeda,menit,jeda....ceksum = 15byte(8 data + 6 jeda)
    private int SampleRate = 44100;
    private double freqHz = 5000;
    private int durationuS = 3333;
    private byte JumlahBit = 9;
    private byte bit = (byte) 0x7F;
    private static byte diam = ~127;
    public void newMethod(byte[] data){
        try{
            int count = ((int)(((2.0 * 44100.0 * (durationuS / 1000000.0))*JumlahBit)*JumlahByte) & ~1);
            byte[] samples = new byte[count * 2];
            int idx = 0;
            int iSample = 1;
            for(int k = 0; k < (count/(JumlahByte)); k += 1){
                samples[idx] = getSinus(iSample);
                samples[idx + 1] = getSinus(iSample);
                iSample++;
                idx+=2;
            }
            for(int k = 0; k < (count/(JumlahByte)); k += 1){
                samples[idx] = diam;
                samples[idx + 1] = diam;
                iSample++;
                idx+=2;
            }
            for (byte datas : data){
                for(int k = 0; k < count/(9*JumlahByte); k += 1){
                    samples[idx] = getSinus(iSample);
                    samples[idx + 1] = getSinus(iSample);
                    iSample++;
                    idx+=2;
                }
                for (int j=0;j<8;j++){
                    for(int i = 0; i < count/(9*JumlahByte); i += 1){
                        if ((datas & 1)==1){
                            samples[idx] = diam;
                            samples[idx + 1] = diam;
                        }else{
                            samples[idx] = getSinus(iSample);
                            samples[idx + 1] = getSinus(iSample);
                        }
                        iSample++;
                        idx+=2;
                    }
                    datas = (byte) (datas >> (byte)1);
                }
                for(int i = 0; i < count/(JumlahByte); i += 1){
                    samples[idx] = diam;
                    samples[idx + 1] = diam;
                    iSample++;
                    idx+=2;
                }
            }

            AudioTrack track = new AudioTrack(AudioManager.STREAM_MUSIC, SampleRate,
                    AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT,
                    count, AudioTrack.MODE_STATIC);

//            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
//                VolumeShaper.Configuration config =
//                        new VolumeShaper.Configuration.Builder()
//                                .setInterpolatorType(VolumeShaper.Configuration.INTERPOLATOR_TYPE_LINEAR)
//                                .build();
//                track.createVolumeShaper(config);
//            }

            track.write(samples, 0, count);
            track.play();
        }catch (IllegalStateException e){
            Toast.makeText(mContext, "Audio Stack, Restart Aplikasi..", Toast.LENGTH_SHORT).show();
        }
    }

    private byte getSinus(int iSample){
        return (byte) (Math.sin(2 * Math.PI * iSample / (SampleRate / freqHz)) * bit);
    }
}
