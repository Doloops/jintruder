package com.arondor.commons.jintruder.collector.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ClassName
{
    private final String className;

    public String getClassName()
    {
        return className;
    }
 
    public ClassName(String className)
    {
        this.className = className;
    }
    
    private Map<String, MethodCall> methodCalls = new HashMap<String, MethodCall>();
    
    public MethodCall findMethod(String methodName)
    {
        MethodCall methodCall = methodCalls.get(methodName);
        if ( methodCall == null )
        {
            methodCall = new MethodCall(this, methodName);
            methodCalls.put(methodName, methodCall);
        }
        
        return methodCall;
    }
    
    public Collection<MethodCall> getMethodCalls()
    {
        return methodCalls.values();
    }
}
