package com.arondor.commons.jintruder;

public class TraceEventBucket
{
    public static final int DEPTH_SIZE = 1024; // 32 * 1024 * 1024;

    private long pid;

    private final long timeArray[] = new long[DEPTH_SIZE];

    private final int methodArray[] = new int[DEPTH_SIZE];

    private int cursor = 0;

    public TraceEventBucket(long pid)
    {
        this.pid = pid;
    }

    public final void addEvent(int methodReference, long time, boolean enter)
    {
        this.timeArray[cursor] = time;
        this.methodArray[cursor] = enter ? methodReference : -methodReference;
        this.cursor++;
    }

    public final boolean isFull()
    {
        return cursor == DEPTH_SIZE;
    }

    public final void reset(long newPid)
    {
        this.cursor = 0;
        this.pid = newPid;
    }

    public static interface Visitor
    {
        public void visit(int methodReference, long pid, long time, boolean enter);
    }

    public final void visit(Visitor visitor)
    {
        for (int index = 0; index < cursor; index++)
        {
            visitor.visit(Math.abs(methodArray[index]), pid, timeArray[index], methodArray[index] >= 0);
        }
    }

    public final int size()
    {
        return cursor;
    }
}
