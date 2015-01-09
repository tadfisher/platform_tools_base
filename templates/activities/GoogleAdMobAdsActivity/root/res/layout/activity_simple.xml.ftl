<RelativeLayout xmlns:android="http://schemas.android.com/apk/res/android"
    <#if adFormat == "banner">xmlns:ads="http://schemas.android.com/apk/res-auto"</#if>
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:paddingLeft="@dimen/activity_horizontal_margin"
    android:paddingRight="@dimen/activity_horizontal_margin"
    android:paddingTop="@dimen/activity_vertical_margin"
    android:paddingBottom="@dimen/activity_vertical_margin"
    tools:context="${relativePackage}.${activityClass}">

    <#if adFormat == "banner">
    <TextView
        android:text="@string/hello_world"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content" />

    <com.google.android.gms.ads.AdView
        android:id="@+id/adView"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_centerHorizontal="true"
        android:layout_alignParentBottom="true"
        ads:adSize="BANNER"
        ads:adUnitId="@string/banner_ad_unit_id" />

    <#elseif adFormat == "interstitial">
    <fragment
        android:name="${packageName}.${activityClass}$InterstitialFragment"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />
    </#if>

</RelativeLayout>
