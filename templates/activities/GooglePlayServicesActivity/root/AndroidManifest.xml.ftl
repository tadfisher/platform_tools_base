<manifest xmlns:android="http://schemas.android.com/apk/res/android" >

    <application>
        <activity android:name="${relativePackage}.${activityClass}"
            <#if isNewProject>
            android:label="@string/app_name"
            <#else>
            android:label="@string/title_${activityToLayout(activityClass)}"
            </#if>
            >
            <#if isLauncher>
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
            </#if>
        </activity>

        <meta-data
            android:name="com.google.android.gms.version"
            android:value="@integer/google_play_services_version" />

        <#if includeCloudSave>
        <meta-data
            android:name="com.google.android.gms.cloudsave.APP_ID"
            android:value="@string/appid" />

        <uses-permission android:name="android.permission.INTERNET" />
        <uses-permission android:name="android.permission.GET_ACCOUNTS" />
        </#if>
    </application>

</manifest>
