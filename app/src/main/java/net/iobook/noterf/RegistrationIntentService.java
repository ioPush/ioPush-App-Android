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
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.OutputStream;
import java.net.HttpURLConnection;

import org.json.JSONException;
import org.json.JSONObject;

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

    /**
     * HTTP request with POST method
     * @param urlStr
     * @param user
     * @param password
     * @param data
     * @param headers
     * @param timeOut
     * @return
     * @throws IOException
     */
    private HttpResultHelper httpPost(String urlStr, String user, String password, String data, String[][] headers, int timeOut) throws IOException
    {
        // Set url
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        // If secure connection
        if (urlStr.startsWith("https")) {
            try {
                SSLContext sc;
                sc = SSLContext.getInstance("TLS");
                sc.init(null, null, new java.security.SecureRandom());
                ((HttpsURLConnection)conn).setSSLSocketFactory(sc.getSocketFactory());
            } catch (Exception e) {
                Log.d(TAG, "Failed to construct SSL object", e);
            }
        }


        // Use this if you need basic authentication
        if ((user != null) && (password != null)) {
            String userPass = user + ":" + password;
            String basicAuth = "Basic " + Base64.encodeToString(userPass.getBytes(), Base64.DEFAULT);
            conn.setRequestProperty("Authorization", basicAuth);
        }

        // Set Timeout and method
        // conn.setRequestProperty("Connection", "close"); // Connection must be closed in order to set length of next request ??
        conn.setReadTimeout(timeOut);
        conn.setConnectTimeout(timeOut);
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        try {
            for (int i=0; i<=headers.length; i++) {
                // conn.setRequestProperty("authentication_token", authToken);
                conn.setRequestProperty(headers[i][0], headers[i][1]);
            }
        } catch (NullPointerException e) {
        } catch (IndexOutOfBoundsException e) {
        }

        if (data != null) {
            conn.setFixedLengthStreamingMode(data.getBytes().length);
            OutputStream os = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(os, "UTF-8"));
            writer.write(data);
            writer.flush();
            writer.close();
            os.close();
        }

        InputStream inputStream = null;
        try
        {
            inputStream = conn.getInputStream();
        }
        catch(IOException exception)
        {
            inputStream = conn.getErrorStream();
        }

        HttpResultHelper result = new HttpResultHelper();
        result.setStatusCode(conn.getResponseCode());
        result.setResponse(inputStream);

        return result;
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
        String result, inputLine = new String();

        try {
            // Get authentication key
            HttpResultHelper httpResult = httpPost("https://ioPush.net/app/api/getAuthToken", "**@ioPush.net", "***", null, null, 7000);
            BufferedReader in = new BufferedReader(new InputStreamReader(httpResult.getResponse()));
            result = "";
            while ((inputLine = in.readLine()) != null) {
                result += inputLine;
            }
            if (httpResult.getStatusCode() == 200) {
                Log.i(TAG, "Result : " + result);
                JSONObject data = new JSONObject();
                try {
                    data.put("service", "AndroidGCM");
                    data.put("regId", regId);
                    data.put("name", "Xiaomi Redmi");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                Log.i(TAG, data.toString());
                String[][] headers = new String[2][2];
                headers[0][0] = "authentication_token";
                headers[0][1] = result;
                headers[1][0] = "Content-Type";
                headers[1][1] = "application/json";
                httpResult = httpPost("https://ioPush.net/app/api/addDevice", null, null, data.toString(), headers, 7000);
                in = new BufferedReader(new InputStreamReader(httpResult.getResponse()));
                result = "";
                while ((inputLine = in.readLine()) != null) {
                    result += inputLine;
                }
                if (httpResult.getStatusCode() == 200) {
                    Log.i(TAG, "Result 2 : " + result);
                } else {
                    Log.i(TAG, "Failed 2 to issue post request, error code : " + httpResult.getStatusCode());
                    Log.i(TAG, "Error 2 message : " + result);
                }
            } else {
                Log.i(TAG, "Failed to issue post request, error code : " + httpResult.getStatusCode());
                Log.i(TAG, "Error message : " + result);
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