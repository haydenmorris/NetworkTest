package com.nexgo.haydenm.exatest;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;

import static android.content.ContentValues.TAG;

public class MainActivity extends AppCompatActivity {

    private String _ping_test_host_address = "8.8.8.8";

    private Button run_tests_button;
    private TextView network_connection_results_output;// = (TextView) findViewById(R.id.network_connection_result_output);
    private TextView dns_resolution_results_output;
    private TextView ping_test_results_output;
    private TextView http_request_results_output;

    private boolean networkOnline = false;
    private boolean dnsOnline     = false;
    private boolean pingOnline    = false;
    private boolean httpOnline    = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        network_connection_results_output   = (TextView) findViewById(R.id.network_connection_result_output);
        dns_resolution_results_output       = (TextView) findViewById(R.id.dns_resolution_results_output);
        ping_test_results_output            = (TextView) findViewById(R.id.ping_test_results_output);
        http_request_results_output         = (TextView) findViewById(R.id.http_request_result_output);

        run_tests_button = (Button) findViewById(R.id.run_tests_button);
        run_tests_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                System.out.println("Connected to some network: " + isNetworkOnline());

                if (!isNetworkOnline())
                {
                    System.out.println("Not connected to ANY network. No reason to proceed.");

                    new AlertDialog.Builder(MainActivity.this)
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {

                                    //Perform the DNS Resolution Test
                                    DNSTestAsyncCaller dnsTest = new DNSTestAsyncCaller();
                                    dnsTest.execute();

                                    //Perform the Ping Test
                                    PingTestAsyncCaller pingTest = new PingTestAsyncCaller();
                                    pingTest.execute();

                                    HTTPRequestTestAsyncCaller httpTest = new HTTPRequestTestAsyncCaller();
                                    httpTest.execute();

                                }
                            })
                            .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    System.out.println("NO --> Clicked CANCEL / NO GEN AGAIN");

                                }
                            })
                            .setTitle("No Networks Connected")
                            .setMessage("Warning! \n\nNo connected networks were detected. \n" +
                                    "\nAll the remaining tests will fail, proceed anyways?")
                            .show();
                }
                else
                {
                    //Set connection true and make it green since we know we're connected from the 'if' stmt
                    network_connection_results_output.setText("Connected");
                    network_connection_results_output.setTextColor(Color.GREEN);

                    //Perform the DNS Resolution Test
                    DNSTestAsyncCaller dnsTest = new DNSTestAsyncCaller();
                    dnsTest.execute();

                    //Perform the Ping Test
                    PingTestAsyncCaller pingTest = new PingTestAsyncCaller();
                    pingTest.execute();


                    HTTPRequestTestAsyncCaller httpTest = new HTTPRequestTestAsyncCaller();
                    httpTest.execute();

                }

            }
        });
    }

    /**
     *
     * @param test_name
     *          NetworkConnected --> Returns whether there is any connection to WIFI AP or GSM Tower.
     *          DNSResolution    --> Returns whether a DNS Resolution test is able to complete.
     *          PingTest         --> Returns whether a Pingtest is able to complete.
     * @return whether or not the requested test passed / failed. Returns boolean denoted as much.
     */
    public boolean executeNetworkTests(String test_name)
    {
        switch (test_name)
        {
            case "NetworkConnected":
                return isNetworkOnline();
            case "DNSResolution":
                return isDNSResolutionWorking("google.com");
            case "PingTest":
                return isPingSuccessful("8.8.8.8", 3);
            default:
                    return false;
        }

    }



    ////////////////////////////////////////////////////////////////////////////////////////////////
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
    private boolean isDNSResolutionWorking(String host_name)
    {
        try {
            InetAddress address = InetAddress.getByName(host_name);
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
    public static boolean isPingSuccessful(String url, int count)  {
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
        try {
            return process.waitFor() == 0;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static int getHttpResponseCode(String myURL) {

        int statusCode = -1;
        System.out.println("Attempting to connect to " + myURL + "...");
        try {
            URL url = new URL(myURL);
            HttpURLConnection http = (HttpURLConnection) url.openConnection();

            //set http request timeout value in ms
            http.setRequestMethod("HEAD");
            http.setConnectTimeout(2000);

            statusCode = http.getResponseCode();
        } catch (java.net.UnknownHostException ex) {
            Log.d(TAG, "Error connecting or resolving hostname.");
            Log.d(TAG, "Unknown Host Exception Error: " + ex.getMessage());
            ex.printStackTrace();
        } catch (java.net.MalformedURLException ex) {
            Log.d(TAG, "The URL is malformed.");
            Log.d(TAG, "Malformed URL Exception Error: " + ex.getMessage());
            ex.printStackTrace();
        } catch (java.net.SocketTimeoutException ex) {
            Log.d(TAG, "Timed out connecting to host");
            Log.d(TAG, "Host connection timeout: " + ex.getMessage());
            ex.printStackTrace();
        } catch (java.io.IOException ex) {
            Log.d(TAG, "There was an IOException");
            Log.d(TAG, "IOException Exception Error: " + ex.getMessage());
            ex.printStackTrace();
        }
        return statusCode;
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////

    private class DNSTestAsyncCaller extends AsyncTask<Void, Void, Void>
    {
        ProgressDialog pdLoading = new ProgressDialog(MainActivity.this);
        String responseCode = "";
        String url_input = "www.google.com";

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            //this method will be running on UI thread
            pdLoading.setMessage("\tRunning DNS Test...");
            pdLoading.setCanceledOnTouchOutside(false);
            pdLoading.setCancelable(false);
            pdLoading.show();
        }
        @Override
        protected Void doInBackground(Void... params) {

            //this method will be running on background thread so don't update UI frome here
            //do your long running http tasks here,you dont want to pass argument and u can access the parent class' variable url over here
            dnsOnline = isDNSResolutionWorking(url_input);

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);

            //this method will be running on UI thread
            dns_resolution_results_output.setText(responseCode);
            if (dnsOnline)
            {
                dns_resolution_results_output.setText("Online");
                dns_resolution_results_output.setTextColor(Color.GREEN);
            }
            else
            {
                dns_resolution_results_output.setText("Offline - FAIL");
                dns_resolution_results_output.setTextColor(Color.RED);
            }

            pdLoading.dismiss();
        }

    }

    private class PingTestAsyncCaller extends AsyncTask<Void, Void, Void>
    {
        ProgressDialog pdLoading = new ProgressDialog(MainActivity.this);
        String responseCode = "";
        String ping_url = "8.8.8.8";

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            //this method will be running on UI thread
            pdLoading.setMessage("\tRunning Ping Test...");
            pdLoading.setCanceledOnTouchOutside(false);
            pdLoading.setCancelable(false);
            pdLoading.show();
        }
        @Override
        protected Void doInBackground(Void... params) {

            //this method will be running on background thread so don't update UI frome here
            //do your long running http tasks here,you dont want to pass argument and u can access the parent class' variable url over here
            pingOnline = isPingSuccessful(ping_url, 5);

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);

            //this method will be running on UI thread
            if (pingOnline)
            {
                ping_test_results_output.setText("Online");
                ping_test_results_output.setTextColor(Color.GREEN);
            }
            else
            {
                ping_test_results_output.setText("Offline - FAIL");
                ping_test_results_output.setTextColor(Color.RED);
            }

            pdLoading.dismiss();
        }

    }

    private class HTTPRequestTestAsyncCaller extends AsyncTask<Void, Void, Void>
    {
        ProgressDialog pdLoading = new ProgressDialog(MainActivity.this);
        String responseCode = "";
        String request_url = "https://www.google.com/";

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            //this method will be running on UI thread
            pdLoading.setMessage("\tRunning HTTP Request Test...");
            pdLoading.setCanceledOnTouchOutside(false);
            pdLoading.setCancelable(false);
            pdLoading.show();
        }
        @Override
        protected Void doInBackground(Void... params) {

            //this method will be running on background thread so don't update UI frome here
            //do your long running http tasks here,you dont want to pass argument and u can access the parent class' variable url over here
            httpOnline = getHttpResponseCode(request_url) == 200;
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);

            //this method will be running on UI thread
            if (httpOnline)
            {
                http_request_results_output.setText("Online");
                http_request_results_output.setTextColor(Color.GREEN);
            }
            else
            {
                http_request_results_output.setText("Offline - FAIL");
                http_request_results_output.setTextColor(Color.RED);
            }

            pdLoading.dismiss();
        }

    }

}
