package com.jintruder.sampler;

import java.io.IOException;
import java.io.PrintStream;

import com.jintruder.sampler.CallStack.CallStackLevel;

public class CallStackToCallGrind
{
    private final String protectMethodName(CallStackLevel level)
    {
        String methodName = level.getMethodName();
        String result = methodName.replace('<', '_').replace('>', '_');
        return result;
    }

    private final String protectClassName(CallStackLevel level)
    {
        String className = level.getClassName();
        return className.replace('/', '.').replace('$', '_');
    }

    public void dumpAll(CallStack callStack)
    {
        long epoch = System.currentTimeMillis();
        String dumpFile = "callgrind.out." + epoch;
        System.err.println("[JINTRUDER] : Dumping events to : " + dumpFile);
        try
        {
            dump(dumpFile, callStack);
        }
        catch (IOException e)
        {
            System.err.println("[JINTRUDER] : Could not dump to file " + dumpFile + " : " + e.getMessage());
            e.printStackTrace(System.err);
        }

    }

    private void dump(String fileName, CallStack callStack) throws IOException
    {
        PrintStream printStream = new PrintStream(fileName);
        printStream.println("version: 1");
        printStream.println("");
        printStream.println("events: ticks");
        printStream.println("");

        for (CallStackLevel level : callStack.getEntryPoints())
        {
            dump(printStream, level);
        }
        printStream.close();
    }

    private void dump(PrintStream printStream, CallStackLevel level)
    {
        printStream.println("fl=" + protectClassName(level));
        printStream.println("fn=" + protectMethodName(level));
        printStream.println("0 " + level.getSelfCount());
        for (CallStackLevel child : level.getChildren())
        {
            printStream.println("cfl=" + protectClassName(child));
            printStream.println("cfn=" + protectMethodName(child));
            printStream.println("calls=" + 1 + " " + 0);

            long timeSpent = child.getCount();
            if (timeSpent <= 0)
            {
                System.err.println("Spurious ! timeSpent=" + timeSpent + " in call : " + level.getClassAndMethodName()
                        + "=>" + child.getClassAndMethodName());
                timeSpent = 1;
            }
            printStream.println("0 " + timeSpent);
        }
        for (CallStackLevel child : level.getChildren())
        {
            dump(printStream, child);
        }
    }
}
