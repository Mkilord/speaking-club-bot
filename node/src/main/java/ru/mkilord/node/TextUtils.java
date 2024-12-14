package ru.mkilord.node;

import org.apache.commons.lang3.math.NumberUtils;

public class TextUtils {
    public static boolean isInRange(String str, int min, int max) {
        if (!NumberUtils.isCreatable(str)) {
            return false;
        }
        var num = Integer.parseInt(str);
        return num >= min && num <= max;
    }
}
