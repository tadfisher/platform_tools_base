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

package com.android.ide.common.res2;

import static com.android.SdkConstants.DOT_9PNG;
import static com.android.SdkConstants.DOT_XML;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.io.IAbstractFile;
import com.android.resources.ResourceType;
import com.android.utils.SdkUtils;

import java.io.File;

import javax.lang.model.SourceVersion;

public class ResourceNameValidator {


    private ResourceNameValidator() {
    }


    public static void validate(@NonNull File file, @NonNull ResourceType resourceType)
            throws MergingException {
        //boolean allowXmlExtension = resourceType != ResourceType.RAW;
        //boolean isImageType = resourceType == ResourceType.DRAWABLE;

        String error = getErrorTextForFileResource(file.getName(), resourceType);
        if (error != null) {
            throw new MergingException(error).setFile(file);
        }
    }

    public static void validate(@NonNull String inputString, @NonNull ResourceType type,
            @Nullable File file)
            throws MergingException {
        //boolean isImageType = type == ResourceType.DRAWABLE;
        String error = getErrorTextForValueResource(inputString, type);
        if (error != null) {
            // TODO find location in file.
            if (file != null) {
                throw new MergingException(error).setFile(file);
            } else {
                throw new MergingException(error);
            }
        }
    }

    /**
     * Returns the name of the resources.
     */
    public static String getResourceName(@NonNull IAbstractFile file, @NonNull ResourceType type) {
        // get the name from the filename.
        String name = file.getName();

        int pos = name.indexOf('.');
        if (pos != -1) {
            name = name.substring(0, pos);
        }

        return name;
    }
/*

         param allowXmlOrImageExtension If true, allow
     *                 .xml as a name suffix. param isFileType               True if the resource
     *                 name being considered is a "file" based resource (where the resource name is
     *                 the actual file name, rather than just a value attribute inside an XML file
     *                 name of arbitrary name) param allowImageType           True if the resource
     *                 type can point to image resources. param allowAndroidNamespace    True if the
     *                 resource name can start with "android:"

 */

    /**
     * Validate a single-file resource name.
     *
     * @param fileNameWithExt The resource file name to validate.
     * @param resourceType The resource type.
     * @return null if no error, otherwise a string describing the error.
     */
    @Nullable
    public static String getErrorTextForFileResource(@NonNull final String fileNameWithExt,
            @NonNull final ResourceType resourceType) {

        if (fileNameWithExt.trim().isEmpty()) {
            return "Resource must have a name";
        }

        final String fileName;

        if (resourceType == ResourceType.RAW) {
            // Allow any single file extension.
            int lastDot = fileNameWithExt.lastIndexOf('.');
            if (lastDot != -1) {
                fileName = fileNameWithExt.substring(0, lastDot);
            } else {
                fileName = fileNameWithExt;
            }
        } else if (resourceType == ResourceType.DRAWABLE | resourceType == ResourceType.MIPMAP) {
            // Require either an image or xml file extension
            if (fileNameWithExt.endsWith(DOT_XML)) {
                fileName = fileNameWithExt
                        .substring(0, fileNameWithExt.length() - DOT_XML.length());
            } else if (SdkUtils.hasImageExtension(fileNameWithExt)) {
                if (fileNameWithExt.endsWith(DOT_9PNG)) {
                    fileName = fileNameWithExt
                            .substring(0, fileNameWithExt.length() - DOT_9PNG.length());
                } else {
                    fileName = fileNameWithExt.substring(0, fileNameWithExt.lastIndexOf('.'));
                }
            } else {
                return "The file name must end with .xml or .png";
            }
        } else {
            // Require xml extension
            if (fileNameWithExt.endsWith(DOT_XML)) {
                fileName = fileNameWithExt
                        .substring(0, fileNameWithExt.length() - DOT_XML.length());
            } else {
                return "The file name must end with .xml";
            }
        }
        char first = fileName.charAt(0);
        if (!(first >= 'a' && first <= 'z')) {
            return "File-based resource names must start with a lowercase letter";
        }

        // AAPT only allows lowercase+digits+_:
        // "%s: Invalid file name: must contain only [a-z0-9_.]","
        for (int i = 0, n = fileName.length(); i < n; i++) {
            char c = fileName.charAt(i);
            if (!((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_')) {
                return String
                  .format("'%1$c' is not a valid file-based resource name character: " +
                          "File-based resource names must contain only lowercase a-z, 0-9, or underscore", c);
            }
        }
        if (SourceVersion.isKeyword(fileName)) {
            return String.format("%1$s is not a valid name (reserved Java keyword)", fileName);
        }

        // Success!
        return null;

    }

    /**
     * Validate a value resource name.
     *
     * @param fullResourceName The resource name to validate.
     * @param resourceType The resource type.
     * @return null if no error, otherwise a string describing the error.
     */
    @Nullable
    public static String getErrorTextForValueResource(@NonNull String fullResourceName,
      ResourceType resourceType) {

        if (resourceType == ResourceType.ATTR) {
            if (fullResourceName.startsWith("android:")) {
                fullResourceName = fullResourceName.substring(8);
            }
        }
        final String resourceName = fullResourceName.replace('.', '_');

        // Resource names must be valid Java identifiers, since they will
        // be represented as Java identifiers in the R file:
        if (!SourceVersion.isIdentifier(resourceName)) {
            if (!Character.isJavaIdentifierStart(resourceName.charAt(0))) {
                return "The resource name must start with a letter";
            } else {
                for (int i = 1, n = resourceName.length(); i < n; i++) {
                    char c = resourceName.charAt(i);
                    if (!Character.isJavaIdentifierPart(c)) {
                        return String
                                .format("'%1$c' is not a valid resource name character", c);
                    }
                }
            }
        }

        if (SourceVersion.isKeyword(resourceName)) {
            return String.format("%1$s is not a valid name (reserved Java keyword)", resourceName);
        }

        // Success.
        return null;
    }
}
