package com.uts.cas.wifireader;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.List;

public class WifiActivity extends Activity {


    WifiManager wifi;
    WifiScanReceiver wifiReciever;
    Thread wifiThread;
    Thread stringUpdater;
    boolean proceed = false;
    boolean lockone = false;
    boolean scanning = false;
    String fileText = "";
    int localCounter = 0;
    int length = 0;
    List<ScanResult> wifiScanList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi);

        wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        wifiReciever = new WifiScanReceiver();

    }

    protected void onResume() {
        registerReceiver(wifiReciever, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        super.onResume();
    }

    protected void onPause() {
        unregisterReceiver(wifiReciever);
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_wifi, menu);
        return true;
    }

    private class WifiScanReceiver extends BroadcastReceiver {
        @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
        public void onReceive(Context c, Intent intent) {

            lockone = true;
            wifiScanList = wifi.getScanResults();

            length = wifiScanList.size();
            lockone = false;

            if(proceed){
                wifi.startScan();
            }

            else{
                scanning = false;
                localCounter = 0;
                Toast.makeText(getApplicationContext(), "Scanning is stopped", Toast.LENGTH_SHORT).show();
                Button myButton = (Button) findViewById(R.id.button);
                myButton.setText("Start");
            }
        }
    }

    public void onStartStop(View v){

        if(!scanning){

            fileText = "";
            proceed = true;

            wifiThread = new Thread(new Runnable() {
                public void run() {
                    wifi.startScan();
                }
            });
            wifiThread.start();

            stringUpdater = new Thread(new Runnable() {
                @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
                public void run() {
                    while (true) {
                        if((wifiScanList != null) && (lockone == false)){
                            localCounter = localCounter + 1;

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    TextView count = (TextView) findViewById(R.id.textView);
                                    count.setText("Current Scan number for the region : " + localCounter);

                                    TextView len = (TextView) findViewById(R.id.textView2);
                                    len.setText("length : " + length);
                                }
                            });

                            Long tsLong = System.currentTimeMillis()/1000;
                            String ts = tsLong.toString();
                            if(!fileText.equals("")){
                                fileText = fileText.substring(0, fileText.length()-1);
                            }
                            fileText = fileText + "\n" + "Scan " + localCounter + "," + ts + ";";

                        for (int i = 0; i < wifiScanList.size(); i++) {
                            fileText = fileText + String.valueOf(wifiScanList.get(i).BSSID.toUpperCase() + "," + wifiScanList.get(i).SSID + "," + wifiScanList.get(i).frequency + "," + wifiScanList.get(i).level) + ";";
                        }
                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException e) {
                            return;
                        }
                    }
                    }
                }
            });
            stringUpdater.start();

            TextView count = (TextView) findViewById(R.id.textView);
            count.setText("Current Scan number for the region : " + localCounter);

            scanning = true;
            Button myButton = (Button) findViewById(R.id.button);
            myButton.setText("Stop");
        }
        else{
            stringUpdater.interrupt();
            proceed = false;
        }
    }

    public void onSave(View v){
        try {
            File path = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS);
            EditText fileName = (EditText)findViewById(R.id.editText);
            File myFile = new File(path, "scans" + fileName.getText() + ".txt");
            FileOutputStream fOut = new FileOutputStream(myFile,true);
            OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
            fileText = fileText.substring(1);                       //removing additional \n
            fileText = fileText.substring(0, fileText.length()-1);  //removing additional ;
            myOutWriter.append(fileText);
            myOutWriter.close();
            fOut.close();

            Toast.makeText(this,"Text file saved Successfully",Toast.LENGTH_LONG).show();
        }

        catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        if(stringUpdater != null){
            stringUpdater.interrupt();
        }
        if(wifiThread != null){
            wifiThread.interrupt();
        }
        super.onDestroy();
    }
}
