package com.arondor.commons.jintruder.collector;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.arondor.commons.jintruder.collector.model.ClassInfo;
import com.arondor.commons.jintruder.collector.model.ClassMap;
import com.arondor.commons.jintruder.collector.model.MethodInfo;
import com.arondor.commons.jintruder.collector.model.MethodStack;
import com.arondor.commons.jintruder.collector.model.MethodStackItem;

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
                    MethodInfo parent = methodStack.peek().getMethodCall();
                    parent.addSubCall(methodInfo);
                }
                methodStack.push(new MethodStackItem(methodInfo, time));
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

        while (!methodStack.isEmpty())
        {
            MethodStackItem currentStackItem = methodStack.peek();
            long startTime = currentStackItem.getStartTime();
            long timeSpent = time - startTime;
            if (timeSpent < 0)
            {
                System.err.println("Spurious ! addCall() timeSpent=" + timeSpent + " at methodInfo=" + methodInfo + " ["
                        + methodInfo.getMethodName() + "]" + "(startTime=" + startTime + ", time=" + time + ")");
                timeSpent = 0;
            }
            currentStackItem.getMethodCall().appendInclusiveTime(timeSpent);

            methodStack.pop();

            if (!methodStack.isEmpty())
            {
                methodStack.peek().getMethodCall().appendCallerTime(currentStackItem.getMethodCall(), timeSpent);
            }

            if (currentStackItem.getMethodCall() == methodInfo)
            {
                break;
            }
            else
            {
                System.err.println(
                        "Jumped stack ! parent = " + currentStackItem.getMethodCall() + ", methodCall=" + methodInfo);
                if (methodStack.isEmpty())
                {
                    System.err.println("Could not rewind stack for methodCall=" + methodInfo);
                }
            }
        }
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
                for (MethodStackItem call : threadEntry.getValue())
                {
                    System.err.println(" * " + call.getMethodCall());
                }
            }
        }
    }

}
