package org.jintruder.instrument;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class JintruderClassAdapter extends ClassVisitor
{
    private static final String INTRUDER_TRACKER_CLASS = JintruderTracker.class.getName().replace('.', '/');

    private List<String> tracedMethodBlacklist = new ArrayList<String>();

    public JintruderClassAdapter(ClassVisitor cv)
    {
        super(Opcodes.ASM9, cv);

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
        private final Label start = new Label(), end = new Label(), handler = new Label();

        private final int methodId;

        public AddDecorationMethodVisitor(MethodVisitor mv, String methodName, String signature)
        {
            super(Opcodes.ASM8, mv);
            this.methodId = JintruderTracker.declareMethod(className, methodName);

            if (isLog())
            {
                log("Decorating " + className + ":" + methodName + ", signature=" + signature);
            }
        }

        @Override
        public void visitCode()
        {
            mv.visitLdcInsn(methodId);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, INTRUDER_TRACKER_CLASS, "startFinishMethod", "(I)V", false);

            mv.visitLabel(start);
            mv.visitCode();
        }

        @Override
        public void visitMaxs(int maxStack, int maxLocals)
        {
            mv.visitTryCatchBlock(start, end, end, null);
            mv.visitLabel(end);

            mv.visitLdcInsn(-methodId);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, INTRUDER_TRACKER_CLASS, "startFinishMethod", "(I)V", false);
            mv.visitInsn(Opcodes.ATHROW);
            // visit the corresponding instructions
            super.visitMaxs(maxStack + 8, maxLocals + 2);
        }

        @Override
        public void visitInsn(int opcode)
        {
            if (opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN)
            {
                mv.visitLdcInsn(-methodId);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, INTRUDER_TRACKER_CLASS, "startFinishMethod", "(I)V", false);
                mv.visitInsn(opcode);
            }
            else if (opcode == Opcodes.ATHROW)
            {
                mv.visitInsn(opcode);
            }
            else
            {
                mv.visitInsn(opcode);
            }
        }
    }

    @Override
    public void visitEnd()
    {
        cv.visitEnd();
    }

}
