package org.jintruder.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ClassMapPrettyPrinter
{
    private static class CycleDetector extends HashMap<MethodInfo, CycleDetector.Cycle>
    {
        public static class Cycle
        {
            Set<CallInfo> calls = new HashSet<CallInfo>();
        }

        public boolean checkCycles(MethodInfo methodInfo, CallInfo callInfo)
        {
            Cycle cycle = get(methodInfo);
            if (cycle == null)
            {
                put(methodInfo, new Cycle());
            }
            else if (!cycle.calls.contains(callInfo))
            {
                cycle.calls.add(callInfo);
            }
            else
            {
                System.err.println("Cycle from " + methodInfo.getClassAndMethodName() + ":");
                for (Map.Entry<MethodInfo, Cycle> entry : entrySet())
                {
                    if (entry.getKey().equals(methodInfo))
                    {
                        System.err.println("* with " + entry.getKey().getClassAndMethodName());
                    }
                }
                return true;
            }
            return false;
        }
    }

    public static String prettyPrintByEntryPoint(ClassMap classMap)
    {
        StringBuilder builder = new StringBuilder();

        for (MethodInfo entryPoint : classMap.getEntryPoints())
        {
            CycleDetector cycleDetector = new CycleDetector();

            builder.append(entryPoint.getClassAndMethodName());
            builder.append('\n');

            prettyPrintMethodInfo(builder, cycleDetector, entryPoint, 1);
        }
        return builder.toString();
    }

    public static final int MAX_DEPTH = 256;

    private static void prettyPrintMethodInfo(StringBuilder builder, CycleDetector cycleDetector, MethodInfo methodInfo,
            int depth)
    {
        if (depth >= MAX_DEPTH)
            return;
        for (Map.Entry<MethodInfo, CallInfo> call : methodInfo.getSubCalls().entrySet())
        {
            for (int d = 0; d < depth; d++)
            {
                builder.append("*  ");
            }
            MethodInfo childMethodInfo = call.getKey();
            CallInfo childCallInfo = call.getValue();

            builder.append(childMethodInfo.getClassAndMethodName());
            builder.append(':');
            builder.append(childCallInfo.getLineNumber());
            builder.append(' ');
            builder.append(childCallInfo.getNumber());
            if (cycleDetector.checkCycles(childMethodInfo, childCallInfo))
            {
                builder.append(" => Cycle\n");
                return;
            }
            else
            {
                builder.append('\n');
                prettyPrintMethodInfo(builder, cycleDetector, childMethodInfo, depth + 1);
            }
        }
    }
}
