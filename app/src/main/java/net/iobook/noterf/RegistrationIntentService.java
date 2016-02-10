package net.iobook.noterf;

/**
 * Created by olivier on 03/12/15.
 */
import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.gcm.GcmPubSub;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import java.io.IOException;

import java.io.InputStream;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import android.util.Base64;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;

import java.io.OutputStream;

import java.net.HttpURLConnection;


public class RegistrationIntentService extends IntentService {

    private static final String TAG = "RegIntentService";
    private static final String[] TOPICS = {"global"};

    public RegistrationIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // if (sharedPreferences.getBoolean(QuickstartPreferences.SENT_TOKEN_TO_SERVER, false) == false ) {

            try {
                // [START register_for_gcm]
                // Initially this call goes out to the network to retrieve the token, subsequent calls
                // are local.
                // R.string.gcm_defaultSenderId (the Sender ID) is typically derived from google-services.json.
                // See https://developers.google.com/cloud-messaging/android/start for details on this file.
                // [START get_token]
                InstanceID instanceID = InstanceID.getInstance(this);
                String token = instanceID.getToken(getString(R.string.gcm_defaultSenderId),
                        GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
                // [END get_token]
                Log.i(TAG, "GCM Registration Token: " + token);

                // TODO: Implement this method to send any registration to your app's servers.
                sendRegistrationToServer(token);

                // Subscribe to topic channels
                subscribeTopics(token);

                // You should store a boolean that indicates whether the generated token has been
                // sent to your server. If the boolean is false, send the token to your server,
                // otherwise your server should have already received the token.
                sharedPreferences.edit().putBoolean(QuickstartPreferences.SENT_TOKEN_TO_SERVER, true).apply();
                // [END register_for_gcm]
            } catch (Exception e) {
                Log.d(TAG, "Failed to complete token refresh", e);
                // If an exception happens while fetching the new token or updating our registration data
                // on a third-party server, this ensures that we'll attempt the update at a later time.
                sharedPreferences.edit().putBoolean(QuickstartPreferences.SENT_TOKEN_TO_SERVER, false).apply();
            }
            // Notify UI that registration has completed, so the progress indicator can be hidden.
            Intent registrationComplete = new Intent(QuickstartPreferences.REGISTRATION_COMPLETE);
            LocalBroadcastManager.getInstance(this).sendBroadcast(registrationComplete);
       /* } else {
            Log.i(TAG, "Token already sent");
        }*/
    }

    /** Issue post request
     * http://stackoverflow.com/questions/16504527/how-to-do-an-https-post-from-android
     * @param urlStr
     * @param user
     * @param password
     * @return
     * @throws IOException
     */
    private String httpPost(String urlStr, String user, String password) throws IOException
    {
        URL url = new URL(urlStr);
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();

        // Create the SSL connection
        try {
            SSLContext sc;
            sc = SSLContext.getInstance("TLS");
            sc.init(null, null, new java.security.SecureRandom());
            conn.setSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
            Log.d(TAG, "Failed to construct SSL object", e);
        }


        // Use this if you need SSL authentication
        String userpass = user + ":" + password;
        String basicAuth = "Basic " + Base64.encodeToString(userpass.getBytes(), Base64.DEFAULT);
        conn.setRequestProperty("Authorization", basicAuth);

        // set Timeout and method
        conn.setRequestProperty("Connection", "close"); // Connection must be closed in order to set length of next request ??
        conn.setReadTimeout(7000);
        conn.setConnectTimeout(7000);
        conn.setRequestMethod("POST");
        conn.setDoInput(true);

        // Add any data you wish to post here

        conn.connect();
        int status = conn.getResponseCode();
        Log.i(TAG, "Post status : " + status);

        InputStream is = conn.getInputStream();

        BufferedReader in = new BufferedReader(new InputStreamReader(is));
        String inputLine;
        String result = new String();
        while ((inputLine = in.readLine()) != null) {
            result += inputLine;
            Log.i(TAG, "Received : " + inputLine);
        }

        conn.disconnect();

        return result;
    }

    private InputStream httpPostDevice(String urlStr, String authToken, String regId) throws IOException
    {
        URL url = new URL(urlStr);
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();

        // Create the SSL connection
        try {
            SSLContext sc;
            sc = SSLContext.getInstance("TLS");
            sc.init(null, null, new java.security.SecureRandom());
            conn.setSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
            Log.d(TAG, "Failed to construct SSL object", e);
        }

        // Set parameters
        String param="{\"service\": \"" + "AndroidGCM" +
                "\", \"regId\": \"" + regId +
                "\", \"name\": \"" + "Xiaomi Redmi" + "\"}";
        Log.i(TAG, "Param : " + param);

        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        // Set Timeout and method
        conn.setReadTimeout(7000);
        conn.setConnectTimeout(7000);

        // Add headers
        // conn.setRequestProperty( "Accept-Encoding", "");
        //conn.setRequestProperty("Connection", "close");
        conn.setRequestProperty("authentication_token", authToken);
        conn.setFixedLengthStreamingMode(param.getBytes().length);
        conn.setRequestProperty("Content-Type", "application/json");




        // Send the request
        /*PrintWriter out = new PrintWriter(conn.getOutputStream());
        out.print(param);
        out.close();*/

        OutputStream os = conn.getOutputStream();
        BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(os, "UTF-8"));
        writer.write(param);
        writer.flush();
        writer.close();
        os.close();

        int status = conn.getResponseCode();
        Log.i(TAG, "Post status : " + status);
        Log.i(TAG, conn.getResponseMessage());
        conn.getErrorStream();

        InputStream inputStream = null;
        try
        {
            inputStream = conn.getInputStream();
        }
        catch(IOException exception)
        {
            inputStream = conn.getErrorStream();
        }
        return inputStream;
    }

    /**
     * Persist registration to third-party servers.
     *
     * Modify this method to associate the user's GCM registration token with any server-side account
     * maintained by your application.
     *
     * @param regId The new token.
     */
    private void sendRegistrationToServer(String regId) {
        String result = new String();
        // Get authentication key
        try {
            result = httpPost("https://ioPush.net/app/api/getAuthToken", "oliv4945@gmail.com", "*******");
            // result = httpPost("http://192.168.0.14:5000/api/getAuthToken", "oliv4945@gmail.com", "aatest");
            // is = httpPostDevice("https://ioPush.net/app/api/addDevice", result, regId);
            String inputLine;
            // InputStream is = httpPostDevice("http://192.168.0.14:5000/api/addDevice", result, regId);
            InputStream is = httpPostDevice("https://ioPush.net/app/api/addDevice", result, regId);
            BufferedReader in = new BufferedReader(new InputStreamReader(is));
            result = "";
            while ((inputLine = in.readLine()) != null) {
                result += inputLine;
                Log.i(TAG, "Received : " + inputLine);
            }
        } catch (Exception e) {
            Log.d(TAG, "Failed to issue post request", e);
        }

    }

    /**
     * Subscribe to any GCM topics of interest, as defined by the TOPICS constant.
     *
     * @param token GCM token
     * @throws IOException if unable to reach the GCM PubSub service
     */
    // [START subscribe_topics]
    private void subscribeTopics(String token) throws IOException {
        GcmPubSub pubSub = GcmPubSub.getInstance(this);
        for (String topic : TOPICS) {
            pubSub.subscribe(token, "/topics/" + topic, null);
        }
    }
    // [END subscribe_topics]

}