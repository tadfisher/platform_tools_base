<RelativeLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:ads="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <!--ads:adUnitId sets the ad unit ID, which is defined in values/strings.xml -->
    <com.google.android.gms.ads.<#if adNetwork == "admob">AdView<#elseif adNetwork == "dfp">doubleclick.PublisherAdView</#if>
        android:id="@+id/adView"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_centerHorizontal="true"
        ads:adSize="BANNER"
        ads:adUnitId="@string/banner_ad_unit_id" />

</RelativeLayout>