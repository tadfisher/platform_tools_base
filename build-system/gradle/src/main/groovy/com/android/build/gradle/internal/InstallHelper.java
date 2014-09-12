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

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.build.gradle.internal.variant.FilteredOutput;
import com.android.resources.Density;

import java.util.List;

/**
 * Helper class to help with installation of multi-output variants.
 */
public class InstallHelper {

    /**
     * Returns which output to use based on given densities and abis.
     * @param outputs the outputs to choose from
     * @param density the density
     * @param abis a list of ABIs in descending priority order.
     * @return the output to use or null if none are compatible.
     */
    @Nullable
    public static FilteredOutput getOutput(
            @NonNull List<? extends FilteredOutput> outputs,
            int density,
            @NonNull String... abis) {
        Density densityEnum = Density.getEnum(density);
        if (densityEnum == null) {
            throw new RuntimeException("Unsupported device density: " + density);
        }
        String densityValue = densityEnum.getResourceValue();

        // a full match, both in density and abi
        FilteredOutput fullMatch = null;
        // the abi level at which it's a match, in order to find the best ABI for it.
        int fullMatchAbiLevel = Integer.MAX_VALUE;

        // a full universal match, ie that output has no filter.
        FilteredOutput universalMatch = null;

        // an ABI match, with universal Density support.
        FilteredOutput universalDensityMatch = null;
        // its ABI level for best ABI support.
        int densityMatchAbiLevel = Integer.MAX_VALUE;

        // a density match with universal ABI support.
        FilteredOutput universalAbiMatch = null;

        // find a matching output.
        for (FilteredOutput output : outputs) {
            String densityFilter = output.getDensityFilter();
            String abiFilter = output.getAbiFilter();

            boolean isUniversalDensityMatch = false;

            if (densityFilter != null) {
                if (!densityFilter.equals(densityValue)) {
                    continue;
                }
            } else {
                isUniversalDensityMatch = true;
            }

            // at this point it's a match, though it could be universal match.
            if (abiFilter != null) {
                // search for a matching abi
                int levelMatch = Integer.MAX_VALUE;
                final int count = abis.length;
                for (int i = 0 ; i < count ; i++) {
                    if (abis[i].equals(abiFilter)) {
                        levelMatch = i;
                        break;
                    }
                }

                // check if the density match was a full match or not.
                if (isUniversalDensityMatch) {
                    // check if this match is better than a previous match.
                    if (levelMatch < densityMatchAbiLevel) {
                        densityMatchAbiLevel = levelMatch;
                        universalDensityMatch = output;
                    }
                } else {
                    if (levelMatch < fullMatchAbiLevel) {
                        fullMatchAbiLevel = levelMatch;
                        fullMatch = output;
                    }
                }
            } else {
                // universal abi match, since the density was already checked for. Just need
                // to check what kind of overall match it is.
                if (isUniversalDensityMatch) {
                    universalMatch = output;
                } else {
                    universalAbiMatch = output;
                }
            }
        }

        // full match is better.
        if (fullMatch != null) {
            return fullMatch;
        }

        // then we prefer a universal density over abi, mostly for the case where
        // devices convert native code.
        // We might want to change this depending on whether there are actual splits or not
        // in either dimension.
        if (universalDensityMatch != null) {
            return universalDensityMatch;
        }

        if (universalAbiMatch != null) {
            return universalAbiMatch;
        }

        // last, universal match, or null if none found.
        return universalMatch;
    }
}
