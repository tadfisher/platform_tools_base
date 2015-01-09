<resources>
    <#if !isNewProject>
    <string name="title_${activityToLayout(activityClass)}">${escapeXmlString(activityTitle)}</string>
    </#if>
    <string name="hello_world">Hello world!</string>
    <string name="action_settings">Settings</string>
    <#if adNetwork == "admob" && adFormat == "banner">
    <string name="banner_ad_unit_id">ca-app-pub-3940256099942544/6300978111</string>
    </#if>
</resources>
