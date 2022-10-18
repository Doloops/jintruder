package org.jintruder.sampler;

public class CallStackPrettyPrinter
{
    public static String prettyPrintByEntryPoint(CallStack classMap)
    {
        StringBuilder builder = new StringBuilder();

        for (CallStack.CallStackItem entryPoint : classMap.getEntryPoints())
        {
            builder.append(entryPoint.getLocation());
            builder.append(' ');
            builder.append(entryPoint.getCount());
            builder.append('\n');

            prettyPrintCallStackLevel(builder, entryPoint, 0);
        }
        return builder.toString();
    }

    private static void prettyPrintCallStackLevel(StringBuilder builder, CallStack.CallStackItem level, int depth)
    {
        for (CallStack.CallStackItem child : level.getChildren())
        {
            for (int d = 0; d < depth; d++)
            {
                builder.append("*  ");
            }
            builder.append(child.getLocation());
            builder.append(' ');
            builder.append(child.getCount());
            builder.append('\n');

            prettyPrintCallStackLevel(builder, child, depth + 1);

        }
    }
}
