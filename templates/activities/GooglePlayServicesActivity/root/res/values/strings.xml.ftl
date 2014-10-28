<resources>
    <#if !isNewProject>
    <string name="title_${activityToLayout(activityClass)}">${escapeXmlString(activityTitle)}</string>
    </#if>
    <#if includeCloudSave>
    <string name="projectid">${cloudsaveProjectID}</string>
    </#if>
</resources>

