package com.arondor.commons.jintruder;

public class TraceEventBucket
{
    public static final int DEPTH_SIZE = 1024; // 32 * 1024 * 1024;

    private final long threadId;

    private final long timeArray[] = new long[DEPTH_SIZE];

    private final int methodArray[] = new int[DEPTH_SIZE];

    private int cursor = 0;

    public TraceEventBucket(long threadId)
    {
        this.threadId = threadId;
    }

    public final void addEvent(int methodReference, long time, boolean enter)
    {
        if (isFull())
        {
            throw new IllegalStateException("TraceEventBucket already full for thread " + threadId);
        }
        this.timeArray[cursor] = time;
        this.methodArray[cursor] = enter ? methodReference : -methodReference;
        this.cursor++;
    }

    public final boolean isFull()
    {
        return cursor == DEPTH_SIZE;
    }

    public static interface Visitor
    {
        public void visit(int methodReference, long threadId, long time, boolean enter);
    }

    public final void visit(Visitor visitor)
    {
        for (int index = 0; index < cursor; index++)
        {
            visitor.visit(Math.abs(methodArray[index]), threadId, timeArray[index], methodArray[index] >= 0);
        }
    }

    public final int size()
    {
        return cursor;
    }

    public final long getThreadId()
    {
        return threadId;
    }
}
