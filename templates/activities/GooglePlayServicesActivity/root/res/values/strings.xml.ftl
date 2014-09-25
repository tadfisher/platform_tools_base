<resources>
    <#if !isNewProject>
    <string name="title_${activityToLayout(activityClass)}">${escapeXmlString(activityTitle)}</string>
    </#if>
    <#if includeCloudSave>
    <string name="appid">${cloudsaveProjectID}</string>
    </#if>
</resources>

