<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:id="@+id/container"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context="${relativePackage}.${activityClass}"
    tools:ignore="MergeRootFrame">

    <fragment
        android:id="@+id/fragment"
        android:name="${packageName}.${fragmentClass}"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />
</FrameLayout>
