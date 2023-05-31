package org.jintruder.sampler;

import java.lang.Thread.State;
import java.text.MessageFormat;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.jintruder.model.sampler.CallStack;
import org.jintruder.model.sampler.StackTraceFilter;
import org.jintruder.model.sampler.CallStack.CallStackItem;

public class ThreadSamplerToCallStack
{
    private static final boolean VERBOSE = false;

    private static void log(String pattern, Object... vars)
    {
        System.err.println(MessageFormat.format(pattern, vars));
    }

    public void mergeStackTrace(StackTraceElement[] stackTrace, CallStack callStack)
    {
        mergeStackTrace(stackTrace, callStack, new StackTraceFilter(null, null, null));
    }

    public void mergeStackTrace(StackTraceElement[] stackTrace, CallStack callStack, StackTraceFilter filter)
    {
        CallStackItem current = null;
        boolean selectedStack = !filter.hasRequiredMethod();
        for (int index = stackTrace.length - 1; index >= 0; index--)
        {
            StackTraceElement element = stackTrace[index];
            String location = element.getClassName() + ":" + element.getMethodName();

            if (!selectedStack && filter.isRequiredMethod(location))
                selectedStack = true;
            if (!selectedStack)
                continue;
            if (filter.isSkippedMethod(location))
                continue;

            if (VERBOSE)
                log("Stack [{0}] {1}", index, location);

            if (current == null)
            {
                current = callStack.addEntryPoint(location);
            }
            else
            {
                current = callStack.addChild(current, location);
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

    private static final int INITIAL_THREADS_ARRAY_SIZE = 256;

    private Thread allThreads[] = new Thread[INITIAL_THREADS_ARRAY_SIZE];

    public void watchMultipleThreads(ScheduledExecutorService scheduler, StackTraceFilter filter, long intervalMs,
            CallStack callStack)
    {
        scheduler.scheduleAtFixedRate(() -> {
            try
            {
                int totalThreads = Thread.enumerate(allThreads);
                if (totalThreads == allThreads.length)
                {
                    allThreads = new Thread[allThreads.length * 2];
                    Thread.enumerate(allThreads);
                }
                for (Thread thread : allThreads)
                {
                    if (!filter.filterThread(thread))
                        continue;
                    if (thread.getName().startsWith("jintruder"))
                        continue;
                    StackTraceElement[] stack = thread.getStackTrace();
                    if (VERBOSE)
                        log("Taken stack of {0} elements for thread {1}", stack.length, thread.getName());
                    synchronized (callStack)
                    {
                        mergeStackTrace(stack, callStack, filter);
                    }
                }
            }
            catch (RuntimeException | Error e)
            {
                log("Caught exception " + e.getClass().getName());
                e.printStackTrace();
            }
            catch (Throwable e)
            {
                log("Caught exception " + e.getClass().getName());
                e.printStackTrace();
            }
        }, 0, intervalMs, TimeUnit.MILLISECONDS);
    }

}
