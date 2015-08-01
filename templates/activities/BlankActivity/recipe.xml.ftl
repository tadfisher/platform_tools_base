<?xml version="1.0"?>
<recipe>
    <merge from="AndroidManifest.xml.ftl"
             to="${escapeXmlAttribute(manifestOut)}/AndroidManifest.xml" />

    <instantiate from="res/menu/main.xml.ftl"
            to="${escapeXmlAttribute(resOut)}/menu/${menuName}.xml" />

    <merge from="res/values/strings.xml.ftl"
             to="${escapeXmlAttribute(resOut)}/values/strings.xml" />

    <merge from="res/values/dimens.xml.ftl"
             to="${escapeXmlAttribute(resOut)}/values/dimens.xml" />
    <merge from="res/values-w820dp/dimens.xml"
             to="${escapeXmlAttribute(resOut)}/values-w820dp/dimens.xml" />

    <instantiate from="res/layout/activity_simple.xml.ftl"
                   to="${escapeXmlAttribute(resOut)}/layout/${layoutName}.xml" />

    <instantiate from="src/app_package/SimpleActivity.java.ftl"
                   to="${escapeXmlAttribute(srcOut)}/${activityClass}.java" />

<#if hasAppBar && appCompatActivity>
    <instantiate from="res/layout/app_bar.xml.ftl"
                   to="${escapeXmlAttribute(resOut)}/layout/${layoutName}_app_bar.xml" />

    <dependency mavenUrl="com.android.support:design:${targetApi}.+"/>
    <dependency mavenUrl="com.android.support:appcompat-v7:${targetApi}.+"/>
</#if>

    <open file="${escapeXmlAttribute(srcOut)}/${activityClass}.java" />
    <open file="${escapeXmlAttribute(resOut)}/layout/${layoutName}.xml" />
</recipe>
