<?xml version="1.0"?>
<globals>
    <global id="useSupport" type="boolean" value="${(minApiLevel lt 11)?string}" />
    <global id="Support" value="${(minApiLevel lt 11)?string('Support','')}" />
    <global id="SupportPackage" value="${(minApiLevel lt 11)?string('.support.v4','')}" />
    <global id="resOut" value="${resDir}" />
    <global id="srcOut" value="${srcDir}/${slashedPackageName(packageName)}" />
</globals>
