package com.jintruder.model;

import java.util.HashMap;
import java.util.Map;

public class MethodInfo
{
    private final int id;

    private final String methodName;

    private final ClassInfo parent;

    private long inclusiveTime = 0;

    private long numberOfCalls = 0;

    public MethodInfo(int id, ClassInfo className, String methodName)
    {
        this.id = id;
        this.parent = className;
        this.methodName = methodName;
    }

    public ClassInfo getClassInfo()
    {
        return parent;
    }

    public String getMethodName()
    {
        return methodName;
    }

    public String getClassAndMethodName()
    {
        return parent.getClassName() + ":" + methodName;
    }

    public int getReferenceId()
    {
        return id;
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

    @Override
    public String toString()
    {
        return parent.getClassName() + ":" + getMethodName();
    }

    public Map<MethodInfo, CallInfo> getSubCalls()
    {
        return subCalls;
    }

    public CallInfo getSubCall(String methodName)
    {
        return getSubCall(parent.getClassName(), methodName);
    }

    public CallInfo getSubCall(String className, String methodName)
    {
        for (Map.Entry<MethodInfo, CallInfo> entry : subCalls.entrySet())
        {
            if (entry.getKey().getClassInfo().getClassName().equals(className)
                    && entry.getKey().getMethodName().equals(methodName))
            {
                return entry.getValue();
            }
        }
        throw new IllegalArgumentException("Could not find " + className + "::" + methodName);
    }

    public long getTotalTime()
    {
        return inclusiveTime;
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

    public long getNumberOfCalls()
    {
        return numberOfCalls;
    }

    public void setNumberOfCalls(long numberOfCalls)
    {
        this.numberOfCalls = numberOfCalls;
    }

    public void incrementNumberOfCalls()
    {
        this.numberOfCalls++;
    }

    @Override
    public int hashCode()
    {
        return parent.getClassName().hashCode() + methodName.hashCode();
    }

    @Override
    public boolean equals(Object o)
    {
        if (o instanceof MethodInfo)
        {
            MethodInfo other = (MethodInfo) o;
            /*
             * We use lasy evaluation to compare objects first, which are
             * cheaper to perform than long equals()
             */
            if (parent != other.parent && !parent.getClassName().equals(other.parent.getClassName()))
                return false;
            if (methodName != other.methodName && !methodName.equals(other.methodName))
                return false;
            return true;
        }
        return false;
    }
}
