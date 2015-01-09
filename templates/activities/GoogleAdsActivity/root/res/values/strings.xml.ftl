<resources>
    <#if !isNewProject>
    <string name="title_${activityToLayout(activityClass)}">${escapeXmlString(activityTitle)}</string>
    </#if>
    <string name="hello_world">Hello world!</string>
    <string name="action_settings">Settings</string>

    <!-- This is an ad unit ID for a test ad. Replace with your own banner ad unit id. -->
    <#if adFormat == "banner">
    <string name="banner_ad_unit_id"><#if adNetwork == "admob">ca-app-pub-3940256099942544/6300978111<#elseif adNetwork == "dfp">/6253334/dfp_example_ad</#if></string>
    <#elseif adFormat == "interstitial">
    <string name="impossible_game">Impossible Game</string>
    <string name="interstitial_ad_unit_id"><#if adNetwork == "admob">ca-app-pub-3940256099942544/1033173712<#elseif adNetwork == "dfp">/6499/example/interstitial</#if></string>
    </#if>
</resources>
