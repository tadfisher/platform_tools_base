<?xml version="1.0" encoding="utf-8"?>
<android.support.wearable.view.BoxInsetLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:id="@+id/container"
    tools:context="${relativePackage}.${activityClass}"
    tools:deviceIds="wear">

    <TextView
        android:id="@+id/text"
        app:layout_box="all"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:text="@string/hello_world" />

    <TextView
        android:id="@+id/clock"
        app:layout_box="all"
        android:layout_gravity="bottom|start"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:textColor="@android:color/white" />

</android.support.wearable.view.BoxInsetLayout>
