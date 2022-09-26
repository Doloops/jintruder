package com.arondor.commons.jintruder.collector;

import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.arondor.commons.jintruder.collector.model.CallInfo;
import com.arondor.commons.jintruder.collector.model.ClassInfo;
import com.arondor.commons.jintruder.collector.model.MethodInfo;
import com.arondor.commons.jintruder.collector.model.MethodStack;
import com.arondor.commons.jintruder.collector.model.MethodStackItem;

public class CacheGrindIntruderCollector implements IntruderCollector
{
    private boolean dumpUncleanThreads = false;

    private static final boolean VERBOSE = true;

    private Map<Long, MethodStack> perThreadStack = new HashMap<Long, MethodStack>();

    private Map<String, ClassInfo> classMap = new HashMap<String, ClassInfo>();

    private Map<Integer, MethodInfo> methodReferenceMap = new ConcurrentHashMap<Integer, MethodInfo>();

    public CacheGrindIntruderCollector()
    {

    }

    private static final void log(String msg)
    {
        System.err.println(msg);
    }

    private synchronized final MethodStack getPerThreadStack(long pid)
    {
        MethodStack methodStack = perThreadStack.get(pid);
        if (methodStack == null)
        {
            methodStack = new MethodStack();
            perThreadStack.put(pid, methodStack);
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
    public final void addCall(long time, long pid, boolean enter, int referenceId)
    {
        MethodInfo methodCall = methodReferenceMap.get(referenceId);
        if (methodCall == null)
        {
            System.err.println("Could not resolve : " + referenceId);
            return;
        }
        addCall(time, pid, enter, referenceId, methodCall);
    }

    private final void addCall(long time, long pid, boolean enter, int referenceId, MethodInfo methodInfo)
    {
        if (enter && methodInfo == null)
        {
            throw new IllegalArgumentException("Invalid !");
        }
        MethodStack methodStack = getPerThreadStack(pid);

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
                        System.err.println(
                                "Spurious ! addCall() timeSpent=" + timeSpent + " at methodInfo=" + methodInfo + " ["
                                        + referenceId + "]" + "(startTime=" + startTime + ", time=" + time + ")");
                        timeSpent = 0;
                    }
                    currentStackItem.getMethodCall().appendInclusiveTime(timeSpent);

                    methodStack.pop();

                    if (!methodStack.isEmpty())
                    {
                        methodStack.peek().getMethodCall().appendCallerTime(currentStackItem.getMethodCall(),
                                timeSpent);
                    }

                    if (currentStackItem.getMethodCall() == methodInfo)
                    {
                        break;
                    }
                    else
                    {
                        System.err.println("Jumped stack ! parent = " + currentStackItem.getMethodCall()
                                + ", methodCall=" + methodInfo);
                        if (methodStack.isEmpty())
                        {
                            System.err.println("Could not rewind stack for methodCall=" + methodInfo);
                        }
                    }
                }
            }
        }
    }

    private final String protectMethodName(MethodInfo methodInfo)
    {
        String methodName = methodInfo.getMethodName();
        String result = methodName.replace('<', '_').replace('>', '_');
        return result;
    }

    private final String protectClassName(ClassInfo classInfo)
    {
        String className = classInfo.getClassName();
        return className.replace('/', '.').replace('$', '_');
    }

    @Override
    public void dumpCollection()
    {
        long epoch = System.currentTimeMillis();
        String dumpFile = "callgrind.out." + epoch;
        System.err.println("[INTRUDER] : Dumping events to : " + dumpFile);
        try
        {
            dump(dumpFile);
        }
        catch (IOException e)
        {
            System.err.println("[INTRUDER] : Could not dump to file " + dumpFile + " : " + e.getMessage());
            e.printStackTrace(System.err);
        }

    }

    private void dump(String fileName) throws IOException
    {
        if (dumpUncleanThreads)
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

        PrintStream printStream = new PrintStream(fileName);
        printStream.println("version: 1");
        printStream.println("");
        printStream.println("events: ticks");
        printStream.println("");

        for (ClassInfo className : classMap.values())
        {
            if (className.getTotalTime() > 0)
            {
                printStream.println("fl=" + protectClassName(className));
                for (MethodInfo methodCall : className.getMethodCalls())
                {
                    dump(printStream, methodCall);
                }
                printStream.println("");
            }
        }
        printStream.close();
    }

    private void dump(PrintStream printStream, MethodInfo methodCall)
    {
        if (methodCall.getPrivateTime() == 0)
            return;
        printStream.println("fn=" + protectMethodName(methodCall));
        printStream.println("0 " + methodCall.getPrivateTime());
        for (Map.Entry<MethodInfo, CallInfo> entry : methodCall.getSubCalls())
        {
            MethodInfo subCall = entry.getKey();
            printStream.println("cfl=" + protectClassName(subCall.getClassInfo()));
            printStream.println("cfn=" + protectMethodName(subCall));
            printStream.println("calls=" + entry.getValue().getNumber() + " " + "0");

            long timeSpent = entry.getValue().getTimeSpent();
            if (timeSpent < 0)
            {
                System.err.println("Spurious ! timeSpent=" + timeSpent + " in call : " + methodCall + "=>" + subCall);
                timeSpent = 1;
            }
            printStream.println("0 " + timeSpent);
        }
    }
}
