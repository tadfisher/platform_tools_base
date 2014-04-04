<#assign includeGooglePlus = includePlus || includeDrive || includeGames || includeWallet>
package ${packageName};

import android.annotation.TargetApi;
import android.app.Activity;
<#if includeGooglePlus>
import android.app.AlertDialog;
import android.app.Dialog;
</#if>
import android.app.LoaderManager.LoaderCallbacks;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.CursorLoader;
<#if includeGooglePlus>
import android.content.DialogInterface;
</#if>
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
<#if minApiLevel lt 14>import android.os.Build.VERSION;</#if>
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
<#if includeGooglePlus>
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.SignInButton;
</#if>
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
<#if applicationPackage??>import ${applicationPackage}.R;</#if>

<#if includeDrive>
import com.google.android.gms.drive.Drive;
</#if>
<#if includeGames>
import com.google.android.gms.games.Games;
</#if>
<#if includeGooglePlus>
import com.google.android.gms.plus.Plus;
</#if>
<#if includeWallet>
import com.google.android.gms.wallet.Wallet;
</#if>

/**
 * A sign in screen that offers sign-in via email/password<#if includeGooglePlus> and via Google+ Sign-In</#if>.
 */
public class ${activityClass} extends Activity<#if includeGooglePlus> implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener</#if> {

    private static final String TAG = "${activityClass}";

    /**
     * Use this task to send credentials to your server and retrieve a session
     * token for use by your app.
     */
    private SessionAuthTask mSessionAuthTask = null;
<#if includeGooglePlus>
    /*
     * TODO: To enable Google+ Sign-In for your app, you must first go to:
     * https://developers.google.com/+/mobile/android/getting-started#step_1_enable_the_google_api
     * and follow the steps to create an OAuth 2.0 client for your Android client
     * and your server.  The server's client ID should be configured below.
     */
    private static final String SERVER_CLIENT_ID = "YOUR_SERVER_CLIENT_ID";

    private static final int STATE_DEFAULT = 0;
    private static final int STATE_SIGN_IN = 1;
    private static final int STATE_IN_PROGRESS = 2;

    private static final int RC_SIGN_IN = 0;

    private static final int DIALOG_PLAY_SERVICES_ERROR = 0;

    private static final String SAVED_PROGRESS = "sign_in_progress";

    // GoogleApiClient wraps our service connection to Google Play services and
    // provides access to the users sign in state and Google's APIs.
    private GoogleApiClient mGoogleApiClient;

    // We use mSignInProgress to track whether user has clicked sign in.
    // mSignInProgress can be one of three values:
    //
    //       STATE_DEFAULT: The default state of the application before the user
    //                      has clicked 'Google+ sign in'.  In this state we
    //                      will not attempt to resolve sign in errors.
    //       STATE_SIGN_IN: This state indicates that the user has clicked
    //                      'Google+ sign in', so resolve successive errors
    //                      preventing Google+ sign-in until the user has
    //                      successfully authorized an account for this app.
    //   STATE_IN_PROGRESS: This state indicates that we have started an intent to
    //                      resolve a Google+ sign-in error, and so we should not
    //                      start further intents until the current intent
    //                      completes.
    private int mSignInProgress;

    // Used to store the PendingIntent most recently returned by Google Play
    // services until the user clicks 'Google+ Sign-In'.
    private PendingIntent mSignInIntent;

    // Used to store the error code most recently returned by Google Play services
    // until the user clicks 'Google+ Sign-In'.
    private int mSignInError;
</#if>
    private AutoCompleteTextView mEmailView;
    private EditText mPasswordView;
    private Button mEmailSignInButton;
    private ProgressDialog mSessionAuthProgress;<#if includeGooglePlus>
    private SignInButton mSignInButton;</#if>

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signin);<#if parentActivityClass != "">
        setupActionBar();</#if>
        populateAutoComplete();

        mSessionAuthProgress = new ProgressDialog(this);
        mSessionAuthProgress.setMessage("Signing in...");
<#if includeGooglePlus>
        mSignInButton = (SignInButton) findViewById(R.id.plus_sign_in_button);

        if (GooglePlayServicesUtil.isGooglePlayServicesAvailable(this) ==
                ConnectionResult.SUCCESS) {
            // Set a listener to connect the user when the Google+ Sign-In
            // button is clicked.
            mSignInButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    // TODO: Remove this after you have configured your OAuth 2.0 Client ID
                    if (SERVER_CLIENT_ID.equals("YOUR_SERVER_CLIENT_ID")) {
                        AlertDialog clientIdDialog = new AlertDialog.Builder(SignInActivity.this)
                                .setMessage("You must configure an OAuth 2.0 client ID at "
                                        + "https://console.developers.google.com for both "
                                        + "your Android app and your server.  Then set "
                                        + "the value of SERVER_CLIENT_ID in SignInActivity.")
                                .setPositiveButton(android.R.string.ok,
                                        new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        dialogInterface.dismiss();
                                    }
                                })
                                .create();
                        clientIdDialog.show();
                    } else {

                        if (!mGoogleApiClient.isConnecting()) {
                            // We only process button clicks when GoogleApiClient is
                            // not transitioning between disconnected and connected.
                            resolveSignInError();
                        }

                    }
                }
            });

            mGoogleApiClient = buildGoogleApiClient();
        } else {
            // Don't offer Google+ Sign-In if the app's version is too low to
            // support Google Play services.
            mSignInButton.setVisibility(View.GONE);
        }

        if (savedInstanceState != null) {
            mSignInProgress = savedInstanceState
                .getInt(SAVED_PROGRESS, STATE_DEFAULT);
        }
</#if>
        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.email_sign_in || id == EditorInfo.IME_NULL) {
                    emailSignIn();
                    return true;
                }
                return false;
            }
        });

        mEmailView = (AutoCompleteTextView) findViewById(R.id.email);
        mEmailSignInButton = (Button) findViewById(R.id.email_sign_in_button);
        mEmailSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                emailSignIn();
            }
        });
    }
<#if parentActivityClass != "">
    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void setupActionBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            // Show the Up button in the action bar.
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }
</#if>
<#if includeGooglePlus>
    private GoogleApiClient buildGoogleApiClient() {
        // When we build the GoogleApiClient we specify where connected and
        // connection failed callbacks should be returned, which Google APIs our
        // app uses and which OAuth 2.0 scopes our app requests.
        return new GoogleApiClient.Builder(this)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            <#if includeDrive>
            .addApi(Drive.API)
            </#if>
            <#if includeGames>
            .addApi(Games.API)
            </#if>
            .addApi(Plus.API)
            <#if includeWallet>
            .addApi(Wallet.API)
            </#if>
            <#if includeDrive>
            .addScope(Drive.SCOPE_FILE)
            </#if>
            <#if includeGames>
            .addScope(Games.SCOPE_GAMES)
            </#if>
            .addScope(Plus.SCOPE_PLUS_LOGIN)
            // Optionally, add additional APIs and scopes if required.
            .build();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SAVED_PROGRESS, mSignInProgress);
    }

    /* onConnected is called when our Activity successfully connects to Google
     * Play services.  onConnected indicates that an account was selected on the
     * device, that the selected account has granted any requested permissions to
     * our app and that we were able to establish a service connection to Google
     * Play services.
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        // Update the user interface to reflect that the user is signed in.
        mSignInButton.setEnabled(false);

        // Indicate that the sign in process is complete.
        mSignInProgress = STATE_DEFAULT;

        if (mSessionAuthTask == null) {
            // Show a progress spinner, and kick off a background task to
            // perform the authentication step with our server.
            mSessionAuthProgress.show();
            String googleAccountName = Plus.AccountApi.getAccountName(mGoogleApiClient);
            mSessionAuthTask = new SessionAuthTask(googleAccountName);
            mSessionAuthTask.execute();
        }
    }

    @Override
    public void onConnectionSuspended(int cause) {
        mSignInProgress = STATE_DEFAULT;
        googleSignOut();
    }

    /* onConnectionFailed is called when our Activity could not connect to Google
     * Play services.  onConnectionFailed indicates that the user needs to select
     * an account, grant permissions or resolve an error in order to sign in.
     */
    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // Refer to the javadoc for ConnectionResult to see what error codes might
        // be returned in onConnectionFailed.

        if (mSignInProgress != STATE_IN_PROGRESS) {
            // We do not have an intent in progress so we should store the latest
            // error resolution intent for use when the sign in button is clicked.
            mSignInIntent = result.getResolution();
            mSignInError = result.getErrorCode();

            if (mSignInProgress == STATE_SIGN_IN) {
                // STATE_SIGN_IN indicates the user already clicked the sign in button
                // so we should continue processing errors until the user is signed in
                // or they click cancel.
                resolveSignInError();
            }
        }
    }

    /* Starts an appropriate intent or dialog for user interaction to resolve
     * the current error preventing the user from being signed in.  This could
     * be a dialog allowing the user to select an account, an activity allowing
     * the user to consent to the permissions being requested by your app, a
     * setting to enable device networking, etc.
     */
    private void resolveSignInError() {
        if (mSignInIntent != null) {
            // We have an intent which will allow our user to sign in or
            // resolve an error.  For example if the user needs to
            // select an account to sign in with, or if they need to consent
            // to the permissions your app is requesting.

            try {
                // Send the pending intent that we stored on the most recent
                // OnConnectionFailed callback.  This will allow the user to
                // resolve the error currently preventing our connection to
                // Google Play services.
                mSignInProgress = STATE_IN_PROGRESS;
                startIntentSenderForResult(mSignInIntent.getIntentSender(),
                    RC_SIGN_IN, null, 0, 0, 0);
            } catch (SendIntentException e) {
                Log.i(TAG, "Sign in intent could not be sent: "
                    + e.getLocalizedMessage());
                // The intent was canceled before it was sent.  Attempt to connect to
                // get an updated ConnectionResult.
                mSignInProgress = STATE_SIGN_IN;
                mGoogleApiClient.connect();
            }
        } else {
            // Google Play services wasn't able to provide an intent for some
            // error types, so we show the default Google Play services error
            // dialog which may still start an intent on our behalf if the
            // user can resolve the issue.
            showDialog(DIALOG_PLAY_SERVICES_ERROR);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
            Intent data) {
        switch (requestCode) {
            case RC_SIGN_IN:
                if (resultCode == RESULT_OK) {
                    // If the error resolution was successful we should continue
                    // processing errors.
                    mSignInProgress = STATE_SIGN_IN;
                } else {
                    // If the error resolution was not successful or the user canceled,
                    // we should stop processing errors.
                    mSignInProgress = STATE_DEFAULT;
                }

                if (!mGoogleApiClient.isConnecting()) {
                    // If Google Play services resolved the issue with a dialog then
                    // onStart is not called so we need to re-attempt connection here.
                    mGoogleApiClient.connect();
                }
                break;
        }
    }

    private void googleSignOut() {
        if (mGoogleApiClient.isConnected()) {
            Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
            mGoogleApiClient.disconnect();
            mGoogleApiClient.connect();
        }
        mSignInButton.setEnabled(true);
    }
</#if>
    private void emailSignIn() {
        if (mSessionAuthTask == null && validateEmailForm()) {
            String email = mEmailView.getText().toString();
            String password = mPasswordView.getText().toString();
            // Show a progress spinner, and kick off a background task to
            // perform the authentication step with our server.
            mSessionAuthProgress.show();
            mSessionAuthTask = new SessionAuthTask(email, password);
            mSessionAuthTask.execute();
        }
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    public boolean validateEmailForm() {
        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the sign-in attempt.
        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean isValid = true;

        // Check for a valid password, if the user entered one.
        if (TextUtils.isEmpty(password)) {
            mPasswordView.setError(getString(R.string.error_password_required));
            mPasswordView.requestFocus();
            isValid = false;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_email_required));
            mEmailView.requestFocus();
            isValid = false;
        }

        return isValid;
    }

    /**
     * SessionAuthTask should pass the users ID and credentials (either email
     * and password, or ID token) to your server in order to identify and
     * authenticate your server session.
     */
    public class SessionAuthTask extends AsyncTask<Void, Void, Boolean> {

        private final String mEmail;
        private final String mPassword;
        <#if includeGooglePlus>private final String mGoogleAccountName;</#if>
        private Exception mError;

        SessionAuthTask(String email, String password) {
            mEmail = email;
            mPassword = password;
            <#if includeGooglePlus>mGoogleAccountName = null;</#if>
        }
<#if includeGooglePlus>
        SessionAuthTask(String googleAccountName) {
            mEmail = null;
            mPassword = null;
            mGoogleAccountName = googleAccountName;
        }
</#if>
        @Override
        protected Boolean doInBackground(Void... params) {
<#if includeGooglePlus>
            String idToken = null;

            if (mGoogleAccountName != null) {
                try {
                    // Obtain an ID token from Google to send to our server.
                    // The ID token authenticates both the app and the user to
                    // the server.  For examples of how to verify the ID token
                    // on the server see https://developers.google.com/+/web/signin/token-verification
                    idToken = GoogleAuthUtil.getToken(${activityClass}.this, mGoogleAccountName,
                            "audience:server:client_id:" + SERVER_CLIENT_ID);
                } catch (IOException e) {
                    Log.e(TAG, "Failed to fetch ID Token: "
                        + "IOException indicates a temporary failure. "
                        + "This is a good place to implement exponential "
                        + "backoff and retry, or fail.", e);
                    mError = e;
                    return false;
                } catch (GoogleAuthException e) {
                    Log.e(TAG, "Failed to fetch ID token: "
                        + "This is likely a programming error.  Please "
                        + "check that you are using the correct client ID "
                        + "to retrieve your ID token.", e);
                    mError = e;
                    return false;
                }
            }
</#if>
            try {
                sendCredentials(mEmail, mPassword<#if includeGooglePlus>, idToken</#if>);
            }
            catch (UnknownUserException e) {
                mError = e;
                return false;
            }
            catch (InvalidCredentialsException e) {
                mError = e;
                return false;
            }

            return true;
        }

        protected void sendCredentials(String email, String password<#if includeGooglePlus>, String idToken</#if>)
            throws UnknownUserException, InvalidCredentialsException {

            // TODO: send credentials to your server to authenticate the session
            Log.e(TAG, getString(R.string.signin_not_implemented));

        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mSessionAuthTask = null;
            mSessionAuthProgress.hide();

            if (success) {
                mSessionAuthProgress.dismiss();
                finish();
            } else if (mError instanceof UnknownUserException) {
<#if includeGooglePlus>
                if (mGoogleAccountName != null) {
                    // Reset the GoogleApiClient to signed out state since the
                    // server authentication failed.
                    googleSignOut();

                    // TODO: start registration flow for Google+ Sign-In
                    Toast.makeText(${activityClass}.this,
                        getString(R.string.registration_not_implemented),
                        Toast.LENGTH_LONG).show();

                } else {
</#if>
                    mEmailView.setError(getString(R.string.error_unknown_user));
                    mEmailView.requestFocus();
<#if includeGooglePlus>
                }
</#if>
            } else if (mError instanceof InvalidCredentialsException) {
<#if includeGooglePlus>
                if (mGoogleAccountName != null) {
                    // Reset the GoogleApiClient to signed out state since the
                    // server authentication failed.
                    googleSignOut();

                    Log.e(TAG, "Invalid ID token obtained from Google: "
                        + "This is likely a programming error.  Please "
                        + "check that you are using the correct client ID "
                        + "to retrieve your ID token.");
                } else {
</#if>
                    mPasswordView.setError(getString(R.string.error_incorrect_password));
                    mPasswordView.requestFocus();
<#if includeGooglePlus>
                }
</#if>
            } else {
                Toast.makeText(SignInActivity.this,
                        getString(R.string.error_signin_failed), Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected void onCancelled() {
<#if includeGooglePlus>
            // Reset the GoogleApiClient to signed out state since the
            // server authentication was cancelled.
            googleSignOut();
</#if>
            mSessionAuthTask = null;
            mSessionAuthProgress.hide();
        }
    }

    private void populateAutoComplete() {
<#if minApiLevel gte 14>
        getLoaderManager().initLoader(0, null, new EmailAutoCompleteCallbacks());
<#else>
        if (VERSION.SDK_INT >= 14) {
            // Use ContactsContract.Profile (API 14+)
            getLoaderManager().initLoader(0, null, new EmailAutoCompleteCallbacks());
        } else if (VERSION.SDK_INT >= 8) {
            // Use AccountManager (API 8+)
            new EmailAutoCompleteTask().execute(null, null);
        }
</#if>
    }

    private class EmailAutoCompleteCallbacks implements LoaderCallbacks<Cursor> {
        @Override
        @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
        public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
            return new CursorLoader(${activityClass}.this,
                    // Retrieve data rows for the device user's 'profile' contact.
                    Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI,
                            ContactsContract.Contacts.Data.CONTENT_DIRECTORY), ProfileQuery.PROJECTION,

                    // Select only email addresses.
                    ContactsContract.Contacts.Data.MIMETYPE +
                            " = ?", new String[]{ContactsContract.CommonDataKinds.Email
                                                                         .CONTENT_ITEM_TYPE},

                    // Show primary email addresses first. Note that there won't be
                    // a primary email address if the user hasn't specified one.
                    ContactsContract.Contacts.Data.IS_PRIMARY + " DESC");
        }

        @Override
        public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
            List<String> emails = new ArrayList<String>();
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                emails.add(cursor.getString(ProfileQuery.ADDRESS));
                cursor.moveToNext();
            }

            addEmailsToAutoComplete(emails);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> cursorLoader) { }
    }

    private interface ProfileQuery {
        String[] PROJECTION = {
                ContactsContract.CommonDataKinds.Email.ADDRESS,
                ContactsContract.CommonDataKinds.Email.IS_PRIMARY,
        };

        int ADDRESS = 0;
        int IS_PRIMARY = 1;
    }

<#if minApiLevel lt 14>
    /**
     * Use an AsyncTask to fetch the user's email addresses on a background thread, and update
     * the email text field with results on the main UI thread.
     */
    private class EmailAutoCompleteTask extends AsyncTask<Void, Void, List<String>> {

        @Override
        protected List<String> doInBackground(Void... voids) {
            ArrayList<String> emailAddressCollection = new ArrayList<String>();

            // Get all emails from the user's contacts and copy them to a list.
            ContentResolver cr = getContentResolver();
            Cursor emailCur = cr.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI, null,
                    null, null, null);
            while (emailCur.moveToNext()) {
                String email = emailCur.getString(emailCur.getColumnIndex(ContactsContract
                        .CommonDataKinds.Email.DATA));
                emailAddressCollection.add(email);
            }
            emailCur.close();

            return emailAddressCollection;
        }

            @Override
            protected void onPostExecute(List<String> emailAddressCollection) {
               addEmailsToAutoComplete(emailAddressCollection);
            }
    }
</#if>

    private void addEmailsToAutoComplete(List<String> emailAddressCollection) {
        //Create adapter to tell the AutoCompleteTextView what to show in its dropdown list.
        ArrayAdapter<String> adapter =
                new ArrayAdapter<String>(SignInActivity.this,
                        android.R.layout.simple_dropdown_item_1line, emailAddressCollection);

        mEmailView.setAdapter(adapter);
    }

    private class UnknownUserException extends Exception {};

    private class InvalidCredentialsException extends Exception {};
}
