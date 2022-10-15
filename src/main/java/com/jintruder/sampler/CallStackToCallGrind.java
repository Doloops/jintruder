package com.jintruder.sampler;

import java.io.IOException;
import java.io.PrintStream;
import java.text.MessageFormat;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.jintruder.sampler.CallStack.CallStackLevel;

public class CallStackToCallGrind
{
    private static final long EPOCH = System.currentTimeMillis();

    private static final String DUMP_FILE_PATH = "callgrind.out." + EPOCH;

    private static void log(String pattern, Object... vars)
    {
        System.err.println(MessageFormat.format(pattern, vars));
    }

    private static void debug(String pattern, Object... vars)
    {
    }

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

    public void dumpPeriodically(ScheduledExecutorService scheduler, CallStack callStack, int intervalMs)
    {
        log("Dumping callstack to {0} each {1}ms", DUMP_FILE_PATH, intervalMs);
        scheduler.scheduleAtFixedRate(() -> {
            synchronized (callStack)
            {
                dumpAll(callStack);
            }
        }, 0, intervalMs, TimeUnit.MILLISECONDS);
    }

    public void dumpAll(CallStack callStack)
    {
        debug("[JINTRUDER] : Dumping events to : {0}", DUMP_FILE_PATH);
        try
        {
            dump(DUMP_FILE_PATH, callStack);
        }
        catch (IOException e)
        {
            log("[JINTRUDER] : Could not dump to file {0} because {1} ({2})", DUMP_FILE_PATH, e.getClass().getName(),
                    e.getMessage());
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
