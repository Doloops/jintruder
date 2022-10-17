package org.jintruder.model;

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
        addSubCall(subCall, 0, 0);
    }

    public CallInfo addSubCall(MethodInfo subCall, int lineNumber, int depth)
    {
        CallInfo callInfo = subCalls.get(subCall);
        if (callInfo == null || callInfo.getLineNumber() != lineNumber)
        {
            callInfo = new CallInfo();
            callInfo.setLineNumber(lineNumber);
            callInfo.setDepth(depth);
            subCalls.put(subCall, callInfo);
        }
        callInfo.addCalled();
        return callInfo;
    }

    public MethodInfo addSubCall(ClassInfo classInfo, String methodName, int lineNumber, int depth)
    {
        for (Map.Entry<MethodInfo, CallInfo> entry : subCalls.entrySet())
        {
            MethodInfo methodInfo = entry.getKey();
            if (methodInfo.getClassInfo() != classInfo)
                continue;
            if (!methodInfo.getMethodName().equals(methodName))
                continue;
            CallInfo callInfo = entry.getValue();
            if (callInfo.getLineNumber() != lineNumber)
                continue;
            if (callInfo.getDepth() != depth)
                continue;
            callInfo.addCalled();
            return methodInfo;
        }
        MethodInfo childMethodInfo = new MethodInfo(0, classInfo, methodName);
        CallInfo callInfo = addSubCall(childMethodInfo, lineNumber, depth);
        callInfo.appendCalledTime(1);
        return childMethodInfo;
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
    public String toString()
    {
        return parent.getClassName() + ":" + getMethodName();
    }

    /*
     * @Override public int hashCode() { return parent.getClassName().hashCode()
     * + methodName.hashCode(); }
     * 
     * @Override public boolean equals(Object o) { if (o instanceof MethodInfo)
     * { MethodInfo other = (MethodInfo) o; if (parent != other.parent &&
     * !parent.getClassName().equals(other.parent.getClassName())) return false;
     * if (methodName != other.methodName &&
     * !methodName.equals(other.methodName)) return false; return true; } return
     * false; }
     */
}
