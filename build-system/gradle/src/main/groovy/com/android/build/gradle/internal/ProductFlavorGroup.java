/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.build.gradle.internal;

import com.android.annotations.Nullable;
import com.android.build.gradle.api.GroupableProductFlavor;
import com.android.builder.core.DefaultProductFlavor;
import com.android.builder.model.ProductFlavor;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;

import java.util.List;

/**
 * A group of product flavors each belonging to a different flavor dimension.
 */
public class ProductFlavorGroup {
    private String name;
    private List<GroupableProductFlavor> flavorList;

    /**
     * Create a ProductFlavorGroup.
     * @param flavors Lists of ProductFlavor.  null are ignored.
     */
    public ProductFlavorGroup(GroupableProductFlavor... flavors) {
        ImmutableList.Builder<GroupableProductFlavor> builder = ImmutableList.builder();
        for (GroupableProductFlavor flavor : flavors) {
            if (flavor != null) {
                builder.add(flavor);
            }
        }
        flavorList = builder.build();
    }

    public ProductFlavorGroup(List<GroupableProductFlavor> flavors) {
        flavorList = ImmutableList.copyOf(flavors);
    }

    public String getName() {
        if (name == null) {
            boolean first = true;
            StringBuilder sb = new StringBuilder();
            for (ProductFlavor flavor : flavorList) {
                if (first) {
                    sb.append(flavor.getName());
                    first = false;
                } else {
                    sb.append(StringHelper.capitalize(flavor.getName()));
                }
            }
            name = sb.toString();
        }
        return name;
    }

    public List<GroupableProductFlavor> getFlavorList() {
        return flavorList;
    }

    /**
     * Creates a list containing all combinations of ProductFlavors of the given dimensions.
     * @param flavorDimensions The dimensions each product flavor can belong to.
     * @param productFlavors An iterable of all ProductFlavors in the project..
     * @return A list of ProductFlavorGroup representing all combinations of ProductFlavors.
     */
    public static List<ProductFlavorGroup> createGroupList(
            @Nullable List<String> flavorDimensions,
            Iterable<? extends GroupableProductFlavor> productFlavors) {

        List <ProductFlavorGroup> result = Lists.newArrayList();
        if (flavorDimensions == null || flavorDimensions.size() <= 0) {
            for (GroupableProductFlavor flavor : productFlavors) {
                result.add(new ProductFlavorGroup(flavor));
            }
        } else {
            // need to group the flavor per dimension.
            // First a map of dimension -> list(ProductFlavor)
            ArrayListMultimap<String, GroupableProductFlavor> map = ArrayListMultimap.create();
            for (GroupableProductFlavor flavor : productFlavors) {
                String flavorDimension = flavor.getFlavorDimension();

                if (flavorDimension == null) {
                    throw new RuntimeException(String.format(
                            "Flavor '%1$s' has no flavor dimension.", flavor.getName()));
                }
                if (!flavorDimensions.contains(flavorDimension)) {
                    throw new RuntimeException(String.format(
                            "Flavor '%1$s' has unknown dimension '%2$s'.",
                            flavor.getName(), flavor.getFlavorDimension()));
                }

                map.put(flavorDimension, flavor);
            }

            createProductFlavorGroups(result, new GroupableProductFlavor[flavorDimensions.size()],
                    0, flavorDimensions, map);
        }
        return result;
    }

    private static void createProductFlavorGroups(
            List<ProductFlavorGroup> flavorGroups,
            GroupableProductFlavor[] group,
            int index,
            List<String> flavorDimensionList,
            ListMultimap<String, GroupableProductFlavor> map) {
        if (index == flavorDimensionList.size()) {
            flavorGroups.add(new ProductFlavorGroup(group));
            return;
        }

        // fill the array at the current index.
        // get the dimension name that matches the index we are filling.
        String dimension = flavorDimensionList.get(index);

        // from our map, get all the possible flavors in that dimension.
        List<GroupableProductFlavor> flavorList = map.get(dimension);

        // loop on all the flavors to add them to the current index and recursively fill the next
        // indices.
        if (flavorList.isEmpty()) {
            createProductFlavorGroups(flavorGroups, group, index + 1, flavorDimensionList, map);
        } else {
            for (GroupableProductFlavor flavor : flavorList) {
                group[index] = flavor;
                createProductFlavorGroups(flavorGroups, group, index + 1, flavorDimensionList, map);
            }
        }
    }
}
