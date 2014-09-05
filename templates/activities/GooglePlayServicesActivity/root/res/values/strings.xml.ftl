<resources>
    <#if !isNewProject>
    <string name="title_${activityToLayout(activityClass)}">${escapeXmlString(activityTitle)}</string>
    </#if>
    <#if includeCloudSave>
    <string name="appid_${activityToLayout(activityClass)}">${cloudsaveProjectID}</string>
    </#if>
</resources>

