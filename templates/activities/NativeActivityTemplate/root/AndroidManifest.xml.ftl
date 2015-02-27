<manifest xmlns:android="http://schemas.android.com/apk/res/android" >

    <application>
        <activity android:name="android.app.NativeActivity"
            <#if isNewProject>
            android:label="@string/app_name"
            </#if>
            android:configChanges="orientation|keyboardHidden">

            <meta-data android:name="android.app.lib_name"
                        android:value="${activityClass}" />
            <#if isLauncher>
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
            </#if>
        </activity>
    </application>

</manifest>
