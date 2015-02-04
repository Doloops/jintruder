package com.arondor.commons.jintruder;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public class IntruderTransformer implements ClassFileTransformer
{
    public static void premain(String agentArgs, Instrumentation inst)
    {
        inst.addTransformer(new IntruderTransformer());
    }

    private List<String> tracedClassPrefixes = new ArrayList<String>();

    private List<String> tracedClassBlacklist = new ArrayList<String>();

    private List<String> tracedRegexBlacklist = new ArrayList<String>();

    private List<String> untracedMethods = new ArrayList<String>();

    public static final String intruderTracerClass = IntruderReferenceTracer.class.getName().replace('.', '/');

    public static boolean INTRUDER_NO_DECORATION = false;

    public static boolean INTRUDER_LOG = false;

    private boolean getBooleanProperty(String name)
    {
        String sValue = System.getProperty(name);
        if (sValue != null)
        {
            return sValue.trim().equalsIgnoreCase("true");
        }
        return false;
    }

    public IntruderTransformer()
    {
        untracedMethods.add("<clinit>");
        untracedMethods.add("<init>");

        INTRUDER_NO_DECORATION = getBooleanProperty("jintruder.nodecoration");
        INTRUDER_LOG = getBooleanProperty("jintruder.log");
        String intruderClasses = System.getProperty("jintruder.classes");
        if (intruderClasses == null || intruderClasses.trim().isEmpty())
        {
            intruderClasses = "com:org";
        }
        intruderClasses = intruderClasses.replace('.', '/');
        for (String clazz : intruderClasses.split(":"))
        {
            tracedClassPrefixes.add(clazz);
            if (isLog())
            {
                log("Tracing prefix : " + clazz);
            }
        }

        tracedClassBlacklist.add("org/objectweb");
        tracedClassBlacklist.add("org/xml");
        tracedClassBlacklist.add("com/arondor/common/management");
        tracedClassBlacklist.add("com/arondor/common/jintruder");
        tracedClassBlacklist.add("com/sun");

        tracedRegexBlacklist.add(".*CGLIB.*");
    }

    private synchronized final boolean isClassTraced(final String className)
    {
        if (className.startsWith("com/arondor/commons/jintruder") || className.startsWith("java/")
                || className.startsWith("sun/"))
        {
            return false;
        }

        int lastIndex = className.lastIndexOf('/');
        if (lastIndex == -1)
        {
            lastIndex = className.length();
        }
        String packageName = className.substring(0, lastIndex);

        if (packageName.equals("com/arondor/commons/jintruder")
                || packageName.equals("com/arondor/common/jintruder/parser"))
        {
            return false;
        }

        if (INTRUDER_NO_DECORATION)
        {
            return false;
        }

        boolean traced = doIsClassTraced(className, packageName);
        return traced;
    }

    private boolean doIsClassTraced(final String className, String packageName)
    {
        for (String prefix : tracedClassPrefixes)
        {
            if (className.startsWith(prefix))
            {
                for (String blacklist : tracedClassBlacklist)
                {
                    if (className.startsWith(blacklist))
                    {
                        return false;
                    }
                }
                for (String blacklistRegex : tracedRegexBlacklist)
                {
                    if (isLog())
                    {
                        log("Matching pattern '" + blacklistRegex + "' against '" + className + "'");
                    }
                    Pattern pattern = Pattern.compile(blacklistRegex);
                    if (pattern.matcher(className).matches())
                    {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException
    {
        byte[] retVal = null;
        if (isClassTraced(className))
        {
            try
            {
                ClassWriter cw = new ClassWriter(0);
                ClassVisitor ca = new MyClassAdapter(cw);
                ClassReader cr = new ClassReader(classfileBuffer);

                if (isLog())
                {
                    log("Transforming Class : " + className + ", Cr access : 0x" + Integer.toHexString(cr.getAccess()));
                }
                if ((cr.getAccess() & (Opcodes.ACC_INTERFACE)) != 0)
                {
                    return null;
                }
                cr.accept(ca, 0);
                retVal = cw.toByteArray();
            }
            catch (RuntimeException e)
            {
                log("Catched : " + e);
                e.printStackTrace();
            }
            catch (Throwable e)
            {
                log("Catched : " + e);
                e.printStackTrace();
            }
            finally
            {
                if (isLog())
                {
                    log("Transformed class : " + className);
                }
            }
        }
        return retVal;
    }

    private final boolean isLog()
    {
        return INTRUDER_LOG;
    }

    private final void log(String message)
    {
        System.err.println(message);
    }

    public class MyClassAdapter extends ClassVisitor
    {
        public MyClassAdapter(ClassVisitor cv)
        {
            super(Opcodes.ASM4, cv);

        }

        protected void dumpMethod(MethodNode mn)
        {
            for (AbstractInsnNode node : mn.instructions.toArray())
            {
                log("Node : " + node.getOpcode() + ", " + node.getType() + " (class:" + node.getClass().getName() + ")");
                if (node instanceof LabelNode)
                {
                    log("* Label : " + ((LabelNode) node).getLabel().toString());
                }
                else if (node instanceof LdcInsnNode)
                {
                    log("* Constant : " + ((LdcInsnNode) node).cst);
                }
                else if (node instanceof VarInsnNode)
                {
                    log("* Var : " + ((VarInsnNode) node).var);
                }
                else if (node instanceof LineNumberNode)
                {
                    log("* LineNumber : " + ((LineNumberNode) node).line);
                }
                else if (node instanceof InsnNode)
                {
                    if (node.getOpcode() >= 172 && node.getOpcode() <= 177)
                    {
                        log("* RETURN");
                    }
                    else if (node.getOpcode() == 87)
                    {
                        log("* POP");
                    }
                    else if (node.getOpcode() == 88)
                    {
                        log("* POP2");
                    }

                }
                else if (node instanceof MethodInsnNode)
                {
                    MethodInsnNode methodInsnNode = (MethodInsnNode) node;
                    log("* Method : " + methodInsnNode.owner + ", name=" + methodInsnNode.name);
                }
                else if (node instanceof FieldInsnNode)
                {
                    FieldInsnNode fieldInsnNode = (FieldInsnNode) node;
                    log("* Field : " + fieldInsnNode.owner + ", name=" + fieldInsnNode.name);
                }
            }
        }

        private int methodCount = 0;

        private String intruderFieldName = null;

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions)
        {
            MethodVisitor mv;

            intruderFieldName = "INTRUDER_METHODREF_" + methodCount;
            methodCount++;

            // log("Method : " + name + ", access=" + access);

            cv.visitField(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC, intruderFieldName, "I", null, 0);

            mv = cv.visitMethod(access, name, desc, signature, exceptions);
            if (mv != null)
            {
                if (!untracedMethods.contains(name))
                {
                    mv = new AddDecorationMethodVisitor(mv, name, signature);
                }
            }

            return mv;
        }

        public class AddDecorationMethodVisitor extends MethodVisitor
        {
            private final String methodName;

            // private final int methodReference;

            public AddDecorationMethodVisitor(MethodVisitor mv, String methodName, String signature)
            {
                super(Opcodes.ASM4, mv);
                this.methodName = methodName;

                if (isLog())
                {
                    log("Decorating " + className + ":" + methodName + ", signature=" + signature);
                }
            }

            @Override
            public void visitMaxs(int maxStack, int maxLocals)
            {
                mv.visitMaxs(maxStack + 8, maxLocals + 2);
            }

            @Override
            public void visitCode()
            {
                mv.visitCode();
                mv.visitFieldInsn(Opcodes.GETSTATIC, className, intruderFieldName, "I");

                mv.visitInsn(Opcodes.DUP);

                Label label = new Label();
                mv.visitJumpInsn(Opcodes.IFNE, label);

                mv.visitInsn(Opcodes.POP);
                mv.visitLdcInsn(className);
                mv.visitLdcInsn(this.methodName);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, intruderTracerClass, "declareMethod",
                        "(Ljava/lang/String;Ljava/lang/String;)I");
                mv.visitInsn(Opcodes.DUP);
                mv.visitFieldInsn(Opcodes.PUTSTATIC, className, intruderFieldName, "I");

                mv.visitLabel(label);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, intruderTracerClass, "startMethod", "(I)V");
            }

            @Override
            public void visitInsn(int opcode)
            {
                if ((opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN) || opcode == Opcodes.ATHROW)
                {
                    mv.visitFieldInsn(Opcodes.GETSTATIC, className, intruderFieldName, "I");
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, intruderTracerClass, "finishMethod", "(I)V");
                }
                mv.visitInsn(opcode);
            }
        }

        @Override
        public void visitEnd()
        {
            cv.visitEnd();
        }

        private String className;

        @Override
        public void visit(final int version, final int access, final String name, final String signature,
                final String superName, final String[] interfaces)
        {
            className = name;
            super.visit(version, access, name, signature, superName, interfaces);
        }
    }
}
