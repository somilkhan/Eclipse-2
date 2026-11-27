package hookverify;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Turns the SDK's android.jar (every body is {@code throw new RuntimeException("Stub!")}) into a
 * jar whose methods return default values — the same idea as AGP's mockable-android.jar
 * ({@code unitTests.returnDefaultValues = true}). Constructors keep their super() call.
 *
 * Usage: MockableJar &lt;android.jar&gt; &lt;out.jar&gt;
 */
public final class MockableJar {
    public static void main(String[] args) throws Exception {
        try (ZipFile in = new ZipFile(args[0]); ZipOutputStream out = new ZipOutputStream(new FileOutputStream(args[1]))) {
            Enumeration<? extends ZipEntry> en = in.entries();
            while (en.hasMoreElements()) {
                ZipEntry e = en.nextElement();
                if (e.isDirectory() || e.getName().startsWith("java/")) continue; // the JDK provides java.*
                byte[] data = read(in.getInputStream(e));
                if (e.getName().endsWith(".class")) data = transform(data);
                out.putNextEntry(new ZipEntry(e.getName()));
                out.write(data);
                out.closeEntry();
            }
        }
    }

    private static byte[] read(InputStream is) throws Exception {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        is.transferTo(bo);
        is.close();
        return bo.toByteArray();
    }

    static byte[] transform(byte[] bytes) {
        ClassNode cn = new ClassNode();
        new ClassReader(bytes).accept(cn, 0);
        for (MethodNode mn : cn.methods) {
            if ((mn.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) != 0 || mn.instructions.size() == 0) continue;
            AbstractInsnNode stub = null;
            for (AbstractInsnNode insn : mn.instructions) {
                if (insn.getOpcode() == Opcodes.NEW && "java/lang/RuntimeException".equals(((TypeInsnNode) insn).desc)) {
                    stub = insn;
                    break;
                }
            }
            if (stub == null) continue;
            while (stub.getNext() != null) mn.instructions.remove(stub.getNext());
            Type ret = Type.getReturnType(mn.desc);
            switch (ret.getSort()) {
                case Type.VOID: mn.instructions.set(stub, new InsnNode(Opcodes.RETURN)); break;
                case Type.LONG: mn.instructions.insertBefore(stub, new InsnNode(Opcodes.LCONST_0)); mn.instructions.set(stub, new InsnNode(Opcodes.LRETURN)); break;
                case Type.FLOAT: mn.instructions.insertBefore(stub, new InsnNode(Opcodes.FCONST_0)); mn.instructions.set(stub, new InsnNode(Opcodes.FRETURN)); break;
                case Type.DOUBLE: mn.instructions.insertBefore(stub, new InsnNode(Opcodes.DCONST_0)); mn.instructions.set(stub, new InsnNode(Opcodes.DRETURN)); break;
                case Type.ARRAY: case Type.OBJECT: mn.instructions.insertBefore(stub, new InsnNode(Opcodes.ACONST_NULL)); mn.instructions.set(stub, new InsnNode(Opcodes.ARETURN)); break;
                default: mn.instructions.insertBefore(stub, new InsnNode(Opcodes.ICONST_0)); mn.instructions.set(stub, new InsnNode(Opcodes.IRETURN)); break;
            }
            mn.tryCatchBlocks.clear();
            if (mn.localVariables != null) mn.localVariables.clear();
        }
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        return cw.toByteArray();
    }
}
