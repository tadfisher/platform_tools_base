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

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.annotations.VisibleForTesting;
import com.android.ide.common.rendering.api.ResourceValue;
import com.android.ide.common.resources.ResourceFile;
import com.android.ide.common.resources.configuration.FolderConfiguration;
import com.android.resources.ResourceType;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 */
public class ResourceRepository {

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

    private final Map<ResourceType, Multimap<String, ResourceItem>> mItems = Maps.newEnumMap(
            ResourceType.class);
    private final RepositoryMerger mConsumer = new RepositoryMerger();


    @NonNull
    MergeConsumer<ResourceItem> getMergeConsumer() {
        return mConsumer;
    }

    @NonNull
    @VisibleForTesting
    Map<ResourceType, Multimap<String, ResourceItem>> getItems() {
        return mItems;
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
     * Returns the {@link com.android.ide.common.resources.ResourceFile} matching the given name,
     * {@link ResourceType} and configuration.
     * <p/>
     * This only works with files generating one resource named after the file
     * (for instance, layouts, bitmap based drawable, xml, anims).
     *
     * @param name the resource name or file name
     * @param type the folder type search for
     * @param config the folder configuration to match for
     * @return the matching file or <code>null</code> if no match was found.
     */
    @Nullable
    public ResourceFile getMatchingFile(
            @NonNull String name,
            @NonNull ResourceType type,
            @NonNull FolderConfiguration config) {
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
        return Collections.emptyMap();
    }

    private void addItem(@NonNull ResourceItem item) {
        synchronized (mItems) {
            Multimap<String, ResourceItem> map = mItems.get(item.getType());
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
