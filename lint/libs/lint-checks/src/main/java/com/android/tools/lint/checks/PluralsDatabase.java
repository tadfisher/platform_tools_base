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
package com.android.tools.lint.checks;

import static com.android.tools.lint.checks.PluralsDatabase.Quantity.few;
import static com.android.tools.lint.checks.PluralsDatabase.Quantity.many;
import static com.android.tools.lint.checks.PluralsDatabase.Quantity.one;
import static com.android.tools.lint.checks.PluralsDatabase.Quantity.two;
import static com.android.tools.lint.checks.PluralsDatabase.Quantity.zero;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.lint.detector.api.LintUtils;
import com.google.common.base.Charsets;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.ByteStreams;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Database used by the {@link com.android.tools.lint.checks.PluralsDetector} to get information
 * about plural forms for a given language
 */
public class PluralsDatabase {
    private static final boolean DEBUG = false;
    private static final EnumSet<Quantity> NONE = EnumSet.noneOf(Quantity.class);

    private static final PluralsDatabase sInstance = new PluralsDatabase();

    private Map<String, EnumSet<Quantity>> mPlurals;
    private Map<Quantity, Set<String>> mMultiValueSetNames = Maps.newEnumMap(Quantity.class);
    private String mDescriptions;
    private int mRuleSetOffset;
    private Map<String,String> mSetNamePerLanguage;

    @NonNull
    public static PluralsDatabase get() {
        return sInstance;
    }

    @Nullable
    public EnumSet<Quantity> getRelevant(@NonNull String language) {
        ensureInitialized();
        EnumSet<Quantity> set = mPlurals.get(language);
        if (set == null) {
            String s = getLocaleData(language);
            if (s == null) {
                mPlurals.put(language, NONE);
                return null;
            }
            // Process each item and look for relevance

            set = EnumSet.noneOf(Quantity.class);
            int length = s.length();
            for (int offset = 0, end; offset < length; offset = end + 1) {
                for (; offset < length; offset++) {
                    if (!Character.isWhitespace(s.charAt(offset))) {
                        break;
                    }
                }

                int begin = s.indexOf('{', offset);
                if (begin == -1) {
                    break;
                }
                end = findBalancedEnd(s, begin);
                if (end == -1) {
                    end = length;
                }

                if (s.startsWith("other{", offset)) {
                    // Not included
                    continue;
                }

                // Make sure the rule references applies to integers:
                // Rule definition mentions n or i or @integer
                //
                //    n  absolute value of the source number (integer and decimals).
                //    i  integer digits of n.
                //    v  number of visible fraction digits in n, with trailing zeros.
                //    w  number of visible fraction digits in n, without trailing zeros.
                //    f  visible fractional digits in n, with trailing zeros.
                //    t  visible fractional digits in n, without trailing zeros.
                boolean appliesToIntegers = false;
                boolean inQuotes = false;
                for (int i = begin + 1; i < end - 1; i++) {
                    char c = s.charAt(i);
                    if (c == '"') {
                        inQuotes = !inQuotes;
                    } else if (inQuotes) {
                        if (c == '@') {
                            if (s.startsWith("@integer", i)) {
                                appliesToIntegers = true;
                                break;
                            } else {
                                // @decimal always comes after @integer
                                break;
                            }
                        } else if ((c == 'i' || c == 'n') && Character
                                .isWhitespace(s.charAt(i + 1))) {
                            appliesToIntegers = true;
                            break;
                        }
                    }
                }

                if (!appliesToIntegers) {
                    if (DEBUG) {
                        System.out.println("Skipping quantity " + s.substring(offset, begin)
                                + " in set for locale " + language + " (" + getSetName(language)
                                + ")");
                    }
                    continue;
                }

                if (s.startsWith("one{", offset)) {
                    set.add(one);
                } else if (s.startsWith("few{", offset)) {
                    set.add(few);
                } else if (s.startsWith("many{", offset)) {
                    set.add(many);
                } else if (s.startsWith("two{", offset)) {
                    set.add(two);
                } else if (s.startsWith("zero{", offset)) {
                    set.add(zero);
                } else {
                    // Unexpected quantity: ignore
                    if (DEBUG) {
                        assert false : s.substring(offset, Math.min(offset + 10, length));
                    }
                }
            }

            mPlurals.put(language, set);
        }
        return set == NONE ? null : set;
    }

    public boolean hasMultipleValuesForQuantity(
            @NonNull String language,
            @NonNull Quantity quantity) {
        if (quantity == Quantity.one || quantity == Quantity.two || quantity == Quantity.zero) {
            ensureInitialized();
            String setName = getSetName(language);
            if (setName != null) {
                Set<String> names = mMultiValueSetNames.get(quantity);
                assert names != null : quantity;
                return names.contains(setName);
            }
        }

        return false;
    }

    private void ensureInitialized() {
        if (mPlurals == null) {
            initialize();
        }
    }

    @SuppressWarnings({"UnnecessaryLocalVariable", "UnusedDeclaration"})
    private void initialize() {
        // Sets where more than a single integer maps to the quantity. Take for example
        // set 10:
        //    set10{
        //        one{
        //            "n % 10 = 1 and n % 100 != 11 @integer 1, 21, 31, 41, 51, 61, 71, 81,"
        //            " 101, 1001, … @decimal 1.0, 21.0, 31.0, 41.0, 51.0, 61.0, 71.0, 81.0"
        //            ", 101.0, 1001.0, …"
        //        }
        //    }
        // Here we see that both "1" and "21" will match the "one" category.
        // Note that this only applies to integers (since getQuantityString only takes integer)
        // whereas the plurals data also covers fractions. I was not sure what to do about
        // set17:
        //    set17{
        //        one{"i = 0,1 and n != 0 @integer 1 @decimal 0.1~1.6"}
        //    }
        // since it looks to me like this only differs from 1 in the fractional part.
        //
        // This is currently manually encoded (by looking at the rules). It would be
        // great to handle this automatically via the parser instead.

        mMultiValueSetNames = Maps.newEnumMap(Quantity.class);
        mMultiValueSetNames.put(Quantity.two, Sets.newHashSet(
                "set13", "set19", "set22", "set23", "set40", "set43", "set44", "set45"
        ));
        mMultiValueSetNames.put(Quantity.one, Sets.newHashSet(
                "set10", "set13", "set15", "set18", "set19", "set21", "set22", "set23", "set25",
                "set3", "set30", "set31", "set32", "set33", "set34", "set35", "set38", "set39",
                "set4", "set40", "set42", "set45", "set5", "set9"
        ));
        mMultiValueSetNames.put(Quantity.zero, Collections.singleton("set5"));

        mSetNamePerLanguage = Maps.newHashMapWithExpectedSize(20);
        mPlurals = Maps.newHashMapWithExpectedSize(20);
    }

    @Nullable
    public String findIntegerExamples(@NonNull String language, @NonNull Quantity quantity) {
        String data = getQuantityData(language, quantity);
        if (data != null) {
            int index = data.indexOf("@integer");
            if (index == -1) {
                return null;
            }
            int start = index + "@integer".length();
            int end = data.indexOf('@', start);
            if (end == -1) {
                end = data.length();
            }
            return data.substring(start, end).trim();
        }

        return null;
    }


    @NonNull
    private String getPluralsDescriptions() {
        if (mDescriptions == null) {
            InputStream stream = PluralsDetector.class.getResourceAsStream("data/plurals.txt");
            if (stream != null) {
                try {
                    byte[] bytes = ByteStreams.toByteArray(stream);
                    mDescriptions = new String(bytes, Charsets.UTF_8);
                    mRuleSetOffset = mDescriptions.indexOf("rules{");
                    if (mRuleSetOffset == -1) {
                        if (DEBUG) {
                            assert false;
                        }
                        mDescriptions = "";
                        mRuleSetOffset = 0;
                    }

                } catch (IOException e) {
                    try {
                        stream.close();
                    } catch (IOException e1) {
                        // Stupid API.
                    }
                }
            }
            if (mDescriptions == null) {
                mDescriptions = "";
            }
        }
        return mDescriptions;
    }

    @Nullable
    public String getQuantityData(@NonNull String language, @NonNull Quantity quantity) {
        String data = getLocaleData(language);
        if (data == null) {
            return null;
        }
        String quantityDeclaration = quantity.name() + "{";
        int quantityStart = data.indexOf(quantityDeclaration);
        if (quantityStart == -1) {
            return null;
        }
        int quantityEnd = findBalancedEnd(data, quantityStart);
        if (quantityEnd == -1) {
            return null;
        }
        //String s = data.substring(quantityStart + quantityDeclaration.length(), quantityEnd);
        StringBuilder sb = new StringBuilder();
        boolean inString = false;
        for (int i = quantityStart + quantityDeclaration.length(); i < quantityEnd; i++) {
            char c = data.charAt(i);
            if (c == '"') {
                inString = !inString;
            } else if (inString) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    @Nullable
    public String getSetName(@NonNull String language) {
        String name = mSetNamePerLanguage.get(language);
        if (name == null) {
            name = findSetName(language);
            if (name == null) {
                name = ""; // Store "" instead of null so we remember search result
            }
            mSetNamePerLanguage.put(language, name);
        }

        return name.isEmpty() ? null : name;
    }

    @Nullable
    private String findSetName(@NonNull String language) {
        String data = getPluralsDescriptions();
        int index = data.indexOf("locales{");
        if (index == -1) {
            return null;
        }
        int end = data.indexOf("locales_ordinals{", index + 1);
        if (end == -1) {
            return null;
        }
        String languageDeclaration = " " + language + "{\"";
        index = data.indexOf(languageDeclaration);
        if (index == -1 || index >= end) {
            return null;
        }
        int setEnd = data.indexOf('\"', index + languageDeclaration.length());
        if (setEnd == -1) {
            return null;
        }
        return data.substring(index + languageDeclaration.length(), setEnd).trim();
    }

    @Nullable
    public String getLocaleData(@NonNull String language) {
        String set = getSetName(language);
        if (set == null) {
            return null;
        }
        String data = getPluralsDescriptions();
        int setStart = data.indexOf(set + "{", mRuleSetOffset);
        if (setStart == -1) {
            return null;
        }
        int setEnd = findBalancedEnd(data, setStart);
        if (setEnd == -1) {
            return null;
        }
        return data.substring(setStart + set.length() + 1, setEnd);
    }

    private static int findBalancedEnd(String data, int offset) {
        int balance = 0;
        int length = data.length();
        for (; offset < length; offset++) {
            char c = data.charAt(offset);
            if (c == '{') {
                balance++;
            } else if (c == '}') {
                balance--;
                if (balance == 0) {
                    return offset;
                }
            }
        }

        return -1;
    }

    public enum Quantity {
        // deliberately lower case to match attribute names
        few, many, one, two, zero, other;

        @Nullable
        public static Quantity get(@NonNull String name) {
            for (Quantity quantity : values()) {
                if (name.equals(quantity.name())) {
                    return quantity;
                }
            }

            return null;
        }

        public static String formatSet(EnumSet<Quantity> set) {
            List<String> list = new ArrayList<String>(set.size());
            for (Quantity quantity : set) {
                list.add(quantity.name());
            }
            return LintUtils.formatList(list, Integer.MAX_VALUE);
        }
    }
}
