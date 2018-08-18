package com.nexgo.haydenm.exatest;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;

import static android.content.ContentValues.TAG;

public class MainActivity extends AppCompatActivity {

    private String _ping_test_host_address = "8.8.8.8";
    private Button run_tests_button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        run_tests_button = (Button) findViewById(R.id.run_tests_button);
        run_tests_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {



            }
        });
    }

    public void executeNetworkTests()
    {




    }

    /**
     * Checks if ANY network is connected (WIFI or CELLULAR) - but only denotes a connection, and
     * doesn't guarentee there is a connection to the public internet.
     * @return true if either WIFI is connected to an AP or GSM is connected; false if no networks
     * are connected.
     */
    private boolean isNetworkOnline() {
        boolean status=false;
        try{
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = cm.getNetworkInfo(0);
            if (netInfo != null && netInfo.getState()==NetworkInfo.State.CONNECTED) {
                status= true;
            }else {
                netInfo = cm.getNetworkInfo(1);
                if(netInfo!=null && netInfo.getState()==NetworkInfo.State.CONNECTED)
                    status= true;
            }
        }catch(Exception e){
            e.printStackTrace();
            return false;
        }
        return status;
    }

    /**
     * Checks if DNS resolutions are working. Returns result as boolean.
     * @return true if DNS resolution is successful, false if it fails w/ any exception.
     */
    private boolean isDNSResolutionWorking()
    {
        try {
            InetAddress address = InetAddress.getByName("www.example.com");
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Checks if ping requests are being run successfully.
     * @param url The host address to be 'pinged'
     * @param count The number of times to 'ping' the host address
     * @return true if ping exits successfully, and otherwise returns false.
     */
    public static boolean isPingSuccessful(String url, int count) {
        Process process;
        try {
            String str = "";
            Log.d(TAG, "Starting ping with url" + url);
            Log.d(TAG, "creating process : linux ping command...");

            process = Runtime.getRuntime().exec(
                    "/system/bin/ping -q -c " + count + " " + url);
            Log.d(TAG, "command issued");
            Log.d(TAG, "opening reader...");


            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    process.getInputStream()));
            Log.d(TAG, "reading...");

            int i;
            char[] buffer = new char[4096];
            StringBuffer output = new StringBuffer();
            while ((i = reader.read(buffer)) > 0)
                output.append(buffer, 0, i);
            Log.d(TAG, "read: " + buffer);

            reader.close();

            // body.append(output.toString()+"\n");
            str = output.toString();
            Log.d(TAG, str);
            Log.d(TAG, "Done with ping. Should have output...");
            System.out.println(str);


        } catch (IOException e) {
            // body.append("Error\n");
            e.printStackTrace();
            return false;
        }
        return process.exitValue() == 0;
    }

}
