<?xml version="1.0"?>
<recipe>
    <dependency mavenUrl="com.android.support:appcompat-v7:19.0.0"/>
    <dependency mavenUrl="com.google.android.gms:play-services:4.1.32" />

    <merge from="AndroidManifest.xml.ftl"
            to="${escapeXmlAttribute(manifestOut)}/AndroidManifest.xml" />

    <merge from="res/values/strings.xml.ftl"
            to="${escapeXmlAttribute(resOut)}/values/strings.xml" />

    <instantiate from="src/app_package/activity.java.ftl"
            to="${escapeXmlAttribute(srcOut)}/${activityClass}.java" />

    <open file="${escapeXmlAttribute(srcOut)}/${activityClass}.java" />
</recipe>
