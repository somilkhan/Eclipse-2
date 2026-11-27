package hookverify;

import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.DexFile;
import org.jf.dexlib2.iface.Field;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.MultiDexContainer;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.objectweb.asm.Opcodes.*;

/**
 * Stands in for Instagram's PathClassLoader on a host JVM. Every class defined in the APK's
 * dex files is synthesised on demand as a JVM class with the same name, superclass, interfaces,
 * fields and method signatures (bodies just throw), so reflection — {@code Class.forName},
 * {@code getDeclaredMethods}, DexKit's {@code getMethodInstance} — behaves as on the device.
 * Framework classes ({@code android.*}, {@code java.*}) come from the parent (mockable android.jar
 * + JDK), mirroring ART's boot-classpath-first delegation.
 */
public final class IgClassLoader extends ClassLoader {
    static { registerAsParallelCapable(); }

    private final Map<String, ClassDef> defs = new HashMap<>(1 << 18);
    private final ClassLoader framework;

    public IgClassLoader(File apk, ClassLoader framework) throws Exception {
        super(framework);
        this.framework = framework;
        MultiDexContainer<? extends DexFile> container = DexFileFactory.loadDexContainer(apk, Opcodes.forApi(34));
        List<String> names = new ArrayList<>(container.getDexEntryNames());
        // classes.dex, classes2.dex, ... — ART resolves duplicates in this order.
        names.sort((a, b) -> Integer.compare(dexIndex(a), dexIndex(b)));
        for (String n : names) {
            DexFile dex = container.getEntry(n).getDexFile();
            for (ClassDef c : dex.getClasses()) {
                String bin = toBinary(c.getType());
                defs.putIfAbsent(bin, c);
            }
        }
    }

    private static int dexIndex(String n) {
        String d = n.replaceAll("\\D", "");
        return d.isEmpty() ? 1 : Integer.parseInt(d);
    }

    public int classCount() { return defs.size(); }

    static String toBinary(String type) { // Lcom/a/B; -> com.a.B
        return type.substring(1, type.length() - 1).replace('/', '.');
    }

    static String toInternal(String type) { // Lcom/a/B; -> com/a/B
        return type.substring(1, type.length() - 1);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> c = findLoadedClass(name);
            if (c != null) return c;
            if (name.startsWith("java.")) return framework.loadClass(name);
            ClassDef def = defs.get(name);
            if (def == null || name.startsWith("android.") || name.startsWith("dalvik.")) {
                try {
                    return framework.loadClass(name);
                } catch (ClassNotFoundException e) {
                    if (def == null) throw e;
                }
            }
            byte[] bytes = synthesise(def);
            return defineClass(name, bytes, 0, bytes.length);
        }
    }

    private Class<?> tryLoad(String type) {
        try {
            return loadClass(toBinary(type), false);
        } catch (ClassNotFoundException | LinkageError e) {
            return null;
        }
    }

    private static String internal0(ClassDef def) { return toInternal(def.getType()); }

    private byte[] synthesise(ClassDef def) {
        int dexFlags = def.getAccessFlags();
        boolean isInterface = (dexFlags & ACC_INTERFACE) != 0;
        int access = dexFlags & (ACC_PUBLIC | ACC_FINAL | ACC_INTERFACE | ACC_ABSTRACT | ACC_SYNTHETIC | ACC_ANNOTATION | ACC_ENUM);
        if (isInterface) access |= ACC_ABSTRACT; else access |= ACC_SUPER;
        access |= ACC_PUBLIC; // package-private is irrelevant for the harness and avoids cross-loader access trouble

        String superName = "java/lang/Object";
        if (!isInterface && def.getSuperclass() != null) {
            Class<?> sup = tryLoad(def.getSuperclass());
            if (sup != null && !sup.isInterface() && !java.lang.reflect.Modifier.isFinal(sup.getModifiers()))
                superName = toInternal(def.getSuperclass());
        }
        List<String> ifaces = new ArrayList<>();
        for (String i : def.getInterfaces()) {
            Class<?> ic = tryLoad(i);
            if (ic != null && ic.isInterface()) ifaces.add(toInternal(i));
        }

        ClassWriter cw = new ClassWriter(0);
        cw.visit(V11, access, internal0(def), null, superName, ifaces.toArray(new String[0]));

        List<Field> fields = new ArrayList<>();
        for (Field f : def.getStaticFields()) fields.add(f);
        for (Field f : def.getInstanceFields()) fields.add(f);
        for (Field f : fields) {
            int fa = f.getAccessFlags() & (ACC_PUBLIC | ACC_PRIVATE | ACC_PROTECTED | ACC_STATIC | ACC_FINAL
                    | ACC_VOLATILE | ACC_TRANSIENT | ACC_SYNTHETIC | ACC_ENUM);
            if (isInterface) fa = ACC_PUBLIC | ACC_STATIC | ACC_FINAL;
            cw.visitField(fa, f.getName(), f.getType(), null, null).visitEnd();
        }

        String internal = toInternal(def.getType());
        boolean realEnum = "java/lang/Enum".equals(superName) && EnumSupport.register(def);
        if (realEnum) {
            MethodVisitor ci = cw.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
            ci.visitCode();
            ci.visitLdcInsn(org.objectweb.asm.Type.getObjectType(internal));
            ci.visitMethodInsn(INVOKESTATIC, "hookverify/EnumSupport", "init", "(Ljava/lang/Class;)V", false);
            ci.visitInsn(RETURN);
            ci.visitMaxs(1, 0);
            ci.visitEnd();
        }

        for (Method m : def.getMethods()) {
            String name = m.getName();
            if (name.equals("<clinit>")) continue;
            StringBuilder desc = new StringBuilder("(");
            int slots = 0;
            for (CharSequence p : m.getParameterTypes()) {
                String ps = p.toString();
                desc.append(ps);
                slots += (ps.equals("J") || ps.equals("D")) ? 2 : 1;
            }
            desc.append(')').append(m.getReturnType());
            int ma = m.getAccessFlags() & (ACC_PUBLIC | ACC_PRIVATE | ACC_PROTECTED | ACC_STATIC | ACC_FINAL
                    | ACC_SYNCHRONIZED | ACC_BRIDGE | ACC_VARARGS | ACC_NATIVE | ACC_ABSTRACT | ACC_SYNTHETIC);
            // Mirror libcore Executable.fixMethodFlags: dex marks non-native synchronized methods
            // ACC_DECLARED_SYNCHRONIZED (0x20000) and ART reflection reports exactly those as
            // Modifier.SYNCHRONIZED. Without this every isSynchronized() filter fails offline.
            ma &= ~ACC_SYNCHRONIZED;
            if ((m.getAccessFlags() & 0x20000) != 0) ma |= ACC_SYNCHRONIZED;
            if (name.equals("<init>")) ma &= ~(ACC_STATIC | ACC_FINAL | ACC_SYNCHRONIZED | ACC_BRIDGE | ACC_NATIVE | ACC_ABSTRACT);
            if (isInterface && (ma & ACC_ABSTRACT) != 0) ma = (ma & ~(ACC_PRIVATE | ACC_PROTECTED | ACC_FINAL)) | ACC_PUBLIC;
            if (!isInterface && (access & ACC_ABSTRACT) == 0) ma &= ~ACC_ABSTRACT; // keep the class instantiable-shaped
            MethodVisitor mv = cw.visitMethod(ma, name, desc.toString(), null, null);
            boolean isStatic = (ma & ACC_STATIC) != 0;
            if (realEnum && isStatic && name.equals("values") && desc.toString().equals("()[L" + internal + ";")) {
                mv.visitCode();
                mv.visitLdcInsn(org.objectweb.asm.Type.getObjectType(internal));
                mv.visitMethodInsn(INVOKESTATIC, "hookverify/EnumSupport", "values", "(Ljava/lang/Class;)Ljava/lang/Object;", false);
                mv.visitTypeInsn(CHECKCAST, "[L" + internal + ";");
                mv.visitInsn(ARETURN);
                mv.visitMaxs(1, 0);
            } else if (realEnum && isStatic && name.equals("valueOf") && desc.toString().equals("(Ljava/lang/String;)L" + internal + ";")) {
                mv.visitCode();
                mv.visitLdcInsn(org.objectweb.asm.Type.getObjectType(internal));
                mv.visitVarInsn(ALOAD, 0);
                mv.visitMethodInsn(INVOKESTATIC, "hookverify/EnumSupport", "valueOf", "(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/Object;", false);
                mv.visitTypeInsn(CHECKCAST, internal);
                mv.visitInsn(ARETURN);
                mv.visitMaxs(2, 1);
            } else if ((ma & (ACC_ABSTRACT | ACC_NATIVE)) == 0) {
                mv.visitCode();
                mv.visitTypeInsn(NEW, "java/lang/UnsupportedOperationException");
                mv.visitInsn(DUP);
                mv.visitLdcInsn("hook-verify stub: Instagram code does not run on the host JVM");
                mv.visitMethodInsn(INVOKESPECIAL, "java/lang/UnsupportedOperationException", "<init>", "(Ljava/lang/String;)V", false);
                mv.visitInsn(ATHROW);
                mv.visitMaxs(3, slots + ((ma & ACC_STATIC) != 0 ? 0 : 1));
            }
            mv.visitEnd();
        }
        cw.visitEnd();
        return cw.toByteArray();
    }
}
