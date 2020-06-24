package com.arondor.commons.jintruder;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class IntruderClassAdapter extends ClassVisitor
{
    private static final String INTRUDER_TRACKER_CLASS = IntruderTracker.class.getName().replace('.', '/');

    private List<String> tracedMethodBlacklist = new ArrayList<String>();

    public IntruderClassAdapter(ClassVisitor cv)
    {
        super(Opcodes.ASM4, cv);

        tracedMethodBlacklist.add("<clinit>");
        tracedMethodBlacklist.add("<init>");
    }

    private boolean isLog()
    {
        return false;
    }

    private void log(String message)
    {

    }

    private String className;

    @Override
    public void visit(final int version, final int access, final String name, final String signature,
            final String superName, final String[] interfaces)
    {
        className = name;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    private int methodCount = 0;

    private String intruderFieldName = null;

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions)
    {
        MethodVisitor mv;

        intruderFieldName = "INTRUDER_METHODREF_" + methodCount;
        methodCount++;

        cv.visitField(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC, intruderFieldName, "I", null, 0);

        mv = cv.visitMethod(access, name, desc, signature, exceptions);
        if (mv != null)
        {
            if (!tracedMethodBlacklist.contains(name))
            {
                mv = new AddDecorationMethodVisitor(mv, name, signature);
            }
        }
        return mv;
    }

    public class AddDecorationMethodVisitor extends MethodVisitor
    {
        private final String methodName;

        public AddDecorationMethodVisitor(MethodVisitor mv, String methodName, String signature)
        {
            super(Opcodes.ASM8, mv);
            this.methodName = methodName;

            if (isLog())
            {
                log("Decorating " + className + ":" + methodName + ", signature=" + signature);
            }
        }

        // @Override
        // public void visitMaxs(int maxStack, int maxLocals)
        // {
        // mv.visitMaxs(maxStack + 8, maxLocals + 2);
        // }

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
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, INTRUDER_TRACKER_CLASS, "declareMethod",
                    "(Ljava/lang/String;Ljava/lang/String;)I", false);
            mv.visitInsn(Opcodes.DUP);
            mv.visitFieldInsn(Opcodes.PUTSTATIC, className, intruderFieldName, "I");

            mv.visitLabel(label);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, INTRUDER_TRACKER_CLASS, "startMethod", "(I)V", false);
        }

        @Override
        public void visitInsn(int opcode)
        {
            if ((opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN) || opcode == Opcodes.ATHROW)
            {
                mv.visitFieldInsn(Opcodes.GETSTATIC, className, intruderFieldName, "I");
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, INTRUDER_TRACKER_CLASS, "finishMethod", "(I)V", false);
            }
            mv.visitInsn(opcode);
        }
    }

    @Override
    public void visitEnd()
    {
        cv.visitEnd();
    }

}
