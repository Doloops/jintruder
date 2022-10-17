package org.jintruder.sampler;

import java.io.IOException;
import java.io.PrintStream;
import java.text.MessageFormat;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.jintruder.sampler.CallStack.CallStackLevel;

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
        printStream.println("var echarts_data=");
        printStream.println("{\"name\":\"echarts\",\"value\":\"0\", \"children\": [");

        boolean first = true;
        for (CallStackLevel level : callStack.getEntryPoints())
        {
            if (first)
                first = false;
            else
                printStream.print(",");
            dump(printStream, level);

        }
        printStream.println("]}");
        printStream.close();
    }

    private void dump(PrintStream printStream, CallStackLevel level)
    {
        printStream.print("{\"name\":\"");
        printStream.print(level.getClassAndMethodName());
        printStream.print("\", \"value\":");
        printStream.print(level.getCount());
        printStream.println(", \"children\":[");

        boolean first = true;
        for (CallStackLevel child : level.getChildren())
        {
            if (first)
                first = false;
            else
                printStream.print(",");
            dump(printStream, child);

        }
        printStream.println("]}");
    }

}
