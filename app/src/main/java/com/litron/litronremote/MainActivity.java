package com.litron.litronremote;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.hardware.usb.UsbDevice;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.Settings;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.suke.widget.SwitchButton;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import dev.mhandharbeni.usbserial.deviceids.CH34xIds;
import dev.mhandharbeni.usbserial.usbserial.UsbSerialInterface;
import io.github.handharbeni.wheelview_module.WheelPicker;

public class MainActivity extends AppCompatActivity {
    private boolean isConnected = false;
    /*
     * Notifications from UsbService will be received here.
     */
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case UsbService.ACTION_USB_PERMISSION_GRANTED: // USB PERMISSION GRANTED
                    isConnected = true;
                    initPutarLayar();
                    statusIr.setText("Mode : IR OTG");
                    break;
                case UsbService.ACTION_USB_PERMISSION_NOT_GRANTED: // USB PERMISSION NOT GRANTED
                    isConnected = false;
                    doPutarLayar(0);
                    statusIr.setText("Tidak bisa mendapatkan ijin untuk OTG");
                    break;
                case UsbService.ACTION_NO_USB: // NO USB CONNECTED
                    isConnected = false;
                    doPutarLayar(0);
                    if (ir.haveEmitter()){
                        statusIr.setText("Mode : IR Internal");
                    }else{
                        statusIr.setText("Tidak Ada OTG yang terpasang. jika sudah terpasang, Coba lepas lalu pasang kembali");
                    }
                    break;
                case UsbService.ACTION_USB_DISCONNECTED: // USB DISCONNECTED
                    isConnected = false;
                    doPutarLayar(0);
                    statusIr.setText("OTG Terlepas");
                    if (ir.haveEmitter()){
                        statusIr.setText("Mode : IR Internal");
                    }else{
                        statusIr.setText("Tidak terdapat IR Internal ataupun External");
                    }
                    break;
                case UsbService.ACTION_USB_NOT_SUPPORTED: // USB NOT SUPPORTED
                    isConnected = false;
                    doPutarLayar(0);
                    statusIr.setText("OTG Tidak Mendukung");
                    break;
            }
        }
    };
    private UsbService usbService;
    private MyHandler mHandler;

    private static final String TAG = MainActivity.class.getSimpleName();
    private IrController ir;

    private static final String LABEL_SEPARATOR = ":";

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    private AlertDialog alertDialog;
    private AlertDialog.Builder builder;
    private static final String LabelPutarLayar = "PUTAR_LAYAR";


    @SuppressLint("NonConstantResourceId")
    @BindView(R.id.btnKirim)
    AppCompatButton btnKirim;

    @SuppressLint("NonConstantResourceId")
    @BindView(R.id.JamOn)
    WheelPicker JamOn;

    @SuppressLint("NonConstantResourceId")
    @BindView(R.id.JamOff)
    WheelPicker JamOff;

    @SuppressLint("NonConstantResourceId")
    @BindView(R.id.MenitOn)
    WheelPicker MenitOn;

    @SuppressLint("NonConstantResourceId")
    @BindView(R.id.MenitOff)
    WheelPicker MenitOff;

    @SuppressLint("NonConstantResourceId")
    @BindView(R.id.choose_ir)
    SwitchButton choose_ir;

    @SuppressLint("NonConstantResourceId")
    @BindView(R.id.statusIr)
    TextView statusIr;

    private int max, current;



    static int[] a = new int[9];

    private static final String S_WAKTU_ON = "WaktuON";
    private static final String S_WAKTU_OFF = "WaktuOFF";
    private static final String S_SWITCH_BUTTON = "SwitchBUTTON";

    private AudioManager audioManager;

    private final ServiceConnection usbConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
            try {
                usbService = ((UsbService.UsbBinder) arg1).getService();
                usbService.setHandler(mHandler);
            } catch (Exception ignored) {}
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            usbService = null;
        }
    };
    public String toHex(String arg) {
        return String.format("%040x", new BigInteger(1, arg.getBytes(Charset.defaultCharset())));
    }
    public String getsWaktuOn() {
        return sharedPreferences.getString(S_WAKTU_ON, "17:59");
    }
    public void setsWaktuOn(String sWaktuOn) {
        editor = sharedPreferences.edit();
        editor.putString(S_WAKTU_ON, sWaktuOn);
        editor.apply();
    }
    public String getsWaktuOff() {
        return sharedPreferences.getString(S_WAKTU_OFF, "04:59");
    }
    public void setsWaktuOff(String sWaktuOff) {
        editor = sharedPreferences.edit();
        editor.putString(S_WAKTU_OFF, sWaktuOff);
        editor.apply();
    }
    public boolean getSwitchButton(){
        return sharedPreferences.getBoolean(S_SWITCH_BUTTON, false);
    }
    public void setsSwitchButton(boolean switchButton){
        editor = sharedPreferences.edit();
        editor.putBoolean(S_SWITCH_BUTTON, switchButton);
        editor.apply();
    }
    private void setPutarLayar(int putarLayar){
        editor = sharedPreferences.edit();
        editor.putInt(LabelPutarLayar, putarLayar);
        editor.apply();
    }

    private int getPutarLayar(){
        return sharedPreferences.getInt(LabelPutarLayar, 0);
    }
    private List<String> getListJam(){
        return Arrays.asList(getResources().getStringArray(R.array.jam));
    }
    private List<String> getListMenit(){
        return Arrays.asList(getResources().getStringArray(R.array.menit));
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPreferences = getSharedPreferences("LITRONRemote", Context.MODE_PRIVATE);
        audioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);


        ir = new IrController(this, this);

        setContentView(R.layout.second_layout);
        mHandler = new MyHandler(this);

        ButterKnife.bind(this);

        if (ir.haveEmitter()){
            statusIr.setText("Mode : IR Internal");
        }else{
            if (!isConnected){
                statusIr.setText("Tidak terdapat IR Internal ataupun External");
            }else{
                statusIr.setText("Mode : IR External");
            }
        }
        initDataShared();
//        initPermission();
    }
    private void initDataShared(){
        JamOn.setData(getListJam());
        JamOn.setSelectedItemPosition(getListJam().indexOf(getsWaktuOn().split(":")[0].length()==1?"0"+getsWaktuOn().split(":")[0]:getsWaktuOn().split(":")[0]));

        MenitOn.setData(getListMenit());
        MenitOn.setSelectedItemPosition(getListMenit().indexOf(getsWaktuOn().split(":")[1].length()==1?"0"+getsWaktuOn().split(":")[1]:getsWaktuOn().split(":")[1]));

        JamOff.setData(getListJam());
        JamOff.setSelectedItemPosition(getListJam().indexOf(getsWaktuOff().split(":")[0].length()==1?"0"+getsWaktuOff().split(":")[0]:getsWaktuOff().split(":")[0]));

        MenitOff.setData(getListMenit());
        MenitOff.setSelectedItemPosition(getListMenit().indexOf(getsWaktuOff().split(":")[1].length()==1?"0"+getsWaktuOff().split(":")[1]:getsWaktuOff().split(":")[1]));

        choose_ir.setChecked(getSwitchButton());

        choose_ir.setOnCheckedChangeListener((view, isChecked) -> setsSwitchButton(isChecked));
    }
    @SuppressLint("NonConstantResourceId")
    @OnClick(R.id.btnKirim)
    public void kirimData(){
        Date currentTime = Calendar.getInstance().getTime();

        int HourNow = currentTime.getHours();
        int MinuteNow = currentTime.getMinutes();
        int SecondNow = currentTime.getSeconds();

        int HourOn = Integer.valueOf(getListJam().get(JamOn.getCurrentItemPosition()));
        int MinuteOn = Integer.valueOf(getListMenit().get(MenitOn.getCurrentItemPosition()));

        setsWaktuOn(HourOn+":"+MinuteOn);

        int HourOff = Integer.valueOf(getListJam().get(JamOff.getCurrentItemPosition()));
        int MinuteOff = Integer.valueOf(getListMenit().get(MenitOff.getCurrentItemPosition()));

        setsWaktuOff(HourOff+":"+MinuteOff);

        assert audioManager != null;
        max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

        int[] d = new int[]{170, HourNow, MinuteNow, SecondNow, HourOn, MinuteOn, HourOff, MinuteOff};

        new DoinBackground().execute(d);
    }

    @SuppressLint("StaticFieldLeak")
    private class DoinBackground extends AsyncTask<int[], Integer, Boolean>{

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            btnKirim.setEnabled(false);
            btnKirim.setText("Mengirim Data");
        }

        @Override
        protected Boolean doInBackground(int[]... data) {
            int[] d = data[0];
            byte sum = 0;
            int count = 0;
            for (int i: d){
                a[count] = i;
                sum += (byte)i;
                count++;
            }
            a[8] = ~sum;
            byte[] newData;
            byte[] newData2;

            byte[] ch34Data;
            byte[] ch34Data2;

            if (isConnected) {
                UsbDevice device = usbService.getDevice();

                int deviceVID = device.getVendorId();
                int devicePID = device.getProductId();
                boolean isHoltek = !CH34xIds.isDeviceSupported(deviceVID, devicePID);

                try {
                    usbService.getSerialPort().setParameters(
                            460800,
                            UsbSerialInterface.DATA_BITS_8,
                            UsbSerialInterface.STOP_BITS_2,
                            UsbSerialInterface.PARITY_EVEN
                    );
                } catch (IOException e) { }

                newData = getData(new byte[]{
                        (byte) a[0],
                        (byte) a[1],
                        (byte) a[2],
                        (byte) a[3],
                        (byte) a[4]
                }, true);
                newData2 = getData(new byte[]{
                        (byte) a[5],
                        (byte) a[6],
                        (byte) a[7],
                        (byte) a[8]
                }, true);

//                newData = getData(new byte[]{
//                        (byte) a[0],
//                        (byte) a[1],
//                        (byte) a[2],
//                        (byte) a[3],
//                        (byte) a[4],
//                        (byte) a[5],
//                        (byte) a[6],
//                        (byte) a[7],
//                        (byte) a[8]
//                }, true);

                usbService.write(newData);
                usbService.write(newData2);
            }else{
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    ir.sendCode(a);
                }else{
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "Not Support Operating System, Please Using OTG", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            btnKirim.setEnabled(true);
            btnKirim.setText("SEND");
        }
    }

    public static byte DATA_HIGH = (byte) 219;
    public static byte DATA_LOW = (byte) 248;
    public static int PANJANG_BIT = 125;
    public static int LENGTH_DATA = PANJANG_BIT * a.length;

    public byte[] getData(byte[] bytes, boolean isHoltek){
        PANJANG_BIT = isHoltek?125:140;
        LENGTH_DATA = PANJANG_BIT * a.length;
        return getData(bytes);
    }

    public byte[] getData(byte[] bytes){
        byte[] output = new byte[((LENGTH_DATA)*10)];
        Arrays.fill(output, DATA_HIGH);
        int i=0;
        int j=0;
        for (byte b : bytes){
            /*
             * Start
             * */
            for (int start=0;start<PANJANG_BIT;start++){
                output[i] = DATA_LOW;
                i++;
            }

            /*
             * Data 1 byte
             * */

            boolean[] bArray = getBinary(b);
            for (boolean val : bArray){
                if (val){
                    /*
                     * High
                     * */
                    for (int start=0;start<PANJANG_BIT;start++){
                        output[i] = DATA_HIGH;
                        i++;
                    }

                }else{
                    for (int start=0;start<PANJANG_BIT;start++){
                        output[i] = DATA_LOW;
                        i++;
                    }
                }
            }
            /*
             * jeda
             * */
            if (j<8){
                for (int jeda=0;jeda<8;jeda++){
                    for (int start=0;start<PANJANG_BIT;start++){
                        output[i] = DATA_HIGH;
                        i++;
                    }
                }
            }
            j++;
        }
        return output;
    }

    public boolean[] getBinary(int input){
        boolean[] bits = new boolean[9];
        for (int i = 8; i >= 0; i--) {
            bits[i] = (input & (1 << i)) != 0;
        }
        return bits;
    }
    public void showBantuan(){
        Intent i = new Intent(this,BantuanActivity.class);
        startActivity(i);
        overridePendingTransition(R.anim.slide_up, R.anim.stay);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_help) {
            showBantuan();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    private void initPermission(){
        if (Build.VERSION.SDK_INT >= 23) {
            boolean retVal = Settings.System.canWrite(this);
            if (retVal){
//                initPutarLayar();
            }else{
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, 102);
            }
        }
    }
    private void buildDialog(){
        builder = new AlertDialog.Builder(getWindow().getContext());
        builder.setCancelable(false);
        builder.setMessage("Putar Layar?");
        builder.setPositiveButton("IYA", (dialog, which) -> {
            setPutarLayar(1);
            doPutarLayar(2);
        });
        builder.setNegativeButton("TIDAK",(dialog, which)-> {
            setPutarLayar(2);
            doPutarLayar(0);
        });
        alertDialog = builder.create();
    }
    private void showDialog(){
        alertDialog.show();
    }
    private void initPutarLayar(){
//        doPutarLayar(2);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 102){
            initPutarLayar();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
    private void doPutarLayar(int code){
//        boolean retVal = false;
//        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
//            retVal = Settings.System.canWrite(this);
//            if (retVal){
////                Handler handler = new Handler();
//                mHandler.post(() -> Settings.System.putInt(getContentResolver(), Settings.System.USER_ROTATION, code));
//            }
//        }else{
//            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
//        }

    }

    private void checkUart(){
//        PeripheralManager manager = PeripheralManager.getInstance();
//        List<String> deviceList = manager.getUartDeviceList();
//        if (deviceList.isEmpty()) {
//            Log.i(TAG, "No UART port available on this device.");
//        } else {
//            Log.i(TAG, "List of available devices: " + deviceList);
//        }
    }



    @Override
    protected void onStart() {
        super.onStart();
        initPutarLayar();

        setFilters();
        startService(UsbService.class, usbConnection, null); // Start UsbService(if it was not started before) and Bind it
    }

    @Override
    protected void onResume() {
        super.onResume();
        initPutarLayar();

        setFilters();
        startService(UsbService.class, usbConnection, null); // Start UsbService(if it was not started before) and Bind it
    }


    @Override
    protected void onPause() {
        doPutarLayar(0);
        super.onPause();
        try {
            unregisterReceiver(mUsbReceiver);
            unbindService(usbConnection);
        }catch (Exception ignored){}
    }
    @Override
    protected void onDestroy() {
        doPutarLayar(0);
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (isConnected) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, "Lepas OTG Lebih Dahulu", Toast.LENGTH_SHORT).show();
                }
            });
        }else{
            super.onBackPressed();
            return;
        }
    }

    private static class MyHandler extends Handler {
        private final WeakReference<MainActivity> mActivity;

        public MyHandler(MainActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UsbService.MESSAGE_FROM_SERIAL_PORT:
                    String data = (String) msg.obj;
                    break;
                case UsbService.CTS_CHANGE:
                    Toast.makeText(mActivity.get(), "CTS_CHANGE",Toast.LENGTH_LONG).show();
                    break;
                case UsbService.DSR_CHANGE:
                    Toast.makeText(mActivity.get(), "DSR_CHANGE",Toast.LENGTH_LONG).show();
                    break;
                case UsbService.SYNC_READ:
                    String buffer = (String) msg.obj;
                    break;
            }
        }
    }

    private void startService(Class<?> service, ServiceConnection serviceConnection, Bundle extras) {
        if (!UsbService.SERVICE_CONNECTED) {
            Intent startService = new Intent(this, service);
            if (extras != null && !extras.isEmpty()) {
                Set<String> keys = extras.keySet();
                for (String key : keys) {
                    String extra = extras.getString(key);
                    startService.putExtra(key, extra);
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(startService);
            } else {
                startService(startService);
            }
        }
        Intent bindingIntent = new Intent(this, service);
        bindService(bindingIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void setFilters() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbService.ACTION_USB_PERMISSION_GRANTED);
        filter.addAction(UsbService.ACTION_NO_USB);
        filter.addAction(UsbService.ACTION_USB_DISCONNECTED);
        filter.addAction(UsbService.ACTION_USB_NOT_SUPPORTED);
        filter.addAction(UsbService.ACTION_USB_PERMISSION_NOT_GRANTED);
        registerReceiver(mUsbReceiver, filter);
    }

    @Override
    public void setRequestedOrientation(int requestedOrientation) {
        super.setRequestedOrientation(requestedOrientation);

        Window win = getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            winParams.rotationAnimation = WindowManager.LayoutParams.ROTATION_ANIMATION_JUMPCUT;
        }
        win.setAttributes(winParams);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }
}
