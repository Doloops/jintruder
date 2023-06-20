package org.jintruder.sampler;

import org.jintruder.model.sampler.CallStack;

public class CallStackPrettyPrinter
{
    public static String prettyPrintByEntryPoint(CallStack callStack)
    {
        StringBuilder builder = new StringBuilder();

        int count = 0;
        for (CallStack.CallStackItem entryPoint : callStack.getEntryPoints())
        {
            builder.append('[');
            builder.append(count++);
            builder.append("] ");
            builder.append(callStack.getLocation(entryPoint));
            builder.append(' ');
            builder.append(entryPoint.getCount());
            long self = entryPoint.getSelfCount();
            if (self > 0)
            {
                builder.append(" (self: ");
                builder.append(self);
                builder.append(')');
            }
            builder.append('\n');

            prettyPrintCallStackLevel(builder, callStack, entryPoint, 1);
        }
        return builder.toString();
    }

    private static void prettyPrintCallStackLevel(StringBuilder builder, CallStack callStack,
            CallStack.CallStackItem level, int depth)
    {
        int count = 0;
        for (CallStack.CallStackItem child : level.getChildren())
        {
            for (int d = 0; d < depth; d++)
            {
                builder.append("*  ");
            }
            builder.append('[');
            builder.append(count++);
            builder.append("] ");
            builder.append(callStack.getLocation(child));
            builder.append(' ');
            builder.append(child.getCount());
            long self = child.getSelfCount();
            if (self > 0)
            {
                builder.append(" (self: ");
                builder.append(self);
                builder.append(')');
            }
            builder.append('\n');

            prettyPrintCallStackLevel(builder, callStack, child, depth + 1);
        }
    }
}
