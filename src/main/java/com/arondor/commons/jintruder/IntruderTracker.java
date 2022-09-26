package com.arondor.commons.jintruder;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import com.arondor.commons.jintruder.collector.IntruderCollector;
import com.arondor.commons.jintruder.collector.MemIntruderCollector;
import com.arondor.commons.jintruder.sink.CacheGrindSink;
import com.arondor.commons.jintruder.sink.IntruderSink;

public class IntruderTracker
{
    private static final boolean VERBOSE = false;

    private static final boolean DUMP_EVENTS = false;

    private static final boolean ASYNC_PROCESSING = true;

    private final long startTime = System.nanoTime();

    private long intruderPeriodicDumpInterval = 0;

    private final IntruderCollector intruderCollector = new MemIntruderCollector();

    private final IntruderSink intruderSink = new CacheGrindSink();

    public IntruderTracker()
    {
        String sInterval = System.getProperty("jintruder.dumpInterval");
        if (sInterval != null)
        {
            intruderPeriodicDumpInterval = Integer.parseInt(sInterval) * 1000;
        }
        log("Delayed interval, setting at=" + intruderPeriodicDumpInterval);

        delayedRegistryExecutor = new ScheduledThreadPoolExecutor(1);
        delayedRegistryExecutor.setThreadFactory(new ThreadFactory()
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
                delayedRegistryExecutor.shutdown();

                boolean processActiveQueues = true;
                try
                {
                    Thread.sleep(10);
                    if (!delayedRegistryExecutor.awaitTermination(180, TimeUnit.SECONDS))
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

                intruderSink.dumpAll(intruderCollector.getClassMap());
            }
        });
    }

    private static void log(String message)
    {
        System.err.println(message);
    }

    private synchronized int doDeclareMethod(String className, String methodName)
    {
        return intruderCollector.registerMethodReference(className, methodName);
    }

    protected final TraceEventBucket.Visitor traceEventVisitor = new TraceEventBucket.Visitor()
    {
        @Override
        public void visit(int methodReference, long threadId, long time, boolean enter)
        {
            if (DUMP_EVENTS)
            {
                log((time - startTime) + " [" + threadId + "] " + (enter ? "enter" : "exit") + " "
                        + intruderCollector.getMethodName(methodReference));
            }
            intruderCollector.addCall(time, threadId, enter, methodReference);
        }
    };

    private void visitBucket(TraceEventBucket bucket, TraceEventBucket.Visitor visitor)
    {
        long start = System.currentTimeMillis();
        bucket.visit(visitor);
        long end = System.currentTimeMillis();
        if (VERBOSE)
        {
            log("[INTRUDER] : Processed " + bucket.size() + " events in " + (end - start) + "ms");
        }
        synchronized (IntruderTracker.this)
        {
            if (recycledBuckets.size() < MAX_RECYCLED_BUCKETS)
            {
                recycledBuckets.add(bucket);
            }
            else
            {
                log("[INTRUDER] Dropping bucket because " + recycledBuckets.size() + " recycled, "
                        + delayedRegistryExecutor.getQueue().size() + " buckets in queue.");
            }
        }
    }

    private final ScheduledThreadPoolExecutor delayedRegistryExecutor;

    private final ThreadLocal<TraceEventBucket> threadLocalEvent = new ThreadLocal<TraceEventBucket>();

    private final Map<Thread, TraceEventBucket> activeQueue = new java.util.concurrent.ConcurrentHashMap<Thread, TraceEventBucket>();

    private final List<TraceEventBucket> recycledBuckets = new LinkedList<TraceEventBucket>();

    private static final int MAX_RECYCLED_BUCKETS = 256;

    private final void startFinishMethod(int methodId, boolean startOrFinish)
    {
        Thread currentThread = Thread.currentThread();
        long threadId = currentThread.getId();
        if (ASYNC_PROCESSING)
        {
            TraceEventBucket bucket = threadLocalEvent.get();
            if (bucket == null)
            {
                synchronized (this)
                {
                    if (!recycledBuckets.isEmpty())
                    {
                        bucket = recycledBuckets.remove(0);
                        bucket.reuse(threadId);
                    }
                }
                if (bucket == null)
                {
                    bucket = new TraceEventBucket(threadId);
                }
                activeQueue.put(currentThread, bucket);
                threadLocalEvent.set(bucket);
            }
            else
            {
                if (bucket.getThreadId() != threadId)
                {
                    throw new IllegalArgumentException("Invalid bucket thread ! current=" + threadId
                            + " but bucket has theadId=" + bucket.getThreadId());
                }
            }
            bucket.addEvent(methodId, System.nanoTime(), startOrFinish);
            if (bucket.isFull())
            {
                threadLocalEvent.set(null);
                activeQueue.remove(currentThread);

                final TraceEventBucket finalBucket = bucket;
                delayedRegistryExecutor.submit(() -> visitBucket(finalBucket, traceEventVisitor));

                // log("[INTRUDER] delayed scheduler " +
                // delayedRegistryExecutor.getQueue().size() + ", recycled
                // buckets "
                // + recycledBuckets.size());

                mayPeriodicDump();
            }
        }
        else
        {
            traceEventVisitor.visit(methodId, threadId, System.nanoTime(), startOrFinish);
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
            delayedRegistryExecutor.submit(new Runnable()
            {
                @Override
                public void run()
                {
                    log("Delayed interval, dump now !");
                    intruderSink.dumpAll(intruderCollector.getClassMap());
                }
            });

        }
    }

    /**
     * Singleton and static call part
     */
    private static final IntruderTracker SINGLETON = new IntruderTracker();

    protected static IntruderTracker getIntruderReferenceTracerSingleton()
    {
        return SINGLETON;
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
