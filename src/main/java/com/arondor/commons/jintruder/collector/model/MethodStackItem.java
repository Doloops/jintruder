package com.arondor.commons.jintruder.collector.model;

public class MethodStackItem
{
    private final MethodInfo methodCall;

    private final long startTime;

    public MethodStackItem(MethodInfo methodCall, long startTime)
    {
        this.methodCall = methodCall;
        this.startTime = startTime;
    }

    public MethodInfo getMethodCall()
    {
        return methodCall;
    }

    public long getStartTime()
    {
        return startTime;
    }
}
