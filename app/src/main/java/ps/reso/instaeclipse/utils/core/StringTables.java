package ps.reso.instaeclipse.utils.core;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.enums.StringMatchType;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import ps.reso.instaeclipse.utils.log.ModuleLog;

/**
 * Finds the code that uses a string even when Instagram (457+) moved the literal into a shared
 * string table.
 *
 * <p>Pattern in 457: {@code X.0000.A00(I)String} is one giant {@code packed-switch} whose cases
 * each {@code return "<string>"}. The real user no longer contains {@code const-string}; it does
 * {@code const/16 v0, 0x322; invoke-static {v0}, LX/0000;->A00(I)Ljava/lang/String;}. So:
 * <ol>
 *   <li>DexKit: static {@code (int) -> String} methods that use the string = the table(s);</li>
 *   <li>{@link DexSwitchTable}: read the table's switch to get the case index for the string;</li>
 *   <li>DexKit: methods with {@code usingNumbers(index)} that {@code addInvoke(table)} = the users.</li>
 * </ol>
 *
 * <p>Usage — prefer {@link #findUsers}, which also returns direct (pre-457) users, so one call
 * works on old and new builds:
 * <pre>
 *   for (MethodData m : StringTables.findUsers(bridge, "high_quality_media_upload")) ...
 * </pre>
 * Combine with other constraints via {@link #usingTableString} or {@link #findTableRefs}.
 *
 * <p>{@link #init(String)} must have been called with the Instagram APK path (Module does this with
 * {@code lpparam.appInfo.sourceDir}); without it only direct users are returned.
 */
public final class StringTables {

    private StringTables() {}

    /** A table method plus the case index that returns the requested string. */
    public static final class TableRef {
        public final MethodData table;
        public final int index;

        TableRef(MethodData table, int index) { this.table = table; this.index = index; }

        /** Matcher for code that loads this string via the table: usingNumbers(index) + invoke(table). */
        public MethodMatcher userMatcher() {
            return MethodMatcher.create().usingNumbers(index).addInvoke(table.getDescriptor());
        }

        @Override public String toString() { return table.getDescriptor() + "[" + index + "]"; }
    }

    private static volatile String apkPath;
    private static final Map<String, List<Integer>> keyCache = Collections.synchronizedMap(new HashMap<>());
    private static final Map<String, String> classToDex = Collections.synchronizedMap(new HashMap<>());
    private static SoftReference<Object[]> lastDex = new SoftReference<>(null); // {entryName, Dex}

    /** Sets the Instagram APK the bridge was created from. Safe to call repeatedly. */
    public static void init(String igApkPath) {
        if (igApkPath != null && !igApkPath.equals(apkPath)) {
            apkPath = igApkPath;
            keyCache.clear();
            classToDex.clear();
            lastDex = new SoftReference<>(null);
        }
    }

    public static String apkPath() { return apkPath; }

    // ── public lookups ───────────────────────────────────────────────────────

    /** Direct users ({@code const-string}) ∪ table users, de-duplicated, table methods excluded. */
    public static List<MethodData> findUsers(DexKitBridge bridge, String value) {
        Map<String, MethodData> out = new LinkedHashMap<>();
        List<TableRef> refs = findTableRefs(bridge, value);
        java.util.Set<String> tables = new java.util.HashSet<>();
        for (TableRef r : refs) tables.add(r.table.getDescriptor());
        try {
            for (MethodData m : bridge.findMethod(FindMethod.create().matcher(
                    MethodMatcher.create().addUsingString(value, StringMatchType.Equals))))
                if (!tables.contains(m.getDescriptor())) out.put(m.getDescriptor(), m);
        } catch (Throwable t) {
            ModuleLog.line("(StringTables) direct lookup failed for '" + value + "': " + t);
        }
        for (MethodData m : usersOf(bridge, refs)) out.putIfAbsent(m.getDescriptor(), m);
        return new ArrayList<>(out.values());
    }

    /**
     * Drop-in replacement for {@code findMethod(matcher.usingStrings(strings...))} that survives
     * 457's string tables. First runs the plain query (old builds, or strings still inline); if it
     * finds nothing, every string that lives in a table is replaced by
     * {@code usingNumbers(index) + addInvoke(table)} and the query runs again.
     *
     * @param extra adds the non-string constraints (return type, params, declaring class...) to a
     *              fresh matcher; may be {@code null}
     */
    public static List<MethodData> findMethodUsingStrings(DexKitBridge bridge,
                                                          java.util.function.Consumer<MethodMatcher> extra,
                                                          String... strings) {
        MethodMatcher direct = MethodMatcher.create();
        for (String s : strings) direct.addUsingString(s, StringMatchType.Equals);
        if (extra != null) extra.accept(direct);
        List<MethodData> found;
        try {
            found = bridge.findMethod(FindMethod.create().matcher(direct));
        } catch (Throwable t) {
            ModuleLog.line("(StringTables) direct query failed: " + t);
            found = Collections.emptyList();
        }
        found = withoutTables(found);
        if (!found.isEmpty()) return found;

        MethodMatcher viaTable = MethodMatcher.create();
        List<Number> numbers = new ArrayList<>();
        java.util.Set<String> tables = new java.util.LinkedHashSet<>();
        boolean anyTable = false;
        for (String s : strings) {
            List<TableRef> refs = findTableRefs(bridge, s);
            if (refs.isEmpty()) {
                viaTable.addUsingString(s, StringMatchType.Equals);
            } else {
                anyTable = true;
                numbers.add(refs.get(0).index);
                tables.add(refs.get(0).table.getDescriptor());
            }
        }
        if (!anyTable) return found;
        if (!numbers.isEmpty()) viaTable.usingNumbers(numbers);
        for (String t : tables) viaTable.addInvoke(t);
        if (extra != null) extra.accept(viaTable);
        try {
            List<MethodData> out = new ArrayList<>();
            for (MethodData m : bridge.findMethod(FindMethod.create().matcher(viaTable)))
                if (!tables.contains(m.getDescriptor())) out.add(m);
            return out;
        } catch (Throwable t) {
            ModuleLog.line("(StringTables) table query failed: " + t);
            return Collections.emptyList();
        }
    }

    /**
     * Drops 457-style string-table methods from a plain string query's result. A table
     * ({@code static String A00(int)} with one {@code const-string} per switch case) contains every
     * literal it serves, so {@code usingStrings(x)} matches the table itself; callers that then
     * inspect the "user" (its class, its invokes) silently work on the wrong method.
     */
    public static List<MethodData> withoutTables(List<MethodData> found) {
        if (found == null || found.isEmpty()) return found;
        List<MethodData> out = null;
        for (int i = 0; i < found.size(); i++) {
            MethodData m = found.get(i);
            boolean table = false;
            try { table = isTableShape(m.getModifiers(), m.getParamTypeNames(), m.getReturnTypeName()); }
            catch (Throwable ignored) {}
            if (table && out == null) out = new ArrayList<>(found.subList(0, i));
            else if (!table && out != null) out.add(m);
        }
        return out == null ? found : out;
    }

    /** Shape of a shared string table: static, single int parameter, returns String. */
    static boolean isTableShape(int modifiers, List<String> paramTypeNames, String returnTypeName) {
        return Modifier.isStatic(modifiers) && paramTypeNames != null && paramTypeNames.size() == 1
                && "int".equals(paramTypeNames.get(0)) && "java.lang.String".equals(returnTypeName);
    }

    /** Only the methods that load {@code value} through a string table. */
    public static List<MethodData> findTableUsers(DexKitBridge bridge, String value) {
        return usersOf(bridge, findTableRefs(bridge, value));
    }

    /**
     * First table reference as a matcher to combine with more constraints, e.g.
     * {@code StringTables.usingTableString(b, "x").paramCount(2)}. {@code null} if the string isn't
     * in any table.
     */
    public static MethodMatcher usingTableString(DexKitBridge bridge, String value) {
        List<TableRef> refs = findTableRefs(bridge, value);
        return refs.isEmpty() ? null : refs.get(0).userMatcher();
    }

    /** Every (table method, case index) pair that returns {@code value}. */
    public static List<TableRef> findTableRefs(DexKitBridge bridge, String value) {
        List<TableRef> refs = new ArrayList<>();
        if (bridge == null || value == null) return refs;
        List<MethodData> tables;
        try {
            tables = bridge.findMethod(FindMethod.create().matcher(MethodMatcher.create()
                    .addUsingString(value, StringMatchType.Equals)
                    .modifiers(Modifier.STATIC)
                    .paramTypes(int.class)
                    .returnType(String.class)));
        } catch (Throwable t) {
            ModuleLog.line("(StringTables) table lookup failed for '" + value + "': " + t);
            return refs;
        }
        for (MethodData t : tables) {
            for (int idx : caseKeys(t, value)) refs.add(new TableRef(t, idx));
        }
        return refs;
    }

    private static List<MethodData> usersOf(DexKitBridge bridge, List<TableRef> refs) {
        Map<String, MethodData> out = new LinkedHashMap<>();
        for (TableRef r : refs) {
            try {
                for (MethodData m : bridge.findMethod(FindMethod.create().matcher(r.userMatcher())))
                    if (!m.getDescriptor().equals(r.table.getDescriptor())) out.putIfAbsent(m.getDescriptor(), m);
            } catch (Throwable t) {
                ModuleLog.line("(StringTables) user lookup failed for " + r + ": " + t);
            }
        }
        return new ArrayList<>(out.values());
    }

    // ── dex access ───────────────────────────────────────────────────────────

    /** Case indices of {@code table}'s switch that return {@code value}. */
    public static List<Integer> caseKeys(MethodData table, String value) {
        String key = table.getDescriptor() + "\u0000" + value;
        List<Integer> cached = keyCache.get(key);
        if (cached != null) return cached;
        List<Integer> keys = Collections.emptyList();
        String apk = apkPath;
        if (apk != null) {
            try {
                DexSwitchTable.Dex dex = dexFor(apk, DexSwitchTable.toDescriptor(table.getClassName()));
                if (dex != null) {
                    int s = dex.findString(value);
                    int code = dex.findCodeOffset(DexSwitchTable.toDescriptor(table.getClassName()),
                            table.getMethodName(), table.getMethodSign());
                    if (s >= 0 && code > 0) keys = dex.caseKeysLoading(code, s);
                }
            } catch (Throwable t) {
                ModuleLog.line("(StringTables) reading " + table.getDescriptor() + " failed: " + t);
            }
        }
        keyCache.put(key, keys);
        return keys;
    }

    private static synchronized DexSwitchTable.Dex dexFor(String apk, String classDesc) throws Exception {
        Object[] last = lastDex.get();
        String known = classToDex.get(classDesc);
        if (last != null && (known == null || known.equals(last[0]))
                && ((DexSwitchTable.Dex) last[1]).findClassDef(classDesc) >= 0) {
            classToDex.put(classDesc, (String) last[0]);
            return (DexSwitchTable.Dex) last[1];
        }
        try (ZipFile zip = new ZipFile(apk)) {
            List<String> names = new ArrayList<>();
            if (known != null) names.add(known);
            for (ZipEntry e : Collections.list(zip.entries())) {
                String n = e.getName();
                if (n.startsWith("classes") && n.endsWith(".dex") && !n.contains("/") && !n.equals(known)) names.add(n);
            }
            for (String n : names) {
                ZipEntry e = zip.getEntry(n);
                DexSwitchTable.Dex d = new DexSwitchTable.Dex(read(zip.getInputStream(e), (int) e.getSize()));
                if (d.findClassDef(classDesc) >= 0) {
                    classToDex.put(classDesc, n);
                    lastDex = new SoftReference<>(new Object[]{n, d});
                    return d;
                }
            }
        }
        return null;
    }

    private static byte[] read(InputStream in, int hint) throws Exception {
        try (InputStream is = in) {
            ByteArrayOutputStream bo = new ByteArrayOutputStream(hint > 0 ? hint : 1 << 20);
            byte[] buf = new byte[1 << 16];
            int r;
            while ((r = is.read(buf)) > 0) bo.write(buf, 0, r);
            return bo.toByteArray();
        }
    }
}
