package org.jintruder.sampler;

public class CallStackPrettyPrinter
{
    public static String prettyPrintByEntryPoint(CallStack callStack)
    {
        StringBuilder builder = new StringBuilder();

        for (CallStack.CallStackItem entryPoint : callStack.getEntryPoints())
        {
            builder.append(callStack.getLocation(entryPoint));
            builder.append(' ');
            builder.append(entryPoint.getCount());
            builder.append('\n');

            prettyPrintCallStackLevel(builder, callStack, entryPoint, 0);
        }
        return builder.toString();
    }

    private static void prettyPrintCallStackLevel(StringBuilder builder, CallStack callStack,
            CallStack.CallStackItem level, int depth)
    {
        for (CallStack.CallStackItem child : level.getChildren())
        {
            for (int d = 0; d < depth; d++)
            {
                builder.append("*  ");
            }
            builder.append(callStack.getLocation(level));
            builder.append(' ');
            builder.append(child.getCount());
            builder.append('\n');

            prettyPrintCallStackLevel(builder, callStack, child, depth + 1);

        }
    }
}
