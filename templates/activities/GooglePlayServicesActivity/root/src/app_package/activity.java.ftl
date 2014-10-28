package ${packageName};

import android.app.Activity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;

<#if includeCast>
import com.google.android.gms.cast.Cast;
</#if>
<#if includeDrive>
import com.google.android.gms.drive.Drive;
</#if>
<#if includeGames>
import com.google.android.gms.games.Games;
</#if>
<#if includePlus>
import com.google.android.gms.plus.Plus;
</#if>
<#if includeWallet>
import com.google.android.gms.wallet.Wallet;
</#if>
<#if includeCloudSave>
import com.google.android.gms.cloudsave.CloudSaveManager;
</#if>

public class ${activityClass} extends Activity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "${activityClass}";

    private static final String KEY_IN_RESOLUTION = "is_in_resolution";

    /**
     * Request code for auto Google Play Services error resolution.
     */
    protected static final int REQUEST_CODE_RESOLUTION = 1;

    /**
     * Google API client.
     */
    private GoogleApiClient mGoogleApiClient;

    /**
     * Used to determine if the client is currently in a resolution state or is
     * waiting for a resolution intent to return.
     */
    private boolean mIsInResolution;

    /**
     * Called when the activity is created. Restores any activity state, if it exists.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mIsInResolution = savedInstanceState.getBoolean(KEY_IN_RESOLUTION, false);
        }
    }

    /**
     * Called when the Activity is made visible.
     * A connection to Play Services needs to be initiated as
     * soon as the activity is visible. Registers {@code ConnectionCallbacks}
     * and {@code OnConnectionFailedListener} on the
     * activity itself.
     */
    @Override
    protected void onStart() {
        super.onStart();
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    <#if includeCast>
                    .addApi(Cast.API)
                    </#if>
                    <#if includeDrive>
                    .addApi(Drive.API)
                    </#if>
                    <#if includeGames>
                    .addApi(Games.API)
                    </#if>
                    <#if includePlus>
                    .addApi(Plus.API)
                    </#if>
                    <#if includeWallet>
                    .addApi(Wallet.API)
                    </#if>
                    <#if includeCloudSave>
                    .addApi(CloudSaveManager.API)
                    </#if>
                    <#if includeDrive>
                    .addScope(Drive.SCOPE_FILE)
                    </#if>
                    <#if includeGames>
                    .addScope(Games.SCOPE_GAMES)
                    </#if>
                    <#if includePlus>
                    .addScope(Plus.SCOPE_PLUS_LOGIN)
                    </#if>
                    <#if includeCloudSave>
                    .addScope(CloudSaveManager.SCOPE_CLOUD_SAVE)
                    </#if>
                    // Optionally, add additional APIs and scopes if required.
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        }
        mGoogleApiClient.connect();
    }

    /**
     * Called when the activity is no longer visible. Connection to Play Services needs to
     * be disconnected as soon as an activity is no longer visible.
     */
    @Override
    protected void onStop() {
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    /**
     * Saves the resolution state.
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_IN_RESOLUTION, mIsInResolution);
    }

    /**
     * Handles Google Play Services resolution callbacks.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
        case REQUEST_CODE_RESOLUTION:
            retryConnecting();
            break;
        }
    }

    private void retryConnecting() {
        mIsInResolution = false;
        if (!mGoogleApiClient.isConnecting()) {
            mGoogleApiClient.connect();
        }
    }

    /**
     * Called when the {@code mGoogleApiClient} is connected.
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(TAG, "GoogleApiClient connected");
    <#if includeCloudSave>
        // TODO 1: Before you use the Cloud Save API, you need to obtain a client ID for your Android app in Google Developers Console.
        // To get a client ID, follow this link, follow the directions and press "Create" at the end:
        //   https://console.developers.google.com/flows/enableapi?apiid=datastoremobile&keyType=CLIENT_SIDE_ANDROID&r=${debugKeystoreSha1}%3B${packageName}&project=${cloudsaveProjectID}
        //
        // TODO 2: Once you have registered the client ID, you need to create a consent screen in Google Developers Console.
        // To create a consent screen, follow this link, supply your email address and product name (the other fields in the dialog are optional) and click "Save".
        //   https://console.developers.google.com/project/${cloudsaveProjectID}/apiui/consent
        //
        // TODO 3: Start making API requests.
        //
        // To resolve merge conflicts manually, add a CloudSave ConflictResolutionService by selecting
        // New->Google->CloudSave ConflictResolutionService
    <#else>
        // TODO: Start making API requests.
    </#if>
    }

    /**
     * Called when {@code mGoogleApiClient} connection is suspended.
     */
    @Override
    public void onConnectionSuspended(int cause) {
        Log.i(TAG, "GoogleApiClient connection suspended");
        retryConnecting();
    }

    /**
     * Called when {@code mGoogleApiClient} has failed to connect.
     * Handle {@code result.getResolution()} if there is a resolution
     * available.
     */
    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.i(TAG, "GoogleApiClient connection failed: " + result.toString());
        if (!result.hasResolution()) {
            // Show a localized error dialog.
            GooglePlayServicesUtil.getErrorDialog(
                    result.getErrorCode(), this, 0, new OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    retryConnecting();
                }
            }).show();
            return;
        }
        // If there is an existing resolution error being displayed or a resolution
        // activity has already been started, do nothing and wait for resolution
        // progress to be completed.
        if (mIsInResolution) {
            return;
        }
        mIsInResolution = true;
        try {
            result.startResolutionForResult(this, REQUEST_CODE_RESOLUTION);
        } catch (SendIntentException e) {
            Log.e(TAG, "Exception while starting resolution activity", e);
            retryConnecting();
        }
    }
}
