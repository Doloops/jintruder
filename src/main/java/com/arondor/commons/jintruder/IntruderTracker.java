package com.arondor.commons.jintruder;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.arondor.commons.jintruder.collector.IntruderCollector;
import com.arondor.commons.jintruder.collector.MemIntruderCollector;
import com.arondor.commons.jintruder.collector.model.ClassMap;
import com.arondor.commons.jintruder.sink.CacheGrindSink;
import com.arondor.commons.jintruder.sink.IntruderSink;

public class IntruderTracker
{
    private static final boolean VERBOSE = true;

    private static final boolean DROP_FULL_BUCKETS = false;

    private static final int MAX_RECYCLED_BUCKETS = 8192;

    private static final int MAX_QUEUED_BUCKETS = 1_000_000;

    private static final long BIRTH_TIME = System.nanoTime();

    private long intruderPeriodicDumpInterval = 0;

    private final IntruderCollector intruderCollector = new MemIntruderCollector();

    private final IntruderSink intruderSink = new CacheGrindSink();

    private final List<TraceEventBucket> queuedBuckets = new LinkedList<TraceEventBucket>();

    private final Map<Thread, TraceEventBucket> activeBuckets = new java.util.concurrent.ConcurrentHashMap<Thread, TraceEventBucket>();

    private final List<TraceEventBucket> recycledBuckets = new LinkedList<TraceEventBucket>();

    private boolean shutdown = false;

    private static void log(String message)
    {
        System.err.println(message);
    }

    private static void debug(String string)
    {
    }

    private Thread backgroundThread;

    private static volatile long TICKER = System.nanoTime() - BIRTH_TIME;

    private static final Thread TICKER_THREAD = new Thread()
    {
        @Override
        public void run()
        {
            while (true)
            {
                TICKER = System.nanoTime() - BIRTH_TIME;
                try
                {
                    Thread.sleep(0, 100);
                }
                catch (InterruptedException e)
                {
                }
            }
        }
    };

    public IntruderTracker()
    {
        String sInterval = System.getProperty("jintruder.dumpInterval");
        if (sInterval != null)
        {
            intruderPeriodicDumpInterval = Integer.parseInt(sInterval) * 1000;
            log("Periodic Dump Interval set to " + intruderPeriodicDumpInterval + "ms");
        }

        TICKER_THREAD.setName("IntruderTicker");
        TICKER_THREAD.setDaemon(true);
        TICKER_THREAD.start();

        Runtime.getRuntime().addShutdownHook(new Thread()
        {
            @Override
            public void run()
            {
                stopBackgroundThread();

                intruderSink.dumpAll(intruderCollector.getClassMap());
            }
        });

        startBackgroundThread();
    }

    private synchronized int doDeclareMethod(String className, String methodName)
    {
        return intruderCollector.registerMethodReference(className, methodName);
    }

    private void processBucket(TraceEventBucket bucket)
    {
        intruderCollector.processBucket(bucket);
        synchronized (recycledBuckets)
        {
            if (recycledBuckets.size() < MAX_RECYCLED_BUCKETS)
            {
                recycledBuckets.add(bucket);
            }
            else
            {
                debug("Dropping bucket because " + recycledBuckets.size() + " recycled, " + queuedBuckets.size()
                        + " buckets in queue.");
            }
        }
    }

    private static long lastErrorMessage = 0;

    private static final long SPAM_PERIOD = 10_000;

    private static void tellTheWorldIAmOverwhelmed()
    {
        long now = System.currentTimeMillis();
        if (now - lastErrorMessage > SPAM_PERIOD)
        {
            log("Saturated Buckets ! active=" + SINGLETON.activeBuckets.size() + ", queued="
                    + SINGLETON.queuedBuckets.size() + ", recycled=" + SINGLETON.recycledBuckets.size());
            lastErrorMessage = now;
        }
    }

    private void pushFullBucket(TraceEventBucket bucket)
    {
        activeBuckets.remove(Thread.currentThread());

        if (DROP_FULL_BUCKETS)
        {
            synchronized (recycledBuckets)
            {
                recycledBuckets.add(bucket);
            }
            return;
        }

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

    private TraceEventBucket findCurrentBucket()
    {
        Thread currentThread = Thread.currentThread();
        long threadId = currentThread.getId();

        TraceEventBucket bucket = activeBuckets.get(currentThread);
        if (bucket == null)
        {
            synchronized (recycledBuckets)
            {
                if (bucket == null && !recycledBuckets.isEmpty())
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
        }
        else
        {
            if (bucket.getThreadId() != threadId)
            {
                throw new IllegalArgumentException("Invalid bucket thread ! current=" + threadId
                        + " but bucket has theadId=" + bucket.getThreadId());
            }
        }
        return bucket;
    }

    private long lastPeriodicDump = System.currentTimeMillis();

    private synchronized void mayPeriodicDump()
    {
        long now = System.currentTimeMillis();
        if (intruderPeriodicDumpInterval != 0 && (now - lastPeriodicDump > intruderPeriodicDumpInterval))
        {
            log("Periodic dump : now=" + now + ", last=" + lastPeriodicDump);
            lastPeriodicDump = now;
            intruderSink.dumpAll(intruderCollector.getClassMap());
        }
    }

    private void processActiveQueues()
    {
        log("Processing " + activeBuckets.size() + " per-thread active queues...");
        for (TraceEventBucket activeBucket : activeBuckets.values())
        {
            intruderCollector.processBucket(activeBucket);
            activeBucket.reset();
        }
        activeBuckets.clear();
    }

    private void runBackgroundThread()
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

            /*
             * First, we must cleanup all buckets attached to dead threads.
             */
            List<TraceEventBucket> bucketsFromDeadThreads = new ArrayList<TraceEventBucket>();
            synchronized (this)
            {
                for (Map.Entry<Thread, TraceEventBucket> entry : activeBuckets.entrySet())
                {
                    if (!entry.getKey().isAlive())
                    {
                        TraceEventBucket bucket = entry.getValue();
                        activeBuckets.remove(entry.getKey());
                        bucketsFromDeadThreads.add(bucket);
                    }
                }
            }

            if (VERBOSE)
            {
                log("Buckets active=" + activeBuckets.size() + ", fromDeadThreads=" + bucketsFromDeadThreads.size()
                        + ", queued=" + queuedBuckets.size() + ", recycled=" + recycledBuckets.size());
            }

            /*
             * Then we process all the queued buckets
             */
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

            /*
             * And we finish by all the buckets from the dead threads,
             * considering the previous buckets from these threads were
             * processed from the queued list.
             */
            for (TraceEventBucket bucket : bucketsFromDeadThreads)
                processBucket(bucket);

            mayPeriodicDump();
        }
    }

    public final void startBackgroundThread()
    {
        shutdown = false;
        backgroundThread = new Thread()
        {
            @Override
            public void run()
            {
                runBackgroundThread();
            }
        };
        backgroundThread.setName("IntruderBackgroundThread");
        backgroundThread.setDaemon(true);
        backgroundThread.start();
    }

    private void stopBackgroundThread()
    {
        log("Shutting down backgroundThread ...");
        shutdown = true;
        boolean processActiveQueues = true;
        try
        {
            Thread.sleep(10);
            backgroundThread.join(10_000);
        }
        catch (InterruptedException e)
        {
            log("Could not wait backgroundThread: " + e.getMessage());
            processActiveQueues = false;
        }
        if (processActiveQueues)
        {
            processActiveQueues();
        }
        else
        {
            log("Will not process active queues due to previous errors.");
        }
    }

    /**
     * Singleton and static call part
     */
    private static final IntruderTracker SINGLETON = new IntruderTracker();

    public final ClassMap doGetClassMap()
    {
        stopBackgroundThread();
        ClassMap classMap = intruderCollector.getClassMap();
        startBackgroundThread();
        return classMap;
    }

    /*
     * Public API
     */
    public static int declareMethod(String className, String methodName)
    {
        return SINGLETON.doDeclareMethod(className, methodName);
    }

    /**
     * Fast start/finish method decoration calls
     * 
     * @param methodId
     *            positive for method start, negative for method end
     */
    public static final void startFinishMethod(int methodId)
    {
        if (SINGLETON.queuedBuckets.size() > MAX_QUEUED_BUCKETS)
        {
            tellTheWorldIAmOverwhelmed();
            return;
        }

        TraceEventBucket bucket = SINGLETON.findCurrentBucket();
        TraceEventBucket._addEvent(bucket, methodId, TICKER);
        if (TraceEventBucket._isFull(bucket))
        {
            SINGLETON.pushFullBucket(bucket);
        }
    }

    public static final ClassMap getClassMap()
    {
        return SINGLETON.doGetClassMap();
    }

    public static void reset()
    {
        getClassMap().clear();
    }
}
