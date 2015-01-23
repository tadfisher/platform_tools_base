/*
 * Copyright 2000-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.ide.common.blame.parser.util;

import com.android.annotations.NonNull;

public class StringUtil {

    public static int indexOf(@NonNull CharSequence s, char c, int start, int end, boolean caseSensitive) {
        for (int i = start; i < end; i++) {
            if (charsMatch(s.charAt(i), c, !caseSensitive)) return i;
        }
        return -1;
    }

    private static boolean charsMatch(char c1, char c2, boolean ignoreCase) {
        return compare(c1, c2, ignoreCase) == 0;
    }


    private static int compare(char c1, char c2, boolean ignoreCase) {
        // duplicating String.equalsIgnoreCase logic
        int d = c1 - c2;
        if (d == 0 || !ignoreCase) {
            return d;
        }
        // If characters don't match but case may be ignored,
        // try converting both characters to uppercase.
        // If the results match, then the comparison scan should
        // continue.
        char u1 = StringUtil.toUpperCase(c1);
        char u2 = StringUtil.toUpperCase(c2);
        d = u1 - u2;
        if (d != 0) {
            // Unfortunately, conversion to uppercase does not work properly
            // for the Georgian alphabet, which has strange rules about case
            // conversion.  So we need to make one last check before
            // exiting.
            d = StringUtil.toLowerCase(u1) - StringUtil.toLowerCase(u2);
        }
        return d;
    }


    @NonNull
    public static String toUpperCase(@NonNull String s) {
        StringBuilder answer = null;

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            char upcased = toUpperCase(c);
            if (answer == null && upcased != c) {
                answer = new StringBuilder(s.length());
                answer.append(s.substring(0, i));
            }

            if (answer != null) {
                answer.append(upcased);
            }
        }

        return answer == null ? s : answer.toString();
    }

    private static char toUpperCase(char a) {
        if (a < 'a') {
            return a;
        }
        if (a <= 'z') {
            return (char)(a + ('A' - 'a'));
        }
        return Character.toUpperCase(a);
    }

    public static char toLowerCase(char a) {
        if (a < 'A' || a >= 'a' && a <= 'z') {
            return a;
        }

        if (a <= 'Z') {
            return (char)(a + ('a' - 'A'));
        }

        return Character.toLowerCase(a);
    }
}
