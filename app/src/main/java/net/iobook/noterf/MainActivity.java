package net.iobook.noterf;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

public class MainActivity extends AppCompatActivity {

    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private static final String TAG = "MainActivity";
    public final static String IOPUSH_EMAIL = "net.iobook.noterf.ioPushEmail";
    public final static String IOPUSH_AUTH_TOKEN = "net.iobook.noterf.ioPushAuthToken";

    private BroadcastReceiver mRegistrationBroadcastReceiver;
    private BroadcastReceiver mAuthenticationBroadcastReceiver;
    private ProgressBar mRegistrationProgressBar;
    private TextView mInformationTextView;
    private SharedPreferences userSharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mInformationTextView = (TextView) findViewById(R.id.informationTextView);
        mRegistrationProgressBar = (ProgressBar) findViewById(R.id.registrationProgressBar);
        mRegistrationProgressBar.setVisibility(ProgressBar.GONE);


        // Restore nickname and authToken
        userSharedPref = getApplicationContext().getSharedPreferences(
                getString(R.string.preferenceUser), Context.MODE_PRIVATE);
        String nickname = userSharedPref.getString(getString(R.string.preferenceUserNickname), null);
        String authToken = userSharedPref.getString(getString(R.string.preferenceUserAuthToken), null);

        // Get them if not stored
        if ((nickname == null) || (authToken == null)){
            Log.i(TAG, "Ask for credential details");
            // Wait for credential signal
            mAuthenticationBroadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String nickname = userSharedPref.getString(getString(R.string.preferenceUserNickname), null);
                    String message = nickname + " " + getString(R.string.user_signed_in);
                    mInformationTextView.setText(message);
                    startRegistrationIntentService();
                }
            };
            // Fire login activity
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
        } else {
            startRegistrationIntentService();
        }


    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
                new IntentFilter(QuickstartPreferences.REGISTRATION_COMPLETE));
        LocalBroadcastManager.getInstance(this).registerReceiver(mAuthenticationBroadcastReceiver,
                new IntentFilter(QuickstartPreferences.AUTHENTICATION_COMPLETE));
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRegistrationBroadcastReceiver);
        super.onPause();
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)
                        .show();
            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }

    /*
     * Start RegistrationIntentService
     */
    private void startRegistrationIntentService() {
        // Register GCM
        mRegistrationProgressBar = (ProgressBar) findViewById(R.id.registrationProgressBar);
        mRegistrationBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mRegistrationProgressBar.setVisibility(ProgressBar.GONE);
                SharedPreferences sharedPreferences =
                        PreferenceManager.getDefaultSharedPreferences(context);
                boolean sentToken = sharedPreferences
                        .getBoolean(QuickstartPreferences.SENT_TOKEN_TO_SERVER, false);
                if (sentToken) {
                    String nickname = userSharedPref.getString(getString(R.string.preferenceUserNickname), null);
                    String message = getString(R.string.hello) + " "+ nickname + " " + getString(R.string.gcm_send_message);
                    mInformationTextView.setText(message);
                } else {
                    mInformationTextView.setText(getString(R.string.token_error_message));
                }
            }
        };
        mInformationTextView = (TextView) findViewById(R.id.informationTextView);
        if (checkPlayServices()) {
            // Start IntentService to register this application with GCM.
            Log.i(TAG, "Get GCM regId");
            String authToken = userSharedPref.getString(getString(R.string.preferenceUserAuthToken), null);
            Intent regIntent = new Intent(this, RegistrationIntentService.class);
            regIntent.putExtra(IOPUSH_AUTH_TOKEN, authToken);
            startService(regIntent);
        }
    }

}