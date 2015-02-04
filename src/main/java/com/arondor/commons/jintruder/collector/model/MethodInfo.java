package com.arondor.commons.jintruder.collector.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MethodInfo
{
    private final String methodName;

    private final ClassInfo parent;

    private long inclusiveTime = 0;

    public MethodInfo(ClassInfo className, String methodName)
    {
        this.parent = className;
        this.methodName = methodName;
    }

    public ClassInfo getClassName()
    {
        return parent;
    }

    public String getMethodName()
    {
        return methodName;
    }

    private Map<MethodInfo, CallInfo> subCalls = new HashMap<MethodInfo, CallInfo>();

    public void addSubCall(MethodInfo subCall)
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

    public Set<Map.Entry<MethodInfo, CallInfo>> getSubCalls()
    {
        return subCalls.entrySet();
    }

    public long getPrivateTime()
    {
        long calledTime = 0;
        for (CallInfo allCalled : subCalls.values())
        {
            calledTime += allCalled.getTimeSpent();
        }
        if (inclusiveTime < 0)
        {
            System.err.println("Spurious ! getPrivateTime() inclusiveTime=" + inclusiveTime + " at " + this);
        }
        long result = inclusiveTime - calledTime;

        if (result < 0)
        {
            System.err.println("Spurious ! getPrivateTime() result=" + result + " at " + this + ", calledTime="
                    + calledTime + ", inclusiveTime=" + inclusiveTime);
            result = 1;
        }
        return result;
    }

    public void appendInclusiveTime(long timeSpent)
    {
        inclusiveTime += timeSpent;
    }

    public void appendCallerTime(MethodInfo methodCall, long timeSpent)
    {
        CallInfo callInfo = subCalls.get(methodCall);
        if (callInfo == null)
        {
            System.err.println("SPURIOUS ! this=" + this + " is no caller of " + methodName);
            return;
        }
        callInfo.appendCalledTime(timeSpent);
    }
}
