package com.arondor.commons.jintruder;

import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import com.arondor.commons.jintruder.collector.CacheGrindIntruderCollector;
import com.arondor.commons.jintruder.collector.IntruderCollector;

public class IntruderReferenceTracer
{
    private static boolean VERBOSE = false;

    private long intruderPeriodicDumpInterval = 0;

    public IntruderReferenceTracer()
    {
        String sInterval = System.getProperty("jintruder.dumpInterval");
        if (sInterval != null)
        {
            intruderPeriodicDumpInterval = Integer.parseInt(sInterval) * 1000;
        }
        log("Delayed interval, setting at=" + intruderPeriodicDumpInterval);

        delayedRegistry = new ScheduledThreadPoolExecutor(1);
        delayedRegistry.setThreadFactory(new ThreadFactory()
        {

            public Thread newThread(Runnable arg0)
            {
                Thread thread = new Thread(arg0);
                thread.setName("IntruderBackgroundThread");
                thread.setDaemon(true);
                thread.setPriority(Thread.MIN_PRIORITY);
                return thread;
            }
        });

        Runtime.getRuntime().addShutdownHook(new Thread()
        {
            @Override
            public void run()
            {
                log("[INTRUDER] : Shutting down delayedRegistry ...");
                delayedRegistry.shutdown();

                boolean processActiveQueues = true;
                try
                {
                    Thread.sleep(10);
                    if (!delayedRegistry.awaitTermination(180, TimeUnit.SECONDS))
                    {
                        log("[INTRUDER] : Could not wait delayedRegistry : timeout !");
                        processActiveQueues = false;
                    }
                }
                catch (InterruptedException e)
                {
                    log("[INTRUDER] : Could not wait delayedRegistry : " + e.getMessage());
                    processActiveQueues = false;
                }
                if (processActiveQueues)
                {
                    log("[INTRUDER] : Processing " + activeQueue.size() + " per-thread active queues...");
                    for (TraceEvent activeEvent : activeQueue.values())
                    {
                        activeEvent.visit(traceEventVisitor);
                    }
                }
                else
                {
                    log("[INTRUDER] : will not process active queues due to previous errors.");
                }

                intruderCollector.dumpCollection();
            }
        });
    }

    private static void log(String message)
    {
        System.err.println(message);
    }

    private IntruderCollector intruderCollector = new CacheGrindIntruderCollector();

    private synchronized int doDeclareMethod(String className, String methodName)
    {
        return intruderCollector.registerMethodReference(className, methodName);
    }

    private static final boolean DUMP_EVENTS = false;

    protected final TraceEvent.Visitor traceEventVisitor = new TraceEvent.Visitor()
    {
        public void visit(int methodReference, long pid, long time, boolean enter)
        {
            if (DUMP_EVENTS)
            {
                log("time=" + time + ", pid=" + pid + ", enter=" + enter + ", ref=" + methodReference);
            }
            intruderCollector.addCall(time, pid, enter, methodReference);
        }
    };

    protected static class TraceEventProcessor implements Runnable
    {
        private final TraceEvent traceEvent;

        private final TraceEvent.Visitor visitor;

        public TraceEventProcessor(TraceEvent traceEvent, TraceEvent.Visitor visitor)
        {
            this.traceEvent = traceEvent;
            this.visitor = visitor;
        }

        public void run()
        {
            long start = System.currentTimeMillis();
            traceEvent.visit(visitor);
            long end = System.currentTimeMillis();
            if (VERBOSE)
            {
                log("[INTRUDER] : Processed " + traceEvent.size() + " events in " + (end - start) + "ms");
            }
        }
    }

    private ScheduledThreadPoolExecutor delayedRegistry;

    /**
     * Singleton and static call part
     */
    private static IntruderReferenceTracer SINGLETON = new IntruderReferenceTracer();

    protected static IntruderReferenceTracer getIntruderReferenceTracerSingleton()
    {
        return SINGLETON;
    }

    private ThreadLocal<TraceEvent> threadLocalEvent = new ThreadLocal<TraceEvent>();

    private Map<Thread, TraceEvent> activeQueue = new java.util.concurrent.ConcurrentHashMap<Thread, TraceEvent>();

    private final void startFinishMethod(int methodId, boolean startOrFinish)
    {
        TraceEvent traceEvent = threadLocalEvent.get();
        if (traceEvent == null)
        {
            traceEvent = new TraceEvent(Thread.currentThread().getId());
            activeQueue.put(Thread.currentThread(), traceEvent);
            threadLocalEvent.set(traceEvent);
        }
        traceEvent.addEvent(methodId, System.nanoTime(), startOrFinish);
        if (traceEvent.isFull())
        {
            activeQueue.remove(Thread.currentThread());
            delayedRegistry.submit(new TraceEventProcessor(traceEvent, SINGLETON.traceEventVisitor));
            threadLocalEvent.set(null);

            mayPeriodicDump();
        }
    }

    private long lastPeriodicDump = 0;

    private synchronized void mayPeriodicDump()
    {
        long now = System.currentTimeMillis();
        if (intruderPeriodicDumpInterval != 0 && (now - lastPeriodicDump > intruderPeriodicDumpInterval))
        {
            log("Delayed interval,  now=" + now + ", last=" + lastPeriodicDump);
            lastPeriodicDump = now;
            delayedRegistry.submit(new Runnable()
            {
                @Override
                public void run()
                {
                    log("Delayed interval, dump now !");
                    intruderCollector.dumpCollection();
                }
            });

        }
    }

    /*
     * Public API
     */

    public static int declareMethod(String className, String methodName)
    {
        return SINGLETON.doDeclareMethod(className, methodName);
    }

    public static final void startMethod(int methodId)
    {
        SINGLETON.startFinishMethod(methodId, true);
    }

    public static final void finishMethod(int methodId)
    {
        SINGLETON.startFinishMethod(methodId, false);
    }
}
