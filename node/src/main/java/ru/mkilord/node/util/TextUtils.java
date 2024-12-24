package ru.mkilord.node.util;

import org.apache.commons.lang3.math.NumberUtils;

public class TextUtils {
    public static boolean isRange(String str, int min, int max) {
        if (str.isBlank()) return false;
        if (!NumberUtils.isCreatable(str)) {
            return false;
        }
        var num = Integer.parseInt(str);
        return num >= min && num <= max;
    }

    public static boolean isProneNumber(String str) {
        if (str.isBlank()) return false;
        if (!NumberUtils.isCreatable(str)) {
            return false;
        }
        return isSize(str, 11);
    }

    public static boolean isSize(String str, int minSize) {
        return str.length() >= minSize;
    }

}
