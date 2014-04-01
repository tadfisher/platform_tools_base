<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="${packageName}">

    <application <#if minApiLevel gte 4 && buildApi gte 4>android:allowBackup="true"</#if>>

    </application>

</manifest>
