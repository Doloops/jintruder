package com.arondor.commons.jintruder;

import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import com.arondor.commons.jintruder.collector.CacheGrindIntruderCollector;
import com.arondor.commons.jintruder.collector.IntruderCollector;

public class IntruderTracker
{
    private static final boolean VERBOSE = false;

    private static final boolean ASYNC_PROCESSING = true;

    private long intruderPeriodicDumpInterval = 0;

    public IntruderTracker()
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

            @Override
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
                    for (TraceEventBucket activeEvent : activeQueue.values())
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

    protected final TraceEventBucket.Visitor traceEventVisitor = new TraceEventBucket.Visitor()
    {
        @Override
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
        private final TraceEventBucket traceEvent;

        private final TraceEventBucket.Visitor visitor;

        public TraceEventProcessor(TraceEventBucket traceEvent, TraceEventBucket.Visitor visitor)
        {
            this.traceEvent = traceEvent;
            this.visitor = visitor;
        }

        @Override
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
    private static IntruderTracker SINGLETON = new IntruderTracker();

    protected static IntruderTracker getIntruderReferenceTracerSingleton()
    {
        return SINGLETON;
    }

    private ThreadLocal<TraceEventBucket> threadLocalEvent = new ThreadLocal<TraceEventBucket>();

    private Map<Thread, TraceEventBucket> activeQueue = new java.util.concurrent.ConcurrentHashMap<Thread, TraceEventBucket>();

    private final void startFinishMethod(int methodId, boolean startOrFinish)
    {
        if (ASYNC_PROCESSING)
        {
            TraceEventBucket traceEvent = threadLocalEvent.get();
            if (traceEvent == null)
            {
                traceEvent = new TraceEventBucket(Thread.currentThread().getId());
                activeQueue.put(Thread.currentThread(), traceEvent);
                threadLocalEvent.set(traceEvent);
            }
            traceEvent.addEvent(methodId, System.nanoTime(), startOrFinish);
            if (traceEvent.isFull())
            {
                activeQueue.remove(Thread.currentThread());
                delayedRegistry.submit(new TraceEventProcessor(traceEvent, traceEventVisitor));
                threadLocalEvent.set(null);

                mayPeriodicDump();
            }
        }
        else
        {
            traceEventVisitor.visit(methodId, Thread.currentThread().getId(), System.nanoTime(), startOrFinish);
            mayPeriodicDump();
        }
    }

    private long lastPeriodicDump = System.currentTimeMillis();

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
