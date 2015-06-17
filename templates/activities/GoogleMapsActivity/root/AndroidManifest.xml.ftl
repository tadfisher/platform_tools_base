<manifest xmlns:android="http://schemas.android.com/apk/res/android" >
    <uses-permission android:name="android.permission.INTERNET"/>
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>

    <!-- The ACCESS_COARSE/FINE_LOCATION permissions are not required to use
         Google Maps Android API v2, but one of them is needed for the 'MyLocation' functionality. -->
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>


    <!-- OpenGL ES version 2 is required by the Google Maps Android API v2. -->
    <uses-feature android:glEsVersion="0x00020000" android:required="true"/>

    <application>
        <meta-data android:name="com.google.android.gms.version" android:value="@integer/google_play_services_version" />

        <!-- The API key for Google Maps-based APIs is defined as a string resource.
             (See the file "res/values/google_maps_api.xml").
             Note that the API key is linked to key used to sign the APK. Different API keys are needed for
             all keys used to sign the APK, including the release key that is used to sign the APK for publishing.
             The keys can be defined for the debug and release targets in src/debug/ and src/release/. -->
        <meta-data android:name="com.google.android.geo.API_KEY" android:value="@string/google_maps_key"/>

        <activity android:name="${relativePackage}.${activityClass}"
            android:label="@string/title_${simpleName}">
            <#if parentActivityClass != "">
            <meta-data android:name="android.support.PARENT_ACTIVITY"
                android:value="${parentActivityClass}" />
            </#if>
            <#if isLauncher>
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
            </#if>

        </activity>
    </application>

</manifest>
