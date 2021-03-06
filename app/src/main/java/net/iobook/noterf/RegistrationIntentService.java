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
import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

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
                InstanceID instanceID = InstanceID.getInstance(this);
                String token = instanceID.getToken(getString(R.string.gcm_defaultSenderId),
                        GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
                sendRegistrationToServer(token, intent.getStringExtra(MainActivity.IOPUSH_AUTH_TOKEN));

                // Subscribe to topic channels
                // subscribeTopics(token);

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
     * Persist registration to ioPsuh server.
     *
     * Modify this method to associate the user's GCM registration token with any server-side account
     * maintained by your application.
     *
     * @param regId The new token.
     */
    private void sendRegistrationToServer(String regId, String authToken) {

        try {
            // Send authToken to the server
            HttpHelper http = new HttpHelper(null, null, 7000);
            JSONObject data = new JSONObject();
            try {
                data.put("service", "AndroidGCM");
                data.put("regId", regId);
                data.put("name", "TODO: Nom");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            http.setUser(null);
            http.setPassword(null);
            ArrayList<String[]> headers = new ArrayList<>();
            headers.add(new String[]{"authentication_token", authToken});
            headers.add(new String[]{"Content-Type", "application/json"});
            HttpResultHelper httpResult = http.post("https://ioPush.net/app/api/addDevice", data.toString(), headers);
            BufferedReader in = new BufferedReader(new InputStreamReader(httpResult.getResponse()));
            String result = "";
            String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    result += inputLine;
                }
                if (httpResult.getStatusCode() == 200) {
                    Log.i(TAG, "Send regId result : " + result);
                } else {
                    Log.i(TAG, "Failed to send regId, error code : " + httpResult.getStatusCode());
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
    /*
    // [START subscribe_topics]
    private void subscribeTopics(String token) throws IOException {
        GcmPubSub pubSub = GcmPubSub.getInstance(this);
        for (String topic : TOPICS) {
            pubSub.subscribe(token, "/topics/" + topic, null);
        }
    }
    // [END subscribe_topics]
    */
}