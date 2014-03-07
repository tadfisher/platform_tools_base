package com.android.build.gradle.internal;

import java.util.HashMap;
import java.util.Map;


class StrResIdUtil {
    private static char LRM = 0x200e;
    private static char RLM = 0x200f;
    private static int ID_LENGTH = 32;

    // Mapping of string name hash code to string name.
    private Map<Integer, String> mIdToName;

    StrResIdUtil() {
        mIdToName = new HashMap<Integer, String>();
    }

    private int getNameHashCode(String name) {
        int hashCode = name.hashCode() % Integer.MAX_VALUE;

        String existName;
        int offset = 1;
        while ((existName = mIdToName.get(hashCode)) != null && !existName.equals(name)) {
            // look for next available slot
            hashCode += (int) Math.pow(offset, offset);
            hashCode = hashCode % Integer.MAX_VALUE;

            ++offset;
        }
        mIdToName.put(hashCode, name);
        return hashCode;
    }

    /**
     * Encodes string {@code name} in its hash code's binary representation and use LRM/RLM
     * to represent binary value.
     *
     * @param name string name.
     * @return binary representation of name's hash code.
     */
    String encode(String name) {
        int id = getNameHashCode(name);
        String binaryStr = Integer.toBinaryString(id);
        char[] marks = new char[ID_LENGTH];
        // First several bits might be 0.
        int index = 0;
        for (; index < ID_LENGTH - binaryStr.length(); ++index) {
            marks[index] = LRM;
        }
        for (int i = 0; i < binaryStr.length(); ++i) {
            char ch = binaryStr.charAt(i);
            if (ch == '1') {
                marks[index++] = RLM;
            } else {
                marks[index++] = LRM;
            }
        }
        return new String(marks);
    }

    int decode(String idString) {
        char[] binaryStr = new char[idString.length()];
        for (int i = 0; i < idString.length(); ++i) {
            char ch = idString.charAt(i);
            if (ch == LRM) {
                binaryStr[i] = '0';
            } else {
                binaryStr[i] = '1';
            }
        }
        Long id = Long.parseLong(new String(binaryStr), 2);
        return id.intValue();
    }

}

