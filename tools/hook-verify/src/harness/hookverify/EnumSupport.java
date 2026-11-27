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
        Map<Integer, String> strings = new HashMap<>();
        Map<Integer, Integer> ints = new HashMap<>();
        Map<Integer, Object[]> objs = new HashMap<>();
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
                List<? extends CharSequence> p = mr.getParameterTypes();
                if (p.size() < 2 || !p.get(0).toString().equals("Ljava/lang/String;") || !p.get(1).toString().equals("I")) continue;
                int[] regs;
                if (ins instanceof Instruction35c i) regs = new int[]{i.getRegisterC(), i.getRegisterD(), i.getRegisterE()};
                else { Instruction3rc i = (Instruction3rc) ins; int s = i.getStartRegister(); regs = new int[]{s, s + 1, s + 2}; }
                String name = strings.get(regs[1]);
                Integer ord = ints.get(regs[2]);
                Object[] o = objs.get(regs[0]);
                if (o != null && name != null && ord != null) { o[1] = name; o[2] = ord; }
            } else if (op == Opcode.SPUT_OBJECT) {
                FieldReference fr = (FieldReference) ((ReferenceInstruction) ins).getReference();
                Object[] c = objs.get(((OneRegisterInstruction) ins).getRegisterA());
                if (c != null && c[1] != null && c[0] == null && fr.getDefiningClass().equals(type) && fr.getType().equals(type)) {
                    c[0] = fr.getName();
                    out.add(c);
                }
            }
        }
        if (out.isEmpty()) return false;
        out.sort((a, b) -> Integer.compare((Integer) a[2], (Integer) b[2]));
        REGISTRY.put(IgClassLoader.toBinary(type), out);
        return true;
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
                java.lang.reflect.Field f = c.getDeclaredField((String) k[0]);
                u.putObject(u.staticFieldBase(f), u.staticFieldOffset(f), e);
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
