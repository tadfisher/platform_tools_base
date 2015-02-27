<?xml version="1.0"?>
<recipe>
  <!-- Copy the Manifest, gradle and strings files -->
  <merge from="AndroidManifest.xml.ftl"
             to="${escapeXmlAttribute(manifestOut)}/AndroidManifest.xml" />

  <merge from="build.gradle.ftl"
             to="${escapeXmlAttribute(projectOut)}/build.gradle" />

  <!-- Create jni folder and copy the default files -->
  <mkdir at="${escapeXmlAttribute(manifestOut)}/jni/" />
  <copy from="jni/main.cpp"
            to="${escapeXmlAttribute(manifestOut)}/jni/main.cpp" />
  <copy from="jni/android_native_app_glue.c"
           to="${escapeXmlAttribute(manifestOut)}/jni/android_native_app_glue.c" />
  <copy from="jni/android_native_app_glue.h"
           to="${escapeXmlAttribute(manifestOut)}/jni/android_native_app_glue.h" />
  <copy from="jni/Application.h"
          to="${escapeXmlAttribute(manifestOut)}/jni/Application.h" />
  <copy from="jni/Application.cpp"
          to="${escapeXmlAttribute(manifestOut)}/jni/Application.cpp" />

  <!-- Open the C++ main file -->
  <open file="${escapeXmlAttribute(manifestOut)}/jni/main.cpp" />
</recipe>
