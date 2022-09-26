package com.arondor.commons.jintruder.collector;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.arondor.commons.jintruder.collector.model.ClassInfo;
import com.arondor.commons.jintruder.collector.model.ClassMap;
import com.arondor.commons.jintruder.collector.model.MethodInfo;
import com.arondor.commons.jintruder.collector.model.MethodStack;

public class MemIntruderCollector implements IntruderCollector
{
    private static final boolean VERBOSE = false;

    private boolean dumpUncleanThreads = false;

    private Map<Long, MethodStack> perThreadStack = new HashMap<Long, MethodStack>();

    private final ClassMap classMap = new ClassMap();

    private Map<Integer, MethodInfo> methodReferenceMap = new ConcurrentHashMap<Integer, MethodInfo>();

    public MemIntruderCollector()
    {

    }

    private static final void log(String msg)
    {
        System.err.println(msg);
    }

    private synchronized final MethodStack getPerThreadStack(long threadId)
    {
        MethodStack methodStack = perThreadStack.get(threadId);
        if (methodStack == null)
        {
            methodStack = new MethodStack();
            perThreadStack.put(threadId, methodStack);
        }
        return methodStack;
    }

    private static int nextMethodReference = 0;

    @Override
    public final int registerMethodReference(String className, String methodName)
    {
        MethodInfo methodCall = findClassName(className).findMethod(methodName);
        if (methodCall == null)
        {
            nextMethodReference++;
            int referenceId = nextMethodReference;
            methodCall = findClassName(className).addMethod(referenceId, methodName);
            methodReferenceMap.put(referenceId, methodCall);
        }
        if (VERBOSE)
        {
            log("REGISTER " + methodCall.getReferenceId() + " " + className + "." + methodName);
        }

        return methodCall.getReferenceId();
    }

    @Override
    public String getMethodName(int methodReference)
    {
        MethodInfo info = methodReferenceMap.get(methodReference);
        return info.getClassInfo().getClassName() + "::" + info.getMethodName();
    }

    private ClassInfo findClassName(String className)
    {
        ClassInfo clazz = classMap.get(className);
        if (clazz == null)
        {
            clazz = new ClassInfo(className);
            classMap.put(className, clazz);
        }
        return clazz;
    }

    @Override
    public final void addCall(long time, long threadId, boolean enter, int referenceId)
    {
        MethodInfo methodCall = methodReferenceMap.get(referenceId);
        if (methodCall == null)
        {
            System.err.println("Could not resolve : " + referenceId);
            return;
        }
        addCall(time, threadId, enter, referenceId, methodCall);
    }

    private final void addCall(long time, long threadId, boolean enter, int referenceId, MethodInfo methodInfo)
    {
        if (enter && methodInfo == null)
        {
            throw new IllegalArgumentException("Invalid !");
        }
        MethodStack methodStack = getPerThreadStack(threadId);

        synchronized (methodStack)
        {
            if (enter)
            {
                if (!methodStack.isEmpty())
                {
                    MethodInfo parent = methodStack.peekMethodCall();
                    parent.addSubCall(methodInfo);
                }
                methodStack.push(methodInfo, time);
            }
            else
            {
                methodCallFinished(time, methodInfo, methodStack);
            }
        }
    }

    private void methodCallFinished(long time, MethodInfo methodInfo, MethodStack methodStack)
    {
        if (methodStack.isEmpty())
        {
            System.err.println("Empty stack for methodCall=" + methodInfo);
            return;
        }

        MethodInfo currentMethod = methodStack.peekMethodCall();
        if (currentMethod != methodInfo)
        {
            System.err.println("Jumped stack ! parent=" + currentMethod.getMethodName() + ", methodCall="
                    + methodInfo.getMethodName());
            if (methodStack.isEmpty())
            {
                System.err.println("Could not rewind stack for methodCall=" + methodInfo);
            }
            return;
        }

        methodStack.setFinishTime(time);
        methodStack.pop();
    }

    @Override
    public ClassMap getClassMap()
    {
        if (dumpUncleanThreads)
            dumpUncleanThreads();
        return classMap;
    }

    private void dumpUncleanThreads()
    {
        for (Map.Entry<Long, MethodStack> threadEntry : perThreadStack.entrySet())
        {
            if (!threadEntry.getValue().isEmpty())
            {
                System.err.println("Unclean stack : at thread : " + threadEntry.getKey());
                threadEntry.getValue().dumpStack();
            }
        }
    }

}
