/*
 * Copyright (C) 2013 The Android Open Source Project
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

import static com.android.SdkConstants.ATTR_REF_PREFIX;
import static com.android.SdkConstants.PREFIX_RESOURCE_REF;
import static com.android.SdkConstants.PREFIX_THEME_REF;
import static com.android.SdkConstants.RESOURCE_CLZ_ATTR;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.annotations.VisibleForTesting;
import com.android.ide.common.rendering.api.ResourceValue;
import com.android.ide.common.resources.configuration.FolderConfiguration;
import com.android.resources.ResourceType;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 */
public class ResourceRepository {

    private final boolean mFrameworks;

    private class RepositoryMerger implements MergeConsumer<ResourceItem> {

        @Override
        public void start() throws ConsumerException {
        }

        @Override
        public void end() throws ConsumerException {
        }

        @Override
        public void addItem(@NonNull ResourceItem item) throws ConsumerException {
            if (item.isTouched()) {
                ResourceRepository.this.addItem(item);
            }
        }

        @Override
        public void removeItem(@NonNull ResourceItem removedItem, ResourceItem replacedBy)
                throws ConsumerException {
            ResourceRepository.this.removeItem(removedItem);
        }
    }

    private final Map<ResourceType, ListMultimap<String, ResourceItem>> mItems = Maps.newEnumMap(
            ResourceType.class);
    private final RepositoryMerger mConsumer = new RepositoryMerger();


    public ResourceRepository(boolean isFrameworks) {
        mFrameworks = isFrameworks;
    }

    @NonNull
    MergeConsumer<ResourceItem> getMergeConsumer() {
        return mConsumer;
    }

    @NonNull
    @VisibleForTesting
    Map<ResourceType, ListMultimap<String, ResourceItem>> getItems() {
        return mItems;
    }

    @Nullable
    public List<ResourceItem> getResourceItem(@NonNull ResourceType resourceType,
            @NonNull String resourceName) {
        ListMultimap<String, ResourceItem> map = mItems.get(resourceType);

        return map.get(resourceName);
    }

    @NonNull
    public Collection<String> getItemsOfType(@NonNull ResourceType type) {
        synchronized (mItems) {
            Multimap<String, ResourceItem> map = mItems.get(type);
            if (map == null) {
                return Collections.emptyList();
            }
            return Collections.unmodifiableCollection(map.keySet());
        }
    }

    /**
     * Returns true if this resource repository contains a resource of the given
     * name.
     *
     * @param url the resource URL
     * @return true if the resource is known
     */
    public boolean hasResourceItem(@NonNull String url) {
        // Handle theme references
        if (url.startsWith(PREFIX_THEME_REF)) {
            String remainder = url.substring(PREFIX_THEME_REF.length());
            if (url.startsWith(ATTR_REF_PREFIX)) {
                url = PREFIX_RESOURCE_REF + url.substring(PREFIX_THEME_REF.length());
                return hasResourceItem(url);
            }
            int colon = url.indexOf(':');
            if (colon != -1) {
                // Convert from ?android:progressBarStyleBig to ?android:attr/progressBarStyleBig
                if (remainder.indexOf('/', colon) == -1) {
                    remainder = remainder.substring(0, colon) + RESOURCE_CLZ_ATTR + '/'
                            + remainder.substring(colon);
                }
                url = PREFIX_RESOURCE_REF + remainder;
                return hasResourceItem(url);
            } else {
                int slash = url.indexOf('/');
                if (slash == -1) {
                    url = PREFIX_RESOURCE_REF + RESOURCE_CLZ_ATTR + '/' + remainder;
                    return hasResourceItem(url);
                }
            }
        }

        if (!url.startsWith(PREFIX_RESOURCE_REF)) {
            return false;
        }

        assert url.startsWith("@") || url.startsWith("?") : url;

        int typeEnd = url.indexOf('/', 1);
        if (typeEnd != -1) {
            int nameBegin = typeEnd + 1;

            // Skip @ and @+
            int typeBegin = url.startsWith("@+") ? 2 : 1; //$NON-NLS-1$

            int colon = url.lastIndexOf(':', typeEnd);
            if (colon != -1) {
                typeBegin = colon + 1;
            }
            String typeName = url.substring(typeBegin, typeEnd);
            ResourceType type = ResourceType.getEnum(typeName);
            if (type != null) {
                String name = url.substring(nameBegin);
                return hasResourceItem(type, name);
            }
        }

        return false;
    }

    /**
     * Returns true if this resource repository contains a resource of the given
     * name.
     *
     * @param resourceType the type of resource to look up
     * @param resourceName the name of the resource
     * @return true if the resource is known
     */
    public boolean hasResourceItem(@NonNull ResourceType resourceType, @NonNull String resourceName) {
        ListMultimap<String, ResourceItem> map = mItems.get(resourceType);

        if (map != null) {
            List<ResourceItem> itemList = map.get(resourceName);
            return itemList != null && !itemList.isEmpty();
        }

        return false;
    }

    /**
     * Returns whether the repository has resources of a given {@link ResourceType}.
     * @param resourceType the type of resource to check.
     * @return true if the repository contains resources of the given type, false otherwise.
     */
    public boolean hasResourcesOfType(@NonNull ResourceType resourceType) {
        ListMultimap<String, ResourceItem> map = mItems.get(resourceType);
        return map != null && !map.isEmpty();
    }

    @NonNull
    public List<ResourceType> getAvailableResourceTypes() {
        return Lists.newArrayList(mItems.keySet());
    }

    /**
     * Returns the {@link com.android.ide.common.resources.ResourceFile} matching the given name,
     * {@link ResourceType} and configuration.
     * <p/>
     * This only works with files generating one resource named after the file
     * (for instance, layouts, bitmap based drawable, xml, anims).
     *
     * @param name the resource name
     * @param type the folder type search for
     * @param config the folder configuration to match for
     * @return the matching file or <code>null</code> if no match was found.
     */
    @Nullable
    public ResourceFile getMatchingFile(
            @NonNull String name,
            @NonNull ResourceType type,
            @NonNull FolderConfiguration config) {

        ListMultimap<String, ResourceItem> typeItems = mItems.get(type);
        if (typeItems == null) {
            return null;
        }

        List<ResourceItem> matchingItems = typeItems.get(name);
        if (matchingItems == null || matchingItems.isEmpty()) {
            return null;
        }

        ResourceItem match = (ResourceItem) config.findMatchingConfigurable(matchingItems);

        if (match != null) {
            return match.getSource();
        }

        return null;
    }

    /**
     * Returns the resources values matching a given {@link FolderConfiguration}.
     *
     * @param referenceConfig the configuration that each value must match.
     * @return a map with guaranteed to contain an entry for each {@link ResourceType}
     */
    @NonNull
    public Map<ResourceType, Map<String, ResourceValue>> getConfiguredResources(
            @NonNull FolderConfiguration referenceConfig) {
        Map<ResourceType, Map<String, ResourceValue>> map = Maps.newEnumMap(ResourceType.class);

        for (ResourceType key : ResourceType.values()) {
            // get the local results and put them in the map
            map.put(key, getConfiguredResource(key, referenceConfig));
        }

        return map;
    }

    /**
     * Returns a map of (resource name, resource value) for the given {@link ResourceType}.
     * <p/>The values returned are taken from the resource files best matching a given
     * {@link FolderConfiguration}.
     * @param type the type of the resources.
     * @param referenceConfig the configuration to best match.
     */
    @NonNull
    private Map<String, ResourceValue> getConfiguredResource(
            @NonNull ResourceType type,
            @NonNull FolderConfiguration referenceConfig) {

        // get the resource item for the given type
        ListMultimap<String, ResourceItem> items = mItems.get(type);
        if (items == null) {
            return Maps.newHashMap();
        }

        Set<String> keys = items.keySet();

        // create the map
        Map<String, ResourceValue> map = Maps.newHashMapWithExpectedSize(keys.size());

        for (String key : keys) {
            List<ResourceItem> keyItems = items.get(key);

            // look for the best match for the given configuration
            // the match has to be of type ResourceFile since that's what the input list contains
            ResourceItem match = (ResourceItem) referenceConfig.findMatchingConfigurable(keyItems);

            ResourceValue value = match.getResourceValue(mFrameworks);
            if (value != null) {
                map.put(match.getName(), value);
            }
        }

        return map;
    }

    private void addItem(@NonNull ResourceItem item) {
        synchronized (mItems) {
            ListMultimap<String, ResourceItem> map = mItems.get(item.getType());
            if (map == null) {
                map = ArrayListMultimap.create();
                mItems.put(item.getType(), map);
            }

            if (!map.containsValue(item)) {
                map.put(item.getName(), item);
            }
        }
    }

    private void removeItem(@NonNull ResourceItem removedItem) {
        synchronized (mItems) {
            Multimap<String, ResourceItem> map = mItems.get(removedItem.getType());
            if (map != null) {
                map.remove(removedItem.getName(), removedItem);
            }
        }
    }
}
