package ${packageName};

<#if adNetwork == "admob">
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.<#if adFormat == "banner">AdView<#elseif adFormat == "interstitial">InterstitialAd</#if>;
<#elseif adNetwork == "dfp">
import com.google.android.gms.ads.doubleclick.PublisherAdRequest;
import com.google.android.gms.ads.doubleclick.<#if adFormat == "banner">PublisherAdView<#elseif adFormat == "interstitial">PublisherInterstitialAd</#if>;
</#if>

import android.content.Context;
import android.os.Bundle;
<#if adFormat == "interstitial">import android.os.CountDownTimer;</#if>
import <#if appCompat>android.support.v7.app.ActionBarActivity<#else>android.app.Activity</#if>;
import android.<#if appCompat>support.v7.</#if>app.ActionBar;
import android.<#if appCompat>support.v4.</#if>app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
<#if adFormat == "interstitial">
import android.widget.Button;
import android.widget.TextView;
</#if>
import android.widget.Toast;

<#if applicationPackage??>import ${applicationPackage}.R;</#if>

public class ${activityClass} extends ${appCompat?string('ActionBar','')}Activity {
    // Remove the below line after having your own ad unit id defined.
    private static final String TOAST_TEXT = "Test ads are being show. "
            + "Please remove the ad unit id in res/values/string.xml and replace with your own ad unit id.";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.${layoutName});

        // Remove the below lines in onCreate method after having your own ad unit id defined.
        Context context = getApplicationContext();
        CharSequence text = TOAST_TEXT;
        int duration = Toast.LENGTH_LONG;
        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
    }

    <#include "include_options_menu.java.ftl">

    <#if adFormat == "banner">
    <#include "include_banner_fragment.java.ftl">
    <#elseif adFormat == "interstitial">
    <#include "include_interstitial_fragment.java.ftl">
    </#if>
}
