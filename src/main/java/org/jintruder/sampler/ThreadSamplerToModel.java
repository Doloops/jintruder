package org.jintruder.sampler;

import java.text.MessageFormat;

import org.jintruder.model.ClassInfo;
import org.jintruder.model.ClassMap;
import org.jintruder.model.MethodInfo;

public class ThreadSamplerToModel
{
    private static final boolean VERBOSE = false;

    private static void log(String pattern, Object... vars)
    {
        System.err.println(MessageFormat.format(pattern, vars));
    }

    public void mergeStackTrace(StackTraceElement[] stackTrace, ClassMap classMap)
    {
        MethodInfo previousMethod = null;
        for (int index = stackTrace.length - 1; index > 0; index--)
        {
            int depth = stackTrace.length - index - 1;
            StackTraceElement element = stackTrace[index];
            String className = element.getClassName();
            int lineNumber = 0; // element.getLineNumber();
            String methodName = element.getMethodName();

            if (VERBOSE)
                log("Stack [{0}] {1}:{2}:{3}", depth, className, methodName, lineNumber);

            ClassInfo currentClass = classMap.findClass(className);
            MethodInfo currentMethod;

            if (previousMethod == null)
            {
                currentMethod = currentClass.findMethod(methodName);
                if (currentMethod == null)
                {
                    currentMethod = currentClass.addMethod(0, methodName);
                }
                classMap.addEntryPoint(currentMethod);
            }
            else
            {
                currentMethod = previousMethod.addSubCall(currentClass, methodName, lineNumber, depth);
                currentClass.addMethod(currentMethod);
            }
            currentMethod.appendInclusiveTime(1);
            previousMethod = currentMethod;
        }

    }

    public void watch(Thread newThread, ClassMap classMap, long intervalMs)
    {
        final long intervalNano = intervalMs * 1_000_000;
        Thread watchThread = new Thread()
        {
            @Override
            public void run()
            {
                long threadStart = System.nanoTime();
                long samples = 0;
                while (true)
                {
                    long start = System.nanoTime();
                    if (newThread.getState() == State.NEW)
                    {
                        threadStart = start;
                    }
                    else if (newThread.getState() == State.TERMINATED)
                    {
                        long threadLife = System.nanoTime() - threadStart;
                        log("Watched thread for {0}ns, took {1} samples", threadLife, samples);
                        return;
                    }
                    else
                    {
                        mergeStackTrace(newThread.getStackTrace(), classMap);
                        samples++;
                    }
                    long took = System.nanoTime() - start;
                    try
                    {
                        long remaining = intervalNano - took;
                        if (remaining <= 0)
                            continue;
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
