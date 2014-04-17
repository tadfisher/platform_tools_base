<?xml version="1.0"?>
<globals>
    <global id="useSupport" type="boolean" value="${(minApiLevel lt 11)?string}" />
    <global id="Support" value="${(minApiLevel lt 11)?string('Support','')}" />
    <global id="SupportPackage" value="${(minApiLevel lt 11)?string('.support.v4','')}" />
    <global id="resOut" value="${resDir}" />
    <global id="srcOut" value="${srcDir}/${slashedPackageName(packageName)}" />
    <global id="collection_name" value="${extractLetters(objectKind?lower_case)}" />
    <global id="className" value="${extractLetters(objectKind)}Fragment" />
    <global id="fragment_layout" value="fragment_${extractLetters(objectKind?lower_case)}" />
</globals>
