<RelativeLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:id="@+id/container"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context="${relativePackage}.${activityClass}"
    tools:ignore="MergeRootFrame" >

    <fragment android:name="${relativePackage}.${activityClass}$PlaceholderFragmen"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:layout_above="@+id/adFragment" />

    <fragment
        android:id="@+id/adFragment"
        android:name="${relativePackage}.${activityClass}$AdFragment"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_alignParentBottom="true" />

</RelativeLayout>
