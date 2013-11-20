<manifest xmlns:android="http://schemas.android.com/apk/res/android" >

    <!--To access Google+ APIs:-->
    <uses-permission android:name="android.permission.INTERNET" />

    <!--To retrieve the account name (email) as part of sign-in:-->
    <uses-permission android:name="android.permission.GET_ACCOUNTS" />

    <!--To retrieve OAuth 2.0 tokens or invalidate tokens to disconnect a user. This disconnect
     option is required to comply with the Google+ Sign-In developer policies -->
    <uses-permission android:name="android.permission.USE_CREDENTIALS" />

    <!--To auto-complete the email text field in the login form using known emails -->
    <uses-permission android:name="android.permission.READ_CONTACTS" />

    <application>
        <activity android:name=".${activityClass}"
            <#if isNewProject>
            android:label="@string/app_name"
            <#else>
            android:label="@string/title_${simpleName}"
            </#if>
            android:windowSoftInputMode="adjustResize|stateVisible"
            <#if buildApi gte 16 && parentActivityClass != "">android:parentActivityName="${parentActivityClass}"</#if>>
            <#if parentActivityClass != "">
            <meta-data android:name="android.support.PARENT_ACTIVITY"
                android:value="${parentActivityClass}" />
            </#if>
        </activity>
        <meta-data android:name="com.google.android.gms.version" android:value="4" />

    </application>

</manifest>
