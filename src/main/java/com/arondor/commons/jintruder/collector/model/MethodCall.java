package com.arondor.commons.jintruder.collector.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MethodCall
{
    private final String methodName;

    private final ClassName parent;

    private long inclusiveTime = 0;

    public MethodCall(ClassName className, String methodName)
    {
        this.parent = className;
        this.methodName = methodName;
    }

    public ClassName getClassName()
    {
        return parent;
    }

    public String getMethodName()
    {
        return methodName;
    }

    public static class CallInfo
    {
        private long number = 0;

        private long timeSpent = 0;

        public long getNumber()
        {
            return number;
        }

        public void setNumber(int number)
        {
            this.number = number;
        }

        public long getTimeSpent()
        {
            return timeSpent;
        }

        public void appendCalledTime(long timeSpent)
        {

            this.timeSpent += timeSpent;
        }

        public void addCalled()
        {
            this.number++;
        }
    }

    private Map<MethodCall, CallInfo> subCalls = new HashMap<MethodCall, CallInfo>();

    public void addSubCall(MethodCall subCall)
    {
        CallInfo callInfo = subCalls.get(subCall);
        if (callInfo == null)
        {
            callInfo = new CallInfo();
            subCalls.put(subCall, callInfo);
        }
        callInfo.addCalled();
    }

    public String toString()
    {
        return parent.getClassName() + ":" + getMethodName();
    }

    public Set<Map.Entry<MethodCall, CallInfo>> getSubCalls()
    {
        return subCalls.entrySet();
    }

    // private Map<Long, Long> pidCounter = new HashMap<Long, Long>();
    //
    // public long getCounter(long pid)
    // {
    // Long counter = pidCounter.get(pid);
    // if ( counter == null )
    // {
    // return Long.MIN_VALUE;
    // }
    // return counter;
    // }
    //
    // public void setCounter(long pid, long counter)
    // {
    // pidCounter.put(pid, counter);
    // }

    public long getPrivateTime()
    {
        long calledTime = 0;
        for (CallInfo allCalled : subCalls.values())
        {
            calledTime += allCalled.getTimeSpent();
        }
        if (inclusiveTime < 0)
        {
            System.err.println("Spurious! getPrivateTime() inclusiveTime=" + inclusiveTime + " at " + this);
        }
        long result = inclusiveTime - calledTime;

        if (result < 0)
        {
            System.err.println("Spurious! getPrivateTime() result=" + result + " at " + this + ", calledTime="
                    + calledTime + ", inclusiveTime=" + inclusiveTime);
            result = 1;
        }
        return result;
    }

    public void appendInclusiveTime(long timeSpent)
    {
        inclusiveTime += timeSpent;
    }

    public void appendCallerTime(MethodCall methodCall, long timeSpent)
    {
        CallInfo callInfo = subCalls.get(methodCall);
        if (callInfo == null)
        {
            throw new IllegalArgumentException("this=" + this + " is no caller of " + methodName);
        }
        callInfo.appendCalledTime(timeSpent);
    }
}
