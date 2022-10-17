package org.jintruder.sampler;

import java.lang.Thread.State;
import java.text.MessageFormat;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.jintruder.sampler.CallStack.CallStackLevel;

public class ThreadSamplerToCallStack
{
    private static final boolean VERBOSE = false;

    private static void log(String pattern, Object... vars)
    {
        System.err.println(MessageFormat.format(pattern, vars));
    }

    public void mergeStackTrace(StackTraceElement[] stackTrace, CallStack callStack)
    {
        CallStackLevel current = null;
        for (int index = stackTrace.length - 1; index >= 0; index--)
        {
            int depth = stackTrace.length - index;
            StackTraceElement element = stackTrace[index];
            String className = element.getClassName();
            String methodName = element.getMethodName();

            if (VERBOSE)
                log("Stack [{0}] {1}:{2}:{3}", depth, className, methodName);

            if (current == null)
            {
                current = callStack.addEntryPoint(className, methodName, depth);
            }
            else
            {
                current = current.addChild(className, methodName, depth);
            }
            current.incrementCount();
        }
    }

    public void watchSingleThread(Thread newThread, CallStack callStack, long intervalMs)
    {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(new Runnable()
        {
            private long threadStart = 0;

            private long threadLast = 0;

            private long samples = 0;

            private long totalTook = 0;

            @Override
            public void run()
            {
                long start = System.nanoTime();
                if (newThread.getState() == State.NEW)
                {
                }
                else if (newThread.getState() == State.TERMINATED)
                {
                    long threadLife = threadLast - threadStart;
                    log("Watched thread for {0}ns, took {1} samples, total time to capture {2}ns, average {3}ns per sample",
                            threadLife, samples, totalTook, totalTook / samples);
                    scheduler.shutdown();
                    return;
                }
                else
                {
                    mergeStackTrace(newThread.getStackTrace(), callStack);
                    samples++;
                }
                if (threadStart == 0)
                {
                    threadStart = start;
                }
                threadLast = start;
                totalTook += System.nanoTime() - start;
            }
        }, 0, intervalMs, TimeUnit.MILLISECONDS);
    }

    public void watchMultipleThreads(ScheduledExecutorService scheduler, String pattern, long intervalMs,
            CallStack callStack)
    {
        scheduler.scheduleAtFixedRate(() -> {
            synchronized (callStack)
            {
                for (Map.Entry<Thread, StackTraceElement[]> entry : Thread.getAllStackTraces().entrySet())
                {
                    mergeStackTrace(entry.getValue(), callStack);
                }
            }

        }, 0, intervalMs, TimeUnit.MILLISECONDS);
    }

}
