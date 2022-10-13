package com.jintruder.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ClassInfo
{
    private final String className;

    public String getClassName()
    {
        return className;
    }

    public ClassInfo(String className)
    {
        this.className = className;
    }

    private Map<String, MethodInfo> methodCalls = new HashMap<String, MethodInfo>();

    public MethodInfo findMethod(String methodName)
    {
        MethodInfo methodCall = methodCalls.get(methodName);
        return methodCall;
    }

    public MethodInfo addMethod(int referenceId, String methodName)
    {
        MethodInfo methodCall = new MethodInfo(referenceId, this, methodName);
        methodCalls.put(methodName, methodCall);
        return methodCall;
    }

    public Map<String, MethodInfo> getMethodMap()
    {
        return methodCalls;
    }

    public Collection<MethodInfo> getMethodCalls()
    {
        return methodCalls.values();
    }

    public long getTotalTime()
    {
        return methodCalls.values().stream().map(MethodInfo::getTotalTime).reduce(0L, Long::sum);
    }

    @Override
    public String toString()
    {
        return className + "\n{" + methodCalls.toString() + "}";
    }
}
