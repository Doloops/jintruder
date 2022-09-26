package com.arondor.commons.jintruder;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.arondor.commons.jintruder.collector.IntruderCollector;
import com.arondor.commons.jintruder.collector.MemIntruderCollector;
import com.arondor.commons.jintruder.sink.CacheGrindSink;
import com.arondor.commons.jintruder.sink.IntruderSink;

public class IntruderTracker
{
    private static final boolean VERBOSE = false;

    private static final int MAX_RECYCLED_BUCKETS = 8192;

    private static final int MAX_QUEUED_BUCKETS = 1_000_000;

    private long intruderPeriodicDumpInterval = 0;

    private final IntruderCollector intruderCollector = new MemIntruderCollector();

    private final IntruderSink intruderSink = new CacheGrindSink();

    private final List<TraceEventBucket> queuedBuckets = new LinkedList<TraceEventBucket>();

    private final ThreadLocal<TraceEventBucket> threadLocalEvent = new ThreadLocal<TraceEventBucket>();

    private final Map<Thread, TraceEventBucket> activeBuckets = new java.util.concurrent.ConcurrentHashMap<Thread, TraceEventBucket>();

    private final List<TraceEventBucket> recycledBuckets = new LinkedList<TraceEventBucket>();

    private boolean shutdown = false;

    private static void log(String message)
    {
        System.err.println(message);
    }

    private final Thread backgroundThread = new Thread()
    {
        @Override
        public void run()
        {
            while (!shutdown)
            {
                synchronized (backgroundThread)
                {
                    try
                    {
                        backgroundThread.wait(100);
                    }
                    catch (InterruptedException e)
                    {
                    }
                }

                while (true)
                {
                    TraceEventBucket head;
                    synchronized (queuedBuckets)
                    {
                        if (queuedBuckets.isEmpty())
                            break;
                        head = queuedBuckets.remove(0);
                    }

                    processBucket(head);
                }
                mayPeriodicDump();
            }
        }
    };

    public IntruderTracker()
    {
        String sInterval = System.getProperty("jintruder.dumpInterval");
        if (sInterval != null)
        {
            intruderPeriodicDumpInterval = Integer.parseInt(sInterval) * 1000;
        }
        log("Delayed interval, setting at=" + intruderPeriodicDumpInterval);

        backgroundThread.setName("IntruderBackgroundThread");
        backgroundThread.setDaemon(true);

        Runtime.getRuntime().addShutdownHook(new Thread()
        {
            @Override
            public void run()
            {
                log("[INTRUDER] : Shutting down backgroundThread ...");
                shutdown = true;
                boolean processActiveQueues = true;
                try
                {
                    Thread.sleep(10);
                    backgroundThread.join(10_000);
                }
                catch (InterruptedException e)
                {
                    log("[INTRUDER] : Could not wait backgroundThread: " + e.getMessage());
                    processActiveQueues = false;
                }
                if (processActiveQueues)
                {
                    log("[INTRUDER] : Processing " + activeBuckets.size() + " per-thread active queues...");
                    for (TraceEventBucket activeEvent : activeBuckets.values())
                    {
                        intruderCollector.processBucket(activeEvent);
                    }
                }
                else
                {
                    log("[INTRUDER] : will not process active queues due to previous errors.");
                }

                intruderSink.dumpAll(intruderCollector.getClassMap());
            }
        });
        backgroundThread.start();
    }

    private synchronized int doDeclareMethod(String className, String methodName)
    {
        return intruderCollector.registerMethodReference(className, methodName);
    }

    private void processBucket(TraceEventBucket bucket)
    {
        long start = System.currentTimeMillis();
        intruderCollector.processBucket(bucket);
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
                        + queuedBuckets.size() + " buckets in queue.");
            }
        }
    }

    private final void startFinishMethod(int methodId, boolean startOrFinish)
    {
        Thread currentThread = Thread.currentThread();
        long threadId = currentThread.getId();

        if (queuedBuckets.size() > MAX_QUEUED_BUCKETS)
            return;
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
            activeBuckets.put(currentThread, bucket);
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
            activeBuckets.remove(currentThread);

            boolean mayNotify = false;
            synchronized (queuedBuckets)
            {
                queuedBuckets.add(bucket);
                if (queuedBuckets.size() == MAX_QUEUED_BUCKETS)
                {
                    log("Queued buckets reached maximum " + MAX_QUEUED_BUCKETS + ", recyled=" + recycledBuckets.size()
                            + ", active=" + activeBuckets.size());
                    mayNotify = true;
                }
            }

            if (mayNotify)
            {
                synchronized (backgroundThread)
                {
                    backgroundThread.notify();
                }
            }
        }
    }

    private long lastPeriodicDump = System.currentTimeMillis();

    private synchronized void mayPeriodicDump()
    {
        long now = System.currentTimeMillis();
        if (intruderPeriodicDumpInterval != 0 && (now - lastPeriodicDump > intruderPeriodicDumpInterval))
        {
            log("Delayed interval, now=" + now + ", last=" + lastPeriodicDump);
            lastPeriodicDump = now;
            intruderSink.dumpAll(intruderCollector.getClassMap());
        }
    }

    /**
     * Singleton and static call part
     */
    private static final IntruderTracker SINGLETON = new IntruderTracker();

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
