<RelativeLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    xmlns:ads="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:paddingLeft="@dimen/activity_horizontal_margin"
    android:paddingRight="@dimen/activity_horizontal_margin"
    android:paddingTop="@dimen/activity_vertical_margin"
    android:paddingBottom="@dimen/activity_vertical_margin"
    tools:context="${relativePackage}.${activityClass}$<#if adFormat == "banner">PlaceholderFragment<#elseif adFormat == "interstitial">GameFragment</#if>">

    <#if adFormat == "banner">
    <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/hello_world" />

    <#elseif adFormat == "interstitial">
    <TextView
            android:id="@+id/gameTitle"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_centerHorizontal="true"
            android:layout_marginTop="50dp"
            android:text="@string/impossible_game"
            android:textAppearance="?android:attr/textAppearanceLarge" />

        <TextView
            android:id="@+id/timer"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_below="@+id/gameTitle"
            android:layout_centerHorizontal="true"
            android:text="Large Text"
            android:textAppearance="?android:attr/textAppearanceLarge" />

        <Button
            android:id="@+id/retry_button"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_centerHorizontal="true"
            android:layout_centerVertical="true"
            android:text="Retry" />
    </#if>

</RelativeLayout>
