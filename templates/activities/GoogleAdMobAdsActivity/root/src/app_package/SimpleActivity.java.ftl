package ${packageName};

<#if adFormat == "banner">
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
<#elseif adFormat == "interstitial">
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
</#if>

import android.content.Context;
import android.os.Bundle;
<#if adFormat == "interstitial">
import android.<#if appCompat>support.v4.</#if>app.Fragment;
</#if>
import <#if appCompat>android.support.v7.app.ActionBarActivity<#else>android.app.Activity</#if>;
<#if adFormat == "interstitial">
import android.view.LayoutInflater;
</#if>
import android.view.Menu;
import android.view.MenuItem;
<#if adFormat == "interstitial">
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
</#if>
import android.widget.Toast;

<#if applicationPackage??>import ${applicationPackage}.R;</#if>

public class ${activityClass} extends ${(appCompat)?string('ActionBar','')}Activity {
    // Remove the below line after having your own ad unit id defined.
    private static final String TOAST_TEXT = "Test ads are being show. "
            + "Please remove the ad unit id in res/values/string.xml and replace with your own ad unit id.";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.${layoutName});

        <#if adFormat == "banner">
        AdView mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
        </#if>

        // Remove the below lines in onCreate method after having your own ad unit id defined.
        Context context = getApplicationContext();
        CharSequence text = TOAST_TEXT;
        int duration = Toast.LENGTH_LONG;
        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.${menuName}, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    <#if adFormat == "interstitial">
    /**
     * A fragment mocking levels in a game, with interstitial ads shown between levels.
     */
    public static class InterstitialFragment extends Fragment {
        private static final int START_LEVEL = 1;
        private int mLevel;
        private Button mNextLevelButton;
        private InterstitialAd mInterstitialAd;
        private TextView mLevelTextView;

        public InterstitialFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_interstitial, container, false);
            return rootView;
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            initializeView();
        }

        private void initializeView() {
            // Create the next level button, which tries to show an interstitial when clicked.
            mNextLevelButton = ((Button) getView().findViewById(R.id.next_level_button));
            mNextLevelButton.setEnabled(false);
            mNextLevelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    displayAd();
                }
            });

            // Create the text view to show the level number.
            mLevelTextView = (TextView) getView().findViewById(R.id.level);
            mLevel = START_LEVEL;

            // Create the InterstitialAd and set the adUnitId.
            mInterstitialAd = new InterstitialAd(getActivity());
            // Defined in values/strings.xml
            mInterstitialAd.setAdUnitId(getString(R.string.interstitial_ad_unit_id));
            mInterstitialAd.setAdListener(new AdListener() {
                @Override
                public void onAdLoaded() {
                    mNextLevelButton.setEnabled(true);
                }

                @Override
                public void onAdClosed() {
                    // Proceed to the next level.
                    goToNextLevel();
                }
            });
            startLoadAd();
        }

        private void displayAd() {
            // Show the ad if it's ready. Otherwise toast and reload the ad.
            if (mInterstitialAd != null && mInterstitialAd.isLoaded()) {
                mInterstitialAd.show();
            } else {
                Toast.makeText(getActivity(), "Ad did not load", Toast.LENGTH_LONG).show();
                startLoadAd();
            }
        }

        private void startLoadAd() {
            // Disable the next level button and load the ad.
            mNextLevelButton.setEnabled(false);
            AdRequest adRequest = new AdRequest.Builder().build();
            mInterstitialAd.loadAd(adRequest);
        }

        private void goToNextLevel() {
            // Show the next level and reload the ad to prepare for the level after.
            mLevelTextView.setText("Level " + (++mLevel));
            startLoadAd();
        }
    }
    </#if>
}
