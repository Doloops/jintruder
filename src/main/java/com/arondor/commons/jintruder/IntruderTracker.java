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
    private static final boolean VERBOSE = false;

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
                    System.err.println("Buckets active=" + activeBuckets.size() + ", fromDeadThreads="
                            + bucketsFromDeadThreads.size() + ", queued=" + queuedBuckets.size() + ", recycled="
                            + recycledBuckets.size());
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
    };

    private volatile long ticker = System.nanoTime() - BIRTH_TIME;

    private final Thread tickerThread = new Thread()
    {
        @Override
        public void run()
        {
            while (true)
            {
                ticker = System.nanoTime() - BIRTH_TIME;
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

        tickerThread.setName("IntruderTicker");
        tickerThread.setDaemon(true);
        tickerThread.start();

        backgroundThread.setName("IntruderBackgroundThread");
        backgroundThread.setDaemon(true);

        Runtime.getRuntime().addShutdownHook(new Thread()
        {
            @Override
            public void run()
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
        intruderCollector.processBucket(bucket);
        synchronized (IntruderTracker.this)
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

    private final void doAddCallMethod(int methodId)
    {
        if (queuedBuckets.size() > MAX_QUEUED_BUCKETS)
            return;

        TraceEventBucket bucket = findCurrentBucket();
        bucket.addEvent(methodId, ticker);
        if (bucket.isFull())
        {
            pushFullBucket(bucket);
        }
    }

    private void pushFullBucket(TraceEventBucket bucket)
    {
        activeBuckets.remove(Thread.currentThread());

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
            synchronized (this)
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

    /**
     * Fast start/finish method decoration calls
     * 
     * @param methodId
     *            positive for method start, negative for method end
     */
    public static final void startFinishMethod(int methodId)
    {
        SINGLETON.doAddCallMethod(methodId);
    }

    public static final ClassMap getClassMap()
    {
        synchronized (SINGLETON.backgroundThread)
        {
            SINGLETON.backgroundThread.notify();
        }
        SINGLETON.processActiveQueues();
        try
        {
            Thread.sleep(100);
        }
        catch (InterruptedException e)
        {
        }
        return SINGLETON.intruderCollector.getClassMap();
    }

    public static void reset()
    {
        getClassMap().clear();
    }
}
