<RelativeLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:id="@+id/container"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context="${relativePackage}.${activityClass}"
    tools:ignore="MergeRootFrame" >

    <#if adFormat == "banner">
    <fragment
        android:name="${packageName}.${activityClass}$PlaceholderFragment"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:layout_above="@+id/adFragment" />

    <fragment
        android:id="@+id/adFragment"
        android:name="${packageName}.${activityClass}$AdFragment"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_alignParentBottom="true" />

     <#elseif adFormat == "interstitial">
     <fragment
             android:name="${packageName}.${activityClass}$GameFragment"
             android:layout_width="match_parent"
             android:layout_height="match_parent" />
     </#if>
</RelativeLayout>
