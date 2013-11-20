<?xml version="1.0"?>
<recipe>
    <dependency mavenUrl="com.google.android.gms:play-services:3.2.65" />
    <dependency mavenUrl="com.android.support:appcompat-v7:+" />

    <merge from="AndroidManifest.xml.ftl"
             to="${escapeXmlAttribute(manifestOut)}/AndroidManifest.xml" />

    <merge from="res/values/styles.xml"
             to="${escapeXmlAttribute(resOut)}/values/styles.xml" />
    <merge from="res/values/dimens.xml"
             to="${escapeXmlAttribute(resOut)}/values/dimens.xml" />
    <copy from="res/menu/activity_login.xml"
             to="${escapeXmlAttribute(resOut)}/menu/${menuName}.xml" />
    <copy from="res/layout/fragment_login.xml"
             to="${escapeXmlAttribute(resOut)}/layout/fragment_login.xml" />
    <instantiate from="res/layout/activity_login.xml.ftl"
                   to="${escapeXmlAttribute(resOut)}/layout/${layoutName}.xml" />

    <instantiate from="res/values/strings.xml.ftl"
                   to="${escapeXmlAttribute(resOut)}/values/strings_${simpleName}.xml" />

    <instantiate from="src/app_package/LoginActivity.java.ftl"
                   to="${escapeXmlAttribute(srcOut)}/${activityClass}.java" />

    <instantiate from="src/app_package/PlusBaseActivity.java.ftl"
                   to="${escapeXmlAttribute(srcOut)}/PlusBaseActivity.java" />

    <instantiate from="src/app_package/EmailLoginFragment.java.ftl"
                   to="${escapeXmlAttribute(srcOut)}/EmailLoginFragment.java" />

    <open file="${escapeXmlAttribute(srcOut)}/${activityClass}.java" />

</recipe>
