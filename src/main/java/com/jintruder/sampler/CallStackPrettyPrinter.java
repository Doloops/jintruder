package com.jintruder.sampler;

public class CallStackPrettyPrinter
{
    public static String prettyPrintByEntryPoint(CallStack classMap)
    {
        StringBuilder builder = new StringBuilder();

        for (CallStack.CallStackLevel entryPoint : classMap.getEntryPoints())
        {
            builder.append(entryPoint.getClassAndMethodName());
            builder.append('\n');

            prettyPrintCallStackLevel(builder, entryPoint);
        }
        return builder.toString();
    }

    private static void prettyPrintCallStackLevel(StringBuilder builder, CallStack.CallStackLevel level)
    {
        for (CallStack.CallStackLevel child : level.getChildren())
        {
            for (int d = 0; d < child.getDepth(); d++)
            {
                builder.append("*  ");
            }
            builder.append(child.getClassAndMethodName());
            builder.append(' ');
            builder.append(child.getCount());
            builder.append('\n');

            prettyPrintCallStackLevel(builder, child);

        }
    }
}
