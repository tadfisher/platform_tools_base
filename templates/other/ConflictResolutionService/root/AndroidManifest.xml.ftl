<manifest xmlns:android="http://schemas.android.com/apk/res/android" >
  <application>
     <service android:name="${relativePackage}.${className}"
            android:permission="com.google.android.gms.cloudsave.EVENT_BROADCAST"
            android:exported="${isExported?string}"
            android:enabled="${isEnabled?string}" >
        <intent-filter>
            <action android:name="com.google.android.gms.cloudsave.EVENT"/>
        </intent-filter>
     </service>
     <uses-permission android:name="android.permission.INTERNET" />
     <uses-permission android:name="android.permission.GET_ACCOUNTS" />
  </application>
</manifest>
