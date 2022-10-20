package org.jintruder.sampler;

import java.io.IOException;
import java.io.PrintStream;
import java.text.MessageFormat;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.jintruder.sampler.CallStack.CallStackItem;

public class CallStackToJson
{
    private static final long EPOCH = System.currentTimeMillis();

    private static final String DUMP_FILE_PATH = "echarts.json";

    private static void log(String pattern, Object... vars)
    {
        System.err.println(MessageFormat.format(pattern, vars));
    }

    private static void debug(String pattern, Object... vars)
    {
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
        printStream.println("var echarts_data=");
        printStream.println("{\"name\":\"echarts\",\"value\":\"0\", \"children\": [");

        boolean first = true;
        for (CallStackItem level : callStack.getEntryPoints())
        {
            if (first)
                first = false;
            else
                printStream.print(",");
            dump(printStream, callStack, level);

        }
        printStream.println("]}");
        printStream.close();
    }

    private void dump(PrintStream printStream, CallStack callStack, CallStackItem level)
    {
        printStream.print("{\"name\":\"");
        printStream.print(callStack.getLocation(level));
        printStream.print("\", \"value\":");
        printStream.print(level.getCount());
        printStream.println(", \"children\":[");

        boolean first = true;
        for (CallStackItem child : level.getChildren())
        {
            if (first)
                first = false;
            else
                printStream.print(",");
            dump(printStream, callStack, child);

        }
        printStream.println("]}");
    }

}
