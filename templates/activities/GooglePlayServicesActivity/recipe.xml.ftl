<?xml version="1.0"?>
<recipe>
    <dependency mavenUrl="com.google.android.gms:play-services:4.2.42" />

    <merge from="AndroidManifest.xml.ftl"
            to="${escapeXmlAttribute(manifestOut)}/AndroidManifest.xml" />

    <merge from="res/values/strings.xml.ftl"
            to="${escapeXmlAttribute(resOut)}/values/strings.xml" />

    <instantiate from="src/app_package/activity.java.ftl"
            to="${escapeXmlAttribute(srcOut)}/${activityClass}.java" />

    <#if includeCloudSave>
      <instantiate from="src/app_package/eventservice.java.ftl"
            to="${escapeXmlAttribute(srcOut)}/${cloudSaveService}.java" />
    </#if>

    <open file="${escapeXmlAttribute(srcOut)}/${activityClass}.java" />
    <#if includeCloudSave>
      <open file="${escapeXmlAttribute(srcOut)}/${cloudSaveService}.java" />
    </#if>


</recipe>
