package com.jintruder.sampler;

import java.text.MessageFormat;
import java.util.concurrent.atomic.AtomicLong;

import com.jintruder.sampler.CallStack.CallStackLevel;

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
        for (int index = stackTrace.length - 1; index > 0; index--)
        {
            int depth = stackTrace.length - index - 1;
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

    public void watch(Thread newThread, CallStack callStack, long intervalMs)
    {
        final long intervalNano = intervalMs * 1_000_000;
        final AtomicLong totalTook = new AtomicLong();
        Thread watchThread = new Thread()
        {
            @Override
            public void run()
            {
                long threadStart = 0;
                long threadLast = 0;
                long samples = 0;
                while (true)
                {
                    long start = System.nanoTime();
                    if (newThread.getState() == State.NEW)
                    {
                    }
                    else if (newThread.getState() == State.TERMINATED)
                    {
                        long threadLife = threadLast - threadStart;
                        log("Watched thread for {0}ns, took {1} samples, total time to capture {2}ns, average {3}ns per sample",
                                threadLife, samples, totalTook.get(), totalTook.get() / samples);
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
                    long took = System.nanoTime() - start;
                    totalTook.addAndGet(took);
                    try
                    {
                        long remaining = (intervalNano - took) / 100L;
                        if (remaining <= 100L)
                            continue;
                        log("remaining {0}ns", remaining);
                        long remainingMs = remaining / 1_000_000L;
                        long remainingNs = remaining % 1_000_000L;
                        Thread.sleep(remainingMs, (int) remainingNs);
                    }
                    catch (InterruptedException e)
                    {
                    }
                }
            }
        };
        watchThread.setName("Watching-" + newThread.getName());
        watchThread.start();
    }
}
