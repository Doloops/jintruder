package com.arondor.commons.jintruder.collector.model;

public class MethodStackItem
{
    private final MethodCall methodCall;

    private final long startTime;

    public MethodStackItem(MethodCall methodCall, long startTime)
    {
        this.methodCall = methodCall;
        this.startTime = startTime;
    }

    public MethodCall getMethodCall()
    {
        return methodCall;
    }

    public long getStartTime()
    {
        return startTime;
    }
}
