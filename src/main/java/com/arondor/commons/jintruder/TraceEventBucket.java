package com.arondor.commons.jintruder;

public final class TraceEventBucket
{
    public static final int BUCKET_SIZE = 16 * 1024;

    private long threadId;

    private final long timeArray[] = new long[BUCKET_SIZE];

    private final int methodArray[] = new int[BUCKET_SIZE];

    private int cursor = 0;

    public TraceEventBucket(long threadId)
    {
        this.threadId = threadId;
    }

    public final void addEvent(final int methodReference, final long time)
    {
        int cursor = this.cursor;
        timeArray[cursor] = time;
        methodArray[cursor] = methodReference;
        this.cursor = cursor + 1;
    }

    public static final void __addEvent(final TraceEventBucket bucket, final int methodReference, final long time)
    {
        int cursor = bucket.cursor;
        bucket.timeArray[cursor] = time;
        bucket.methodArray[cursor] = methodReference;
        bucket.cursor = cursor + 1;
    }

    public final void reset()
    {
        cursor = 0;
    }

    public final boolean isFull()
    {
        return cursor == BUCKET_SIZE;
    }

    public final int size()
    {
        return cursor;
    }

    public final long getThreadId()
    {
        return threadId;
    }

    public final void reuse(long threadId)
    {
        this.threadId = threadId;
        this.cursor = 0;
    }

    public final long getTime(int idx)
    {
        return timeArray[idx];
    }

    public final int getMethodId(int idx)
    {
        return Math.abs(methodArray[idx]);
    }

    public final boolean getEnter(int idx)
    {
        return methodArray[idx] > 0;
    }

}
