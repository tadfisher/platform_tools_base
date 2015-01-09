<resources>
    <#if !isNewProject>
    <string name="title_${activityToLayout(activityClass)}">${escapeXmlString(activityTitle)}</string>
    </#if>
    <string name="hello_world">Hello world!</string>
    <string name="action_settings">Settings</string>

    <!-- -
        This is an ad unit ID for a test ad. Replace with your own banner ad unit id.
        For more information, please go to https://support.google.com/admob/answer/3052638
    <!- -->
    <#if adFormat == "banner">
    <string name="banner_ad_unit_id">ca-app-pub-3940256099942544/6300978111</string>
    <#elseif adFormat == "interstitial">
    <string name="impossible_game">Impossible Game</string>
    <string name="interstitial_ad_unit_id">ca-app-pub-3940256099942544/1033173712</string>
    </#if>
</resources>
