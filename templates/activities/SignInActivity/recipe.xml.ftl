<?xml version="1.0"?>
<recipe>
    <dependency mavenUrl="com.google.android.gms:play-services:4.3.+" />
    <dependency mavenUrl="com.android.support:appcompat-v7:19.+" />

    <merge from="AndroidManifest.xml.ftl"
             to="${escapeXmlAttribute(manifestOut)}/AndroidManifest.xml" />

    <merge from="res/values/dimens.xml"
             to="${escapeXmlAttribute(resOut)}/values/dimens.xml" />

    <instantiate from="res/layout/activity_signin.xml.ftl"
                   to="${escapeXmlAttribute(resOut)}/layout/${layoutName}.xml" />

    <instantiate from="res/values/strings.xml.ftl"
                   to="${escapeXmlAttribute(resOut)}/values/strings_${simpleName}.xml" />

    <instantiate from="src/app_package/SignInActivity.java.ftl"
                   to="${escapeXmlAttribute(srcOut)}/${activityClass}.java" />

    <open file="${escapeXmlAttribute(srcOut)}/${activityClass}.java" />

</recipe>
