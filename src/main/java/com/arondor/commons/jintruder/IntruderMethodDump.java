package com.arondor.commons.jintruder;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public class IntruderMethodDump
{
    public void log(String message)
    {

    }

    public void dumpMethod(MethodNode mn)
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

}
