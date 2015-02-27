<manifest xmlns:android="http://schemas.android.com/apk/res/android" >

    <!-- Tell the system this app requires OpenGL ES 2.0. -->
    <uses-feature android:glEsVersion="0x00020000" android:required="true" />

    <application>
        <activity android:name="android.app.NativeActivity"
            <#if isNewProject>
            android:label="@string/app_name"
            </#if>
            android:configChanges="orientation|keyboardHidden">

            <meta-data android:name="android.app.lib_name"
                        android:value="${libraryName}" />
            <#if isLauncher>
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
            </#if>
        </activity>
    </application>

</manifest>
