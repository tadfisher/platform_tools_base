<#if enableDeeplink>
<intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data android:scheme="${rightMostDomain(applicationPackage!(packageName!"http"))}"
        android:host="${reversePackageName(applicationPackage!(packageName!"http"))}"
        android:pathPrefix="/${activityClass!}" />
</intent-filter>
</#if>
