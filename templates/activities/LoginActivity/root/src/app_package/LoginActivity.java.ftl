package ${packageName};

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;
<#if parentActivityClass != "">
import android.view.MenuItem;
import android.support.v4.app.NavUtils;
</#if>

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.SignInButton;

/**
 * A login screen that offers both login via email/password and via Google+ sign in.
 * <p/>
 * ************ IMPORTANT SETUP NOTES: ************
 * In order for Google+ sign in to work with your app, you must first go to:
 * https://developers.google.com/+/mobile/android/getting-started#step_1_enable_the_google_api
 * and follow the steps in "Step 1" to create an OAuth 2.0 client for your package.
 */
public class ${activityClass} extends PlusBaseActivity {

    private static final String TAG = "LoginActivity";
    private EmailLoginFragment emailLoginFragment;
    private AsyncTask task;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.${layoutName});
        <#if parentActivityClass != "">
        setupActionBar();
        </#if>

        // Find the sign in button.
        SignInButton mSignInButton = (SignInButton) findViewById(R.id.sign_in_button);
        if (supportsGooglePlayServices()) {
            // Set a listener to connect the user when the G+ button is clicked.
            mSignInButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    signIn();
                }
            });
        } else {
            // Don't offer G+ sign in if the app's version is too low to support Google Play
            // Services.
            mSignInButton.setVisibility(View.GONE);
            return;
        }

        // Add the email login fragment.
        FragmentManager fragmentManager = getSupportFragmentManager();
        if (fragmentManager.findFragmentById(R.id.email_login_fragment) == null) {
            emailLoginFragment = new EmailLoginFragment();
            getSupportFragmentManager().beginTransaction().add(R.id.email_login_fragment,
                    emailLoginFragment).commit();
        }
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // This ID represents the Home or Up button. In the case of this
                // activity, the Up button is shown. Use NavUtils to allow users
                // to navigate up one level in the application structure. For
                // more details, see the Navigation pattern on Android Design:
                //
                // http://developer.android.com/design/patterns/navigation.html#up-vs-back
                //
                // TODO: If Settings has multiple levels, Up should navigate up
                // that hierarchy.
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    </#if>


    @Override
    protected void onPlusClientSignIn() {
        String accountName = getPlusClient().getAccountName();
        Toast.makeText(this, accountName + " is connected.", Toast.LENGTH_LONG).show();
        finish();
    }

    @Override
    protected void onPlusClientBlockingUI(boolean show) {
        if (emailLoginFragment == null) {
            return;
        }
        emailLoginFragment.showProgress(show);
    }

    @Override
    protected void updateConnectButtonState() {
        // Not handled here, because the login activity finishes once the user is signed in.
    }

    @Override
    protected void onPlusClientRevokeAccess() {
        // Not handled here, but per the developer terms, your app must offer an option to revoke
        // access to the user's Google+ account and delete any stored user data.  A good place for
        // this is a Settings activity showing the user's connection status and extending
        // PlusBaseActivity.
        // You can revoke access by calling PlusBaseActivity.revokeAccess().
    }


    @Override
    protected void onPlusClientSignOut() {
        // Not handled here, but your app should offer an option to sign the user out in order to
        // switch accounts. A good place for this is a Settings activity showing the user's
        // connection status and extending PlusBaseActivity.
        // You can sign the user out by calling PlusBaseActivity.signOut().
    }

    /**
     * Check if the device supports Google Play Services.  It's best
     * practice to check first rather than handling this as an error case.
     *
     * @return whether the device supports Google Play Services
     */
    private boolean supportsGooglePlayServices() {
        return GooglePlayServicesUtil.isGooglePlayServicesAvailable(this) ==
                ConnectionResult.SUCCESS;
    }
}
