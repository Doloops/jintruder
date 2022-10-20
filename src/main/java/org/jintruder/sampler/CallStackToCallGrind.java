package org.jintruder.sampler;

import java.io.IOException;
import java.io.PrintStream;
import java.text.MessageFormat;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.jintruder.sampler.CallStack.CallStackItem;

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

    private final String protectLocation(CallStack callStack, CallStackItem level)
    {
        String location = callStack.getLocation(level);
        String result = location.replace('<', '_').replace('>', '_').replace('/', '.').replace('$', '_');
        return result;
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

        for (CallStackItem level : callStack.getEntryPoints())
        {
            dump(printStream, callStack, level);
        }
        printStream.close();
    }

    private void dump(PrintStream printStream, CallStack callStack, CallStackItem level)
    {
        printStream.println("fl=" + "Java Class"); // protectClassName(level)
        printStream.println("fn=" + protectLocation(callStack, level));
        printStream.println("0 " + level.getSelfCount());
        for (CallStackItem child : level.getChildren())
        {
            printStream.println("cfl=" + "Java Class"); // protectClassName(child)
            printStream.println("cfn=" + protectLocation(callStack, child));
            printStream.println("calls=" + 1 + " " + 0);

            long timeSpent = child.getCount();
            if (timeSpent <= 0)
            {
                System.err.println("Spurious ! timeSpent=" + timeSpent + " in call : " + callStack.getLocation(level)
                        + "=>" + callStack.getLocation(child));
                timeSpent = 1;
            }
            printStream.println("0 " + timeSpent);
        }
        for (CallStackItem child : level.getChildren())
        {
            dump(printStream, callStack, child);
        }
    }

}
