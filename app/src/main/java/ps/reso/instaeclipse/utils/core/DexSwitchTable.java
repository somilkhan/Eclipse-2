package ps.reso.instaeclipse.utils.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Minimal, dependency-free dex reader for one job: given a "string table" method — a method whose
 * body is {@code switch (index) { case N: return "some_string"; ... }} — find which case index (or
 * indices) return a given string.
 *
 * <p>Instagram 457+ moved many literal strings out of the methods that use them into shared tables
 * like {@code X.0000.A00(I)String} (one giant packed-switch). The real user then does
 * {@code const/16 v0, 0x322; invoke-static X/0000;->A00(I)} instead of {@code const-string}, so a
 * DexKit {@code usingStrings} search only finds the table. DexKit doesn't expose switch operands,
 * so this class reads them from the dex directly; {@link StringTables} turns the indices back into
 * a DexKit query ({@code usingNumbers(index) + addInvoke(table)}).
 *
 * <p>Pure Java (no Android / DexKit types) so it is unit-tested on the JVM.
 */
public final class DexSwitchTable {

    private DexSwitchTable() {}

    // ── public API ───────────────────────────────────────────────────────────

    /**
     * Case keys of {@code method}'s switch whose branch loads {@code value} (const-string or
     * const-string/jumbo, possibly after gotos). Empty if the method or string isn't found.
     *
     * @param dex        one dex file's bytes
     * @param className  binary or descriptor class name, e.g. {@code X.0000} or {@code LX/0000;}
     * @param methodName method name, e.g. {@code A00}
     * @param protoDesc  JVM-style descriptor, e.g. {@code (I)Ljava/lang/String;}
     */
    public static List<Integer> caseKeysForString(byte[] dex, String className, String methodName,
                                                  String protoDesc, String value) {
        Dex d = new Dex(dex);
        int strIdx = d.findString(value);
        if (strIdx < 0) return Collections.emptyList();
        int code = d.findCodeOffset(toDescriptor(className), methodName, protoDesc);
        if (code <= 0) return Collections.emptyList();
        return d.caseKeysLoading(code, strIdx);
    }

    /** Same as {@link #caseKeysForString(byte[], String, String, String, String)} over every dex in an APK. */
    public static List<Integer> caseKeysForString(String apkPath, String className, String methodName,
                                                  String protoDesc, String value) throws IOException {
        String desc = toDescriptor(className);
        try (ZipFile zip = new ZipFile(apkPath)) {
            List<? extends ZipEntry> entries = Collections.list(zip.entries());
            for (ZipEntry e : entries) {
                String n = e.getName();
                if (!n.startsWith("classes") || !n.endsWith(".dex") || n.contains("/")) continue;
                byte[] bytes = readAll(zip.getInputStream(e), (int) Math.max(0, e.getSize()));
                Dex d = new Dex(bytes);
                if (d.findClassDef(desc) < 0) continue; // class lives in another dex
                int strIdx = d.findString(value);
                if (strIdx < 0) return Collections.emptyList();
                int code = d.findCodeOffset(desc, methodName, protoDesc);
                if (code <= 0) return Collections.emptyList();
                return d.caseKeysLoading(code, strIdx);
            }
        }
        return Collections.emptyList();
    }

    static String toDescriptor(String className) {
        if (className.startsWith("L") && className.endsWith(";")) return className;
        return "L" + className.replace('.', '/') + ";";
    }

    private static byte[] readAll(InputStream in, int hint) throws IOException {
        try (InputStream is = in) {
            ByteArrayOutputStream bo = new ByteArrayOutputStream(hint > 0 ? hint : 1 << 20);
            byte[] buf = new byte[1 << 16];
            int r;
            while ((r = is.read(buf)) > 0) bo.write(buf, 0, r);
            return bo.toByteArray();
        }
    }

    // ── dex structures ───────────────────────────────────────────────────────

    static final class Dex {
        final byte[] b;
        final int stringIdsSize, stringIdsOff, typeIdsSize, typeIdsOff, protoIdsOff, methodIdsSize, methodIdsOff,
                classDefsSize, classDefsOff;

        Dex(byte[] bytes) {
            this.b = bytes;
            if (bytes.length < 0x70 || bytes[0] != 'd' || bytes[1] != 'e' || bytes[2] != 'x')
                throw new IllegalArgumentException("not a dex file");
            stringIdsSize = u4(0x38); stringIdsOff = u4(0x3C);
            typeIdsSize = u4(0x40); typeIdsOff = u4(0x44);
            protoIdsOff = u4(0x4C);
            methodIdsSize = u4(0x58); methodIdsOff = u4(0x5C);
            classDefsSize = u4(0x60); classDefsOff = u4(0x64);
        }

        int u1(int o) { return b[o] & 0xFF; }
        int u2(int o) { return (b[o] & 0xFF) | ((b[o + 1] & 0xFF) << 8); }
        int s2(int o) { return (short) u2(o); }
        int u4(int o) { return u2(o) | (u2(o + 2) << 16); }

        /** Reads a uleb128 at pos[0], advancing it. */
        int uleb(int[] pos) {
            int result = 0, shift = 0, cur;
            do {
                cur = b[pos[0]++] & 0xFF;
                result |= (cur & 0x7F) << shift;
                shift += 7;
            } while ((cur & 0x80) != 0 && shift < 35);
            return result;
        }

        String string(int idx) {
            int[] pos = {u4(stringIdsOff + idx * 4)};
            int utf16Len = uleb(pos);
            return mutf8(pos[0], utf16Len);
        }

        private String mutf8(int off, int len) {
            char[] out = new char[len];
            int o = off;
            for (int i = 0; i < len; i++) {
                int a = b[o++] & 0xFF;
                if (a < 0x80) out[i] = (char) a;
                else if ((a & 0xE0) == 0xC0) out[i] = (char) (((a & 0x1F) << 6) | (b[o++] & 0x3F));
                else {
                    int b1 = b[o++] & 0x3F, b2 = b[o++] & 0x3F;
                    out[i] = (char) (((a & 0x0F) << 12) | (b1 << 6) | b2);
                }
            }
            return new String(out);
        }

        /** Binary search of the sorted string_ids; -1 if absent. */
        int findString(String s) {
            int lo = 0, hi = stringIdsSize - 1;
            while (lo <= hi) {
                int mid = (lo + hi) >>> 1;
                int c = string(mid).compareTo(s);
                if (c == 0) return mid;
                if (c < 0) lo = mid + 1; else hi = mid - 1;
            }
            return -1;
        }

        String type(int idx) { return string(u4(typeIdsOff + idx * 4)); }

        int findType(String desc) {
            int s = findString(desc);
            if (s < 0) return -1;
            int lo = 0, hi = typeIdsSize - 1; // type_ids are sorted by string index
            while (lo <= hi) {
                int mid = (lo + hi) >>> 1;
                int v = u4(typeIdsOff + mid * 4);
                if (v == s) return mid;
                if (v < s) lo = mid + 1; else hi = mid - 1;
            }
            return -1;
        }

        int findClassDef(String desc) {
            int t = findType(desc);
            if (t < 0) return -1;
            for (int i = 0; i < classDefsSize; i++)
                if (u4(classDefsOff + i * 32) == t) return i;
            return -1;
        }

        String protoDescriptor(int protoIdx) {
            int p = protoIdsOff + protoIdx * 12;
            String ret = type(u4(p + 4));
            int paramsOff = u4(p + 8);
            StringBuilder sb = new StringBuilder("(");
            if (paramsOff != 0) {
                int n = u4(paramsOff);
                for (int i = 0; i < n; i++) sb.append(type(u2(paramsOff + 4 + i * 2)));
            }
            return sb.append(')').append(ret).toString();
        }

        /** code_item offset of the method, 0 if abstract/native, -1 if not found. */
        int findCodeOffset(String classDesc, String name, String proto) {
            int cd = findClassDef(classDesc);
            if (cd < 0) return -1;
            int classDataOff = u4(classDefsOff + cd * 32 + 24);
            if (classDataOff == 0) return -1;
            int[] pos = {classDataOff};
            int sf = uleb(pos), inf = uleb(pos), dm = uleb(pos), vm = uleb(pos);
            for (int i = 0; i < sf + inf; i++) { uleb(pos); uleb(pos); }
            for (int group = 0; group < 2; group++) {
                int count = group == 0 ? dm : vm, idx = 0;
                for (int i = 0; i < count; i++) {
                    idx += uleb(pos);
                    uleb(pos); // access flags
                    int codeOff = uleb(pos);
                    int m = methodIdsOff + idx * 8;
                    if (string(u4(m + 4)).equals(name) && protoDescriptor(u2(m + 2)).equals(proto)) return codeOff;
                }
            }
            return -1;
        }

        /** All switch case keys whose target (following gotos) is a const-string of {@code strIdx}. */
        List<Integer> caseKeysLoading(int codeOff, int strIdx) {
            int insnsSize = u4(codeOff + 12);
            int insns = codeOff + 16;
            List<Integer> keys = new ArrayList<>();
            // const-string values hoisted before the switch (d8 shares one register for repeated
            // strings: `const-string v0, "x"; switch; ... case: return-object v0`)
            java.util.Map<Integer, Integer> hoisted = new java.util.HashMap<>();
            int pc = 0;
            while (pc < insnsSize) {
                int unit = u2(insns + pc * 2);
                int op = unit & 0xFF;
                if (op == 0x00 && unit != 0) { // payload pseudo-instruction
                    pc += payloadUnits(insns + pc * 2, unit);
                    continue;
                }
                if (op == 0x1A) hoisted.put(u1(insns + pc * 2 + 1), u2(insns + pc * 2 + 2));
                else if (op == 0x1B) hoisted.put(u1(insns + pc * 2 + 1), u4(insns + pc * 2 + 2));
                if (op == 0x2B || op == 0x2C) { // packed-switch / sparse-switch vAA, +BBBBBBBB
                    int payload = insns + (pc + u4(insns + pc * 2 + 2)) * 2;
                    int ident = u2(payload), size = u2(payload + 2);
                    for (int i = 0; i < size; i++) {
                        int key, rel;
                        if (ident == 0x0100) { key = u4(payload + 4) + i; rel = u4(payload + 8 + i * 4); }
                        else if (ident == 0x0200) { key = u4(payload + 4 + i * 4); rel = u4(payload + 4 + size * 4 + i * 4); }
                        else break;
                        if (loadsString(insns, insnsSize, pc + rel, strIdx, hoisted)) keys.add(key);
                    }
                }
                pc += width(op);
            }
            return keys;
        }

        private boolean loadsString(int insns, int insnsSize, int target, int strIdx, java.util.Map<Integer, Integer> hoisted) {
            for (int hops = 0; hops < 4 && target >= 0 && target < insnsSize; hops++) {
                int at = insns + target * 2;
                int op = u1(at);
                if (op == 0x1A) return u2(at + 2) == strIdx;          // const-string vAA, string@BBBB
                if (op == 0x1B) return u4(at + 2) == strIdx;          // const-string/jumbo vAA, string@BBBBBBBB
                if (op == 0x11) { Integer h = hoisted.get(u1(at + 1)); return h != null && h == strIdx; } // return-object vAA
                if (op == 0x28) { target += (byte) u1(at + 1); continue; }   // goto +AA
                if (op == 0x29) { target += s2(at + 2); continue; }          // goto/16
                if (op == 0x2A) { target += u4(at + 2); continue; }          // goto/32
                return false;
            }
            return false;
        }

        private int payloadUnits(int at, int ident) {
            int size = u2(at + 2);
            if (ident == 0x0100) return 4 + size * 2;
            if (ident == 0x0200) return 2 + size * 4;
            if (ident == 0x0300) { int w = u2(at + 2), n = u4(at + 4); return 4 + (w * n + 1) / 2; }
            return 1;
        }
    }

    /** Instruction width in 16-bit code units, by opcode (Dalvik bytecode formats). */
    static int width(int op) {
        if (op <= 0x00) return 1;
        if (op <= 0x01) return 1;           // move
        if (op == 0x02) return 2;           // move/from16
        if (op == 0x03) return 3;           // move/16
        if (op == 0x04) return 1;
        if (op == 0x05) return 2;
        if (op == 0x06) return 3;
        if (op == 0x07) return 1;
        if (op == 0x08) return 2;
        if (op == 0x09) return 3;
        if (op <= 0x12) return 1;           // move-result*, move-exception, return*, const/4
        if (op == 0x13) return 2;           // const/16
        if (op == 0x14) return 3;           // const
        if (op == 0x15) return 2;           // const/high16
        if (op == 0x16) return 2;           // const-wide/16
        if (op == 0x17) return 3;           // const-wide/32
        if (op == 0x18) return 5;           // const-wide
        if (op == 0x19) return 2;           // const-wide/high16
        if (op == 0x1A) return 2;           // const-string
        if (op == 0x1B) return 3;           // const-string/jumbo
        if (op == 0x1C) return 2;           // const-class
        if (op <= 0x1E) return 1;           // monitor-enter/exit
        if (op <= 0x20) return 2;           // check-cast, instance-of
        if (op == 0x21) return 1;           // array-length
        if (op <= 0x23) return 2;           // new-instance, new-array
        if (op <= 0x26) return 3;           // filled-new-array(/range), fill-array-data
        if (op == 0x27) return 1;           // throw
        if (op == 0x28) return 1;           // goto
        if (op == 0x29) return 2;           // goto/16
        if (op <= 0x2C) return 3;           // goto/32, packed-switch, sparse-switch
        if (op <= 0x31) return 2;           // cmp*
        if (op <= 0x3D) return 2;           // if-*
        if (op <= 0x43) return 1;           // unused
        if (op <= 0x6D) return 2;           // aget/aput/iget/iput/sget/sput
        if (op <= 0x72) return 3;           // invoke-*
        if (op == 0x73) return 1;           // unused
        if (op <= 0x78) return 3;           // invoke-*/range
        if (op <= 0x7A) return 1;           // unused
        if (op <= 0x8F) return 1;           // unops
        if (op <= 0xAF) return 2;           // binop
        if (op <= 0xCF) return 1;           // binop/2addr
        if (op <= 0xE2) return 2;           // binop/lit16, lit8
        if (op <= 0xF9) return 1;           // unused
        if (op <= 0xFB) return 4;           // invoke-polymorphic(/range)
        if (op <= 0xFD) return 3;           // invoke-custom(/range)
        return 2;                           // const-method-handle, const-method-type
    }
}
