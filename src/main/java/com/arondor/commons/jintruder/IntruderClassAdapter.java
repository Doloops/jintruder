package com.arondor.commons.jintruder;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.ClassVisitor;
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

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions)
    {
        MethodVisitor mv;

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

        @Override
        public void visitCode()
        {
            int methodId = IntruderTracker.declareMethod(className, methodName);

            mv.visitCode();
            mv.visitLdcInsn(methodId);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, INTRUDER_TRACKER_CLASS, "startMethod", "(I)V", false);
        }

        @Override
        public void visitInsn(int opcode)
        {
            if ((opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN) || opcode == Opcodes.ATHROW)
            {
                int methodId = IntruderTracker.declareMethod(className, methodName);

                mv.visitLdcInsn(methodId);
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
