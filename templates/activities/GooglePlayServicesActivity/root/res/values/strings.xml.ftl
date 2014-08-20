<resources>
    <#if !isNewProject>
    <string name="title_${activityToLayout(activityClass)}">${escapeXmlString(activityTitle)}</string>
    </#if>
    <#if includeCloudSave>
    <!-- TODO -->
    <string name="app_id">TODO: Get your ID in Play Store | Game Services</string>
    </#if>
</resources>
