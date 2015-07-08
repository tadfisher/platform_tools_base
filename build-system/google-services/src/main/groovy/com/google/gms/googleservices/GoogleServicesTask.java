/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.gms.googleservices;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 */
public class GoogleServicesTask extends DefaultTask {

    private static final String STATUS_DISABLED = "1";
    private static final String STATUS_ENABLED = "2";

    private static final String XML_HEADER =
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources>\n";
    private static final String XML_FOOTER = "</resources>\n";

    private static final String OAUTH_CLIENT_TYPE_ANDROID = "1";
    private static final String OAUTH_CLIENT_TYPE_IOS = "2";

    private static final String APPINVITE_ANDROID_TARGET_APPLICATION =
            "ai_android_target_application";
    private static final String APPINVITE_IOS_TARGET_APPLICATION = "ai_ios_target_application";

    /**
     * The input is not technically optional but we want to control the error message.
     * Without @Optional, Gradle will complain itself the file is missing.
     */
    @InputFile @Optional
    public File quickstartFile;

    @OutputDirectory
    public File intermediateDir;

    @Input
    public String packageName;

    @TaskAction
    public void action() throws IOException {
        if (!quickstartFile.isFile()) {
            getLogger().warn("File " + quickstartFile.getName() + " is missing from module root folder." +
                    " The Google Quickstart Plugin cannot function without it.");

            // Skip the rest of the actions because it would not make sense if `quickstartFile` is missing.
            return;
        }

        // delete content of outputdir.
        deleteFolder(intermediateDir);
        if (!intermediateDir.mkdirs()) {
            throw new GradleException("Failed to create folder: " + intermediateDir);
        }

        JsonElement root = new JsonParser().parse(Files.newReader(quickstartFile, Charsets.UTF_8));

        if (!root.isJsonObject()) {
            throw new GradleException("Malformed root json");
        }

        JsonObject rootObject = root.getAsJsonObject();

        Map<String, String> resValues = new TreeMap<String, String>();

        handleProjectNumber(rootObject, resValues);

        JsonObject clientObject = getClientForPackageName(rootObject);

        if (clientObject != null) {
            handleAnalytics(clientObject, resValues);
            handleAdsService(clientObject, resValues);
            handleAppinviteService(clientObject, resValues);
        } else {
            getLogger().warn("No matching client found for package name '" + packageName + "'");
        }

        // write the values file.
        File values = new File(intermediateDir, "values");
        if (!values.exists() && !values.mkdirs()) {
            throw new GradleException("Failed to create folder: " + values);
        }

        Files.write(getValuesContent(resValues), new File(values, "values.xml"), Charsets.UTF_8);
    }

    /**
     * Handle project_info/project_number for @string/gcm_defaultSenderId, and fill the res map with the read value.
     * @param rootObject the root Json object.
     * @throws IOException
     */
    private void handleProjectNumber(JsonObject rootObject, Map<String, String> resValues)
            throws IOException {
        JsonObject projectInfo = rootObject.getAsJsonObject("project_info");
        if (projectInfo == null) {
            throw new GradleException("Missing project_info object");
        }

        JsonPrimitive projectNumber = projectInfo.getAsJsonPrimitive("project_number");
        if (projectNumber == null) {
            throw new GradleException("Missing project_info/project_number object");
        }

        resValues.put("gcm_defaultSenderId", projectNumber.getAsString());
    }

    /**
     * Handle a client object for analytics (@xml/global_tracker)
     * @param clientObject the client Json object.
     * @throws IOException
     */
    private void handleAnalytics(JsonObject clientObject, Map<String, String> resValues)
            throws IOException {
        JsonObject analyticsService = getServiceByName(clientObject, "analytics_service");
        if (analyticsService == null) return;

        JsonObject analyticsProp = analyticsService.getAsJsonObject("analytics_property");
        if (analyticsProp == null) return;

        JsonPrimitive trackingId = analyticsProp.getAsJsonPrimitive("tracking_id");
        if (trackingId == null) return;

        resValues.put("ga_trackingId", trackingId.getAsString());

        File xml = new File(intermediateDir, "xml");
        if (!xml.exists() && !xml.mkdirs()) {
            throw new GradleException("Failed to create folder: " + xml);
        }

        Files.write(getGlobalTrackerContent(
                trackingId.getAsString()),
                new File(xml, "global_tracker.xml"),
                Charsets.UTF_8);
    }

    /**
     * Handle a client object for Ads Service
     * @param clientObject the client Json object.
     * @throws IOException
     */
    private void handleAdsService(JsonObject clientObject, Map<String, String> resValues)
            throws IOException {
        JsonObject adsService = getServiceByName(clientObject, "ads_service");
        if (adsService == null) return;

        findStringByName(adsService, "test_banner_ad_unit_id", resValues);
        findStringByName(adsService, "test_interstitial_ad_unit_id", resValues);
    }

    private void addAndroidOauthClientForAppinviteService(
            JsonObject clientObject, Map<String, String> resValues) {
        JsonArray oauthClientArray = clientObject.getAsJsonArray("oauth_client");
        if (oauthClientArray == null || oauthClientArray.size() == 0) {
            return;
        }

        final int count = oauthClientArray.size();

        for (int i = 0; i < count; i++) {
            JsonElement oauthClientElement = oauthClientArray.get(i);
            if (oauthClientElement == null || !oauthClientElement.isJsonObject()) {
                continue;
            }

            JsonObject oauthClientObject = oauthClientElement.getAsJsonObject();

            JsonPrimitive clientId = oauthClientObject.getAsJsonPrimitive("client_id");
            if (clientId == null) continue;

            resValues.put(APPINVITE_ANDROID_TARGET_APPLICATION, clientId.getAsString());
            return;
        }
    }

    private void handleAppinviteService(JsonObject clientObject, Map<String, String> resValues)
            throws IOException {
        JsonObject appinviteService = getServiceByName(clientObject, "appinvite_service");
        if (appinviteService == null) return;

        addAndroidOauthClientForAppinviteService(clientObject, resValues);

        JsonArray otherPlatformOauthClientArray =
                appinviteService.getAsJsonArray("other_platform_oauth_client");

        if (otherPlatformOauthClientArray == null || otherPlatformOauthClientArray.size() == 0) {
            getLogger().warn(
                    "Appinvite Service is enabled but no other platform oauth client is found.");
        } else {
            final int count = otherPlatformOauthClientArray.size();

            for (int i = 0; i < count; i++) {
                JsonElement otherPlatformOauthClientElement = otherPlatformOauthClientArray.get(i);

                if (otherPlatformOauthClientElement == null ||
                        !otherPlatformOauthClientElement.isJsonObject()) continue;

                JsonObject otherPlatformOauthClientObject = otherPlatformOauthClientElement
                        .getAsJsonObject();

                JsonPrimitive clientType =
                        otherPlatformOauthClientObject.getAsJsonPrimitive("client_type");
                if (clientType == null) continue;
                String clientTypeStr = clientType.getAsString();

                JsonPrimitive clientId = otherPlatformOauthClientObject
                        .getAsJsonPrimitive("client_id");
                if (clientId == null) continue;
                String clientIdStr = clientId.getAsString();

                String key = null;
                if (OAUTH_CLIENT_TYPE_IOS.equals(clientTypeStr)) {
                    key = APPINVITE_IOS_TARGET_APPLICATION;
                } else {
                    continue;
                }

                resValues.put(key, clientIdStr);
            }
        }

        File xml = new File(intermediateDir, "xml");
        if (!xml.exists() && !xml.mkdirs()) {
            throw new GradleException("Failed to create folder: " + xml);
        }

        Files.write(getAppinviteContent(resValues),
                new File(xml, "ai_config.xml"),
                Charsets.UTF_8);
    }

    private static void findStringByName(JsonObject jsonObject, String stringName,
            Map<String, String> resValues) {
        JsonPrimitive id = jsonObject.getAsJsonPrimitive(stringName);
        if (id != null) {
            resValues.put(stringName, id.getAsString());
        }
    }

    /**
     * find an item in the "client" array that match the package name of the app
     * @param jsonObject the root json object.
     * @return a JsonObject representing the client entry or null if no match is found.
     */
    private JsonObject getClientForPackageName(JsonObject jsonObject) {
        JsonArray array = jsonObject.getAsJsonArray("client");
        if (array != null) {
            final int count = array.size();
            for (int i = 0 ; i < count ; i++) {
                JsonElement clientElement = array.get(i);
                if (clientElement == null || !clientElement.isJsonObject()) {
                    continue;
                }

                JsonObject clientObject = clientElement.getAsJsonObject();

                JsonObject clientInfo = clientObject.getAsJsonObject("client_info");
                if (clientInfo == null) continue;

                JsonObject androidClientInfo = clientInfo.getAsJsonObject("android_client_info");
                if (androidClientInfo == null) continue;

                JsonPrimitive clientPackageName = androidClientInfo.getAsJsonPrimitive("package_name");
                if (clientPackageName == null) continue;

                if (packageName.equals(clientPackageName.getAsString())) {
                    return clientObject;
                }
            }
        }

        return null;
    }

    /**
     * Finds a service by name in the client object. Returns null if the service is not found
     * or if the service is disabled.
     *
     * @param clientObject the json object that represents the client.
     * @param serviceName the service name
     * @return the service if found.
     */
    private JsonObject getServiceByName(JsonObject clientObject, String serviceName) {
        JsonObject services = clientObject.getAsJsonObject("services");
        if (services == null) return null;

        JsonObject service = services.getAsJsonObject(serviceName);
        if (service == null) return null;

        JsonPrimitive status = service.getAsJsonPrimitive("status");
        if (status == null) return null;

        String statusStr = status.getAsString();

        if (STATUS_DISABLED.equals(statusStr)) return null;
        if (!STATUS_ENABLED.equals(statusStr)) {
            getLogger().warn(String.format("Status with value '%1$s' for service '%2$s' is unknown",
                    statusStr,
                    serviceName));
            return null;
        }

        return service;
    }

    private static String getStringEntry(String name, String value) {
        return "    <string name=\"" + name + "\">" + value + "</string>\n";
    }

    private static String getGlobalTrackerContent(String ga_trackingId) {
        return XML_HEADER + getStringEntry("ga_trackingId", ga_trackingId) + XML_FOOTER;
    }

    private static String getValuesContent(Map<String, String> entries) {
        StringBuilder sb = new StringBuilder(256);

        sb.append(XML_HEADER);

        for (Map.Entry<String, String> entry : entries.entrySet()) {
            sb.append(getStringEntry(entry.getKey(), entry.getValue()));
        }

        sb.append(XML_FOOTER);

        return sb.toString();
    }

    private static String getAppinviteContent(Map<String, String> entries) {
        StringBuilder sb = new StringBuilder(256);

        sb.append(XML_HEADER);

        List<String> keys = Arrays.asList("ga_trackingId",
                APPINVITE_IOS_TARGET_APPLICATION, APPINVITE_ANDROID_TARGET_APPLICATION);

        for (String key : keys) {
            if (entries.containsKey(key)) {
                sb.append(getStringEntry(key, entries.get(key)));
            }
        }

        sb.append(XML_FOOTER);
        return sb.toString();
    }

    private static void deleteFolder(final File folder) {
        if (!folder.exists()) {
            return;
        }
        File[] files = folder.listFiles();
        if (files != null) {
            for (final File file : files) {
                if (file.isDirectory()) {
                    deleteFolder(file);
                } else {
                    if (!file.delete()) {
                        throw new GradleException("Failed to delete: " + file);
                    }
                }
            }
        }
        if (!folder.delete()) {
            throw new GradleException("Failed to delete: " + folder);
        }
    }
}
