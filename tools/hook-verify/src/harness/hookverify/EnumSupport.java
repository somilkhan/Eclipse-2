package hookverify;

import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.NarrowLiteralInstruction;
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction;
import org.jf.dexlib2.iface.instruction.ReferenceInstruction;
import org.jf.dexlib2.iface.instruction.formats.Instruction35c;
import org.jf.dexlib2.iface.instruction.formats.Instruction3rc;
import org.jf.dexlib2.iface.reference.FieldReference;
import org.jf.dexlib2.iface.reference.MethodReference;
import org.jf.dexlib2.iface.reference.StringReference;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Real enum constants for synthesised Instagram enums. Module code calls {@code values()},
 * {@code valueOf()}, reads {@code Enum.name()} and static enum fields; with throwing stubs every
 * such hook looked broken offline although it works on the phone. The constants (field, name,
 * ordinal) are read from the enum's dex {@code <clinit>} — {@code const-string vN "NAME"},
 * {@code const vM, ordinal}, {@code invoke-direct {vX, vN, vM, ...}, T;-><init>(String, int, ...)},
 * {@code sput-object vX, T;->field} — and materialised without running Instagram code.
 */
public final class EnumSupport {
    private EnumSupport() {}

    /** class binary name -> constants in ordinal order; each {fieldName, constName, ordinal}. */
    private static final Map<String, List<Object[]>> REGISTRY = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Object> VALUES = new ConcurrentHashMap<>();

    static boolean register(ClassDef def) {
        if (!"Ljava/lang/Enum;".equals(def.getSuperclass())) return false;
        String type = def.getType();
        MethodImplementation impl = null;
        for (Method m : def.getMethods())
            if (m.getName().equals("<clinit>")) impl = m.getImplementation();
        if (impl == null) return false;
        // <init>s: which argument (register-list index, 0 = this) carries the name / ordinal,
        // read from their Enum.<init>(String, int) super call.
        Map<String, int[]> inits = new HashMap<>();
        for (Method m : def.getMethods()) {
            if (!m.getName().equals("<init>") || m.getImplementation() == null) continue;
            int base = m.getImplementation().getRegisterCount() - insSize(m.getParameterTypes(), false);
            for (Instruction i : m.getImplementation().getInstructions()) {
                if (i.getOpcode() != Opcode.INVOKE_DIRECT && i.getOpcode() != Opcode.INVOKE_DIRECT_RANGE) continue;
                MethodReference mr = (MethodReference) ((ReferenceInstruction) i).getReference();
                if (!mr.getDefiningClass().equals("Ljava/lang/Enum;") || !mr.getName().equals("<init>")) continue;
                int[] r = regsOf(i);
                if (r.length >= 3) inits.put(key("<init>", m.getParameterTypes()), new int[]{r[1] - base, r[2] - base});
            }
        }
        // static factories T.X(..)T doing new-instance + <init>: argument index of name / ordinal
        Map<String, int[]> factories = new HashMap<>();
        for (Method m : def.getMethods()) {
            if (!m.getReturnType().equals(type) || (m.getAccessFlags() & 0x8) == 0 || m.getImplementation() == null) continue;
            int ins = insSize(m.getParameterTypes(), true);
            int base = m.getImplementation().getRegisterCount() - ins;
            for (Instruction i : m.getImplementation().getInstructions()) {
                if (i.getOpcode() != Opcode.INVOKE_DIRECT && i.getOpcode() != Opcode.INVOKE_DIRECT_RANGE) continue;
                MethodReference mr = (MethodReference) ((ReferenceInstruction) i).getReference();
                if (!mr.getDefiningClass().equals(type) || !mr.getName().equals("<init>")) continue;
                int[] im = inits.get(key("<init>", mr.getParameterTypes()));
                int[] r = regsOf(i);
                if (im == null || im[0] < 0 || im[1] < 0 || im[0] >= r.length || im[1] >= r.length) continue;
                int n = r[im[0]] - base, o = r[im[1]] - base;
                if (n >= 0 && o >= 0 && n < ins && o < ins) factories.put(key(m.getName(), m.getParameterTypes()), new int[]{n, o});
            }
        }
        Map<Integer, String> strings = new HashMap<>();
        Map<Integer, Integer> ints = new HashMap<>();
        Map<Integer, Object[]> objs = new HashMap<>();
        Object[] lastFactory = null;
        List<Object[]> out = new ArrayList<>();
        for (Instruction ins : impl.getInstructions()) {
            Opcode op = ins.getOpcode();
            if ((op == Opcode.CONST_STRING || op == Opcode.CONST_STRING_JUMBO) && ins instanceof ReferenceInstruction ri) {
                int a = ((OneRegisterInstruction) ins).getRegisterA();
                objs.remove(a); ints.remove(a);
                strings.put(a, ((StringReference) ri.getReference()).getString());
            } else if ((op == Opcode.CONST_4 || op == Opcode.CONST_16 || op == Opcode.CONST) && ins instanceof NarrowLiteralInstruction nl) {
                int a = ((OneRegisterInstruction) ins).getRegisterA();
                objs.remove(a); strings.remove(a);
                ints.put(a, nl.getNarrowLiteral());
            } else if (op == Opcode.NEW_INSTANCE && ins instanceof ReferenceInstruction ri
                    && ri.getReference().toString().equals(type)) {
                int a = ((OneRegisterInstruction) ins).getRegisterA();
                strings.remove(a); ints.remove(a);
                objs.put(a, new Object[]{null, null, null});
            } else if (op == Opcode.MOVE_OBJECT || op == Opcode.MOVE_OBJECT_FROM16 || op == Opcode.MOVE_OBJECT_16
                    || op == Opcode.MOVE || op == Opcode.MOVE_FROM16 || op == Opcode.MOVE_16) {
                int a = ((OneRegisterInstruction) ins).getRegisterA();
                int b = ((org.jf.dexlib2.iface.instruction.TwoRegisterInstruction) ins).getRegisterB();
                copy(objs, a, b); copy(strings, a, b); copy(ints, a, b);
            } else if (op == Opcode.INVOKE_DIRECT || op == Opcode.INVOKE_DIRECT_RANGE) {
                MethodReference mr = (MethodReference) ((ReferenceInstruction) ins).getReference();
                if (!mr.getDefiningClass().equals(type) || !mr.getName().equals("<init>")) continue;
                int[] im = inits.get(key("<init>", mr.getParameterTypes()));
                int[] regs = regsOf(ins);
                if (im == null || im[0] < 0 || im[1] < 0 || im[0] >= regs.length || im[1] >= regs.length) continue;
                String name = strings.get(regs[im[0]]);
                Integer ord = ints.get(regs[im[1]]);
                Object[] o = objs.get(regs[0]);
                if (o != null && name != null && ord != null) { o[1] = name; o[2] = ord; }
            } else if (op == Opcode.INVOKE_STATIC || op == Opcode.INVOKE_STATIC_RANGE) {
                MethodReference mr = (MethodReference) ((ReferenceInstruction) ins).getReference();
                int[] f = mr.getDefiningClass().equals(type) ? factories.get(key(mr.getName(), mr.getParameterTypes())) : null;
                lastFactory = null;
                if (f == null || f[0] < 0 || f[1] < 0) continue;
                int[] regs = regsOf(ins);
                String name = f[0] < regs.length ? strings.get(regs[f[0]]) : null;
                Integer ord = f[1] < regs.length ? ints.get(regs[f[1]]) : null;
                if (name != null && ord != null) lastFactory = new Object[]{null, name, ord};
            } else if (op == Opcode.SGET_OBJECT) {
                // the register now holds some other object; forget what it held
                int a = ((OneRegisterInstruction) ins).getRegisterA();
                objs.remove(a); strings.remove(a); ints.remove(a);
            } else if (op == Opcode.MOVE_RESULT_OBJECT) {
                int a = ((OneRegisterInstruction) ins).getRegisterA();
                strings.remove(a); ints.remove(a);
                if (lastFactory != null) objs.put(a, lastFactory); else objs.remove(a);
                lastFactory = null;
            } else if (op == Opcode.SPUT_OBJECT) {
                FieldReference fr = (FieldReference) ((ReferenceInstruction) ins).getReference();
                Object[] c = objs.get(((OneRegisterInstruction) ins).getRegisterA());
                if (c != null && c[1] != null && c[0] == null && fr.getDefiningClass().equals(type) && fr.getType().equals(type)) {
                    c[0] = fr.getName();
                    if (!containsRef(out, c)) out.add(c);
                }
            } else if ((op == Opcode.FILLED_NEW_ARRAY || op == Opcode.FILLED_NEW_ARRAY_RANGE)
                    && ((ReferenceInstruction) ins).getReference().toString().equals("[" + type)) {
                // R8 keeps a static field only for constants the app reads directly; the rest exist
                // only in the values() array (seen on Instagram 457's like-animation render enum,
                // where only UNRECOGNIZED had a field). Those constants get no field (c[0] null).
                for (int r : regsOf(ins)) {
                    Object[] c = objs.get(r);
                    if (c != null && c[1] != null && !containsRef(out, c)) out.add(c);
                }
            }
        }
        if (out.isEmpty()) return false;
        out.sort((x, y) -> Integer.compare((Integer) x[2], (Integer) y[2]));
        REGISTRY.put(IgClassLoader.toBinary(type), out);
        return true;
    }

    private static boolean containsRef(List<Object[]> list, Object[] c) {
        for (Object[] x : list) if (x == c) return true;
        return false;
    }

    private static int insSize(List<? extends CharSequence> params, boolean isStatic) {
        int n = isStatic ? 0 : 1;
        for (CharSequence pt : params) { String t = pt.toString(); n += (t.equals("J") || t.equals("D")) ? 2 : 1; }
        return n;
    }

    private static int[] regsOf(Instruction ins) {
        if (ins instanceof Instruction35c i) {
            int[] all = {i.getRegisterC(), i.getRegisterD(), i.getRegisterE(), i.getRegisterF(), i.getRegisterG()};
            return java.util.Arrays.copyOf(all, i.getRegisterCount());
        }
        Instruction3rc i = (Instruction3rc) ins;
        int[] r = new int[i.getRegisterCount()];
        for (int k = 0; k < r.length; k++) r[k] = i.getStartRegister() + k;
        return r;
    }

    private static String key(String name, List<? extends CharSequence> params) {
        StringBuilder b = new StringBuilder(name).append('(');
        for (CharSequence c : params) b.append(c.toString());
        return b.append(')').toString();
    }

    private static <V> void copy(Map<Integer, V> m, int to, int from) {
        V v = m.get(from);
        if (v == null) m.remove(to); else m.put(to, v);
    }

    /** Called from the synthesised {@code <clinit>}. */
    public static void init(Class<?> c) {
        List<Object[]> consts = REGISTRY.get(c.getName());
        if (consts == null) return;
        try {
            sun.misc.Unsafe u = unsafe();
            long nameOff = u.objectFieldOffset(Enum.class.getDeclaredField("name"));
            long ordOff = u.objectFieldOffset(Enum.class.getDeclaredField("ordinal"));
            Object arr = Array.newInstance(c, consts.size());
            for (int i = 0; i < consts.size(); i++) {
                Object[] k = consts.get(i);
                Object e = u.allocateInstance(c);
                u.putObject(e, nameOff, k[1]);
                u.putInt(e, ordOff, (Integer) k[2]);
                if (k[0] != null) { // array-only constants have no static field
                    java.lang.reflect.Field f = c.getDeclaredField((String) k[0]);
                    u.putObject(u.staticFieldBase(f), u.staticFieldOffset(f), e);
                }
                Array.set(arr, i, e);
            }
            VALUES.put(c, arr);
        } catch (Throwable t) {
            System.err.println("[hook-verify] enum init " + c.getName() + ": " + t);
        }
    }

    /** Body of the synthesised {@code values()}. */
    public static Object values(Class<?> c) {
        Object v = VALUES.get(c);
        if (v == null) return Array.newInstance(c, 0);
        int n = Array.getLength(v);
        Object copy = Array.newInstance(c, n);
        System.arraycopy(v, 0, copy, 0, n);
        return copy;
    }

    /** Body of the synthesised {@code valueOf(String)}. */
    public static Object valueOf(Class<?> c, String name) {
        Object v = VALUES.get(c);
        if (v != null) for (int i = 0; i < Array.getLength(v); i++) {
            Object e = Array.get(v, i);
            if (((Enum<?>) e).name().equals(name)) return e;
        }
        throw new IllegalArgumentException("No enum constant " + c.getName() + "." + name);
    }

    private static sun.misc.Unsafe unsafe() throws Exception {
        java.lang.reflect.Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        return (sun.misc.Unsafe) f.get(null);
    }
}
