package io.github.somilkhan.instaeclipse2.mods.piko;

import java.util.ArrayList;
import java.util.List;

/**
 * Thread ids out of DirectThreadKey.toString(). Instagram 458 prints
 * {@code DirectThreadKey{mThreadId='X', mThreadV2Id='Y}} (no closing quote on the second);
 * both quote styles are accepted. Pure, unit tested.
 */
public final class ThreadKeyIds {

    private ThreadKeyIds() {}

    public static List<String> parse(String key) {
        List<String> out = new ArrayList<>(2);
        if (key == null) return out;
        add(out, value(key, "mThreadId='"));
        add(out, value(key, "mThreadV2Id='"));
        return out;
    }

    private static String value(String s, String label) {
        int i = s.indexOf(label);
        if (i < 0) return null;
        int start = i + label.length();
        int end = start;
        while (end < s.length() && s.charAt(end) != '\'' && s.charAt(end) != '}' && s.charAt(end) != ',') end++;
        return s.substring(start, end).trim();
    }

    private static void add(List<String> out, String v) {
        if (v == null || v.isEmpty() || v.equals("null") || out.contains(v)) return;
        out.add(v);
    }
}
