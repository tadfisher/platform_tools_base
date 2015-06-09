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
import java.util.Map;
import java.util.TreeMap;

/**
 */
public class GoogleServicesTask extends DefaultTask {

    private static final String STATUS_DISABLED = "1";
    private static final String STATUS_ENABLED = "2";

    private static final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources>\n";
    private static final String XML_FOOTER = "</resources>\n";

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

            // TODO: handleAppinvite has dependency on handleAnalytics because we also need to put
            // ga_trackingId into ai_config.xml. Remove this dependency if possible
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
     * @param resValues a Map that store key and value
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
     * @param resValues a Map that store key and value
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
     * Handle a client object for AdsService
     * @param clientObject the client Json object.
     * @param resValues a Map that store key and value
     * @throws IOException
     */
    private void handleAdsService(JsonObject clientObject, Map<String, String> resValues)
            throws IOException {
        JsonObject adsService = getServiceByName(clientObject, "ads_service");
        if (adsService == null) return;

        findStringByName(adsService, "test_banner_ad_unit_id", resValues);
        findStringByName(adsService, "test_interstitial_ad_unit_id", resValues);
    }

    /**
     * Handle a client object for Appinvite (@xml/ai_config)
     * Need to be called after handleAnalytics() as it requires "ga_trackingId" fetched in "analytics_service"
     *
     * @param clientObject the client Json object.
     * @param resValues a Map that store key and value
     * @throws IOException
     */
    private void handleAppinviteService(JsonObject clientObject, Map<String, String> resValues) throws IOException {
        JsonObject appinviteService = getServiceByName(clientObject, "appinvite_service");
        if (appinviteService == null) return;

        JsonArray otherPlatformOauthClientArray = appinviteService.getAsJsonArray("other_platform_oauth_client");

        if (otherPlatformOauthClientArray != null) {
            final int count = otherPlatformOauthClientArray.size();

            // Could be more than one oauth client, for now just use the first one we found
            // TODO: Find a better way to do this
            for (int i = 0; i < count; i++) {
                JsonElement otherPlatformOauthClient = otherPlatformOauthClientArray.get(i);
                if (otherPlatformOauthClient == null || !otherPlatformOauthClient.isJsonObject()) continue;

                JsonPrimitive id = otherPlatformOauthClient.getAsJsonObject().getAsJsonPrimitive("client_id");
                if (id == null) continue;

                File xml = new File(intermediateDir, "xml");
                if (!xml.exists() && !xml.mkdirs()) {
                    throw new GradleException("Failed to create folder: " + xml);
                }

                Files.write(getAppinviteContent(resValues, id.getAsString()),
                        new File(xml, "ai_config.xml"),
                        Charsets.UTF_8);

                // Skip the rest of other platform oauth client
                return;
            }
        }
        getLogger().warn("Appinvite is enabled but no other platform oauth client is found.");
    }

    /**
     * Find stringName from jsonObject and add the key, value pair to the resValues
     * @param jsonObject the JsonObject to be inspected.
     * @param stringName the name that we want to find
     * @param resValues a Map that store key and value
     */
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

    /**
     * Generate the XML content for Analytics
     * @param ga_trackingId Global Tracker ID
     * @return a String that contain XML formatted information
     */
    private static String getGlobalTrackerContent(String ga_trackingId) {
        return XML_HEADER +
                "    <string name=\"ga_trackingId\">" + ga_trackingId + "</string>\n" +
                XML_FOOTER;
    }

    /**
     * Generate the XML content for (key, value) in entries
     * @param entries the map that store all (key, value)
     * @return a String that contain XML formatted information
     */
    private static String getValuesContent(Map<String, String> entries) {
        StringBuilder sb = new StringBuilder(256);

        sb.append(XML_HEADER);

        for (Map.Entry<String, String> entry : entries.entrySet()) {
            sb.append("    <string name=\"").append(entry.getKey()).append("\">")
                    .append(entry.getValue()).append("</string>\n");
        }

        sb.append(XML_FOOTER);

        return sb.toString();
    }

    /**
     * Generate the XML content for Appinvite Service
     * @param entries the map that store all (key, value)
     * @param clientID the client ID for iOS platform
     * @return a String that contain XML formatted information
     */
    private static String getAppinviteContent(Map<String, String> entries, String clientID) {
        StringBuilder sb = new StringBuilder(256);

        sb.append(XML_HEADER);

        if (entries.containsKey("ga_trackingId")) {
            sb.append("    <string name=\"ga_trackingId\">" + entries.get("ga_trackingId") + "</string>\n");
        }

        sb.append("    <string name=\"ai_ios_target_application\">" + clientID + "</string>\n").append(XML_FOOTER);

        return sb.toString();
    }

    /**
     * Delete Folder
     * @param folder the folder to be deleted
     */
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
