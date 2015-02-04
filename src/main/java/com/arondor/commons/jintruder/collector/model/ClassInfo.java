package com.arondor.commons.jintruder.collector.model;

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
        if (methodCall == null)
        {
            methodCall = new MethodInfo(this, methodName);
            methodCalls.put(methodName, methodCall);
        }
        return methodCall;
    }

    public Collection<MethodInfo> getMethodCalls()
    {
        return methodCalls.values();
    }
}
