package com.arondor.commons.jintruder.collector;

import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.arondor.commons.jintruder.collector.model.ClassName;
import com.arondor.commons.jintruder.collector.model.MethodCall;
import com.arondor.commons.jintruder.collector.model.MethodCall.CallInfo;
import com.arondor.commons.jintruder.collector.model.MethodStack;
import com.arondor.commons.jintruder.collector.model.MethodStackItem;

public class CacheGrindIntruderCollector implements IntruderCollector
{
    private Map<Long, MethodStack> perThreadStack = new HashMap<Long, MethodStack>();

    private Map<String, ClassName> classMap = new HashMap<String, ClassName>();

    private Map<Integer, MethodCall> methodReferenceMap = new ConcurrentHashMap<Integer, MethodCall>();

    private final MethodStack getPerThreadStack(long pid)
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
        nextMethodReference++;
        int referenceId = nextMethodReference;

        MethodCall methodCall = findClassName(className).findMethod(methodName);
        methodReferenceMap.put(referenceId, methodCall);

        return referenceId;
    }

    private ClassName findClassName(String className)
    {
        ClassName clazz = classMap.get(className);
        if (clazz == null)
        {
            clazz = new ClassName(className);
            classMap.put(className, clazz);
        }
        return clazz;
    }

    @Override
    public synchronized final void addCall(long time, long pid, boolean enter, int referenceId)
    {
        MethodCall methodCall = methodReferenceMap.get(referenceId);
        if (methodCall == null)
        {
            System.err.println("Could not resolve : " + referenceId);
            return;
        }
        addCall(time, pid, enter, referenceId, methodCall);
    }

    private final void addCall(long time, long pid, boolean enter, int referenceId, MethodCall methodCall)
    {
        if (enter && methodCall == null)
        {
            throw new IllegalArgumentException("Invalid !");
        }
        MethodStack methodStack = getPerThreadStack(pid);

        if (enter)
        {
            if (!methodStack.isEmpty())
            {
                MethodCall parent = methodStack.peek().getMethodCall();
                parent.addSubCall(methodCall);
            }
            methodStack.push(new MethodStackItem(methodCall, time));
        }
        else
        {
            if (methodStack.isEmpty())
            {
                System.err.println("Empty stack !!!");
                throw new IllegalArgumentException("Empty stack !!");
            }

            if (methodCall == null)
            {
                // methodCall = methodStack.peek().getMethodCall();
            }

            while (!methodStack.isEmpty() && methodStack.peek().getMethodCall() != methodCall)
            {
                System.err.println("Jumped stack ! parent = " + methodStack.peek().getMethodCall() + ", methodCall="
                        + methodCall);
                // for (MethodStackItem parent : methodStack)
                // {
                // System.err.println(" Trace * " + parent.getMethodCall());
                // }
                // throw new IllegalArgumentException("Corrupted stack !!");
                // return;
                methodStack.pop();
            }
            long startTime = methodStack.peek().getStartTime();
            long timeSpent = time - startTime;
            if (timeSpent < 0)
            {
                System.err.println("Spurious! addCall() timeSpent=" + timeSpent + " at methodCall=" + methodCall
                        + "(startTime=" + startTime + ", time=" + time + ")");
                for (MethodStackItem parent : methodStack)
                {
                    System.err.println(" Trace * " + parent.getMethodCall());
                }
                timeSpent = 0;
            }
            methodCall.appendInclusiveTime(timeSpent);
            methodStack.pop();

            if (!methodStack.isEmpty())
            {
                MethodCall caller = methodStack.peek().getMethodCall();
                caller.appendCallerTime(methodCall, timeSpent);
            }
        }
    }

    private final String protectMethodName(String methodName)
    {
        return methodName.replace('<', '_').replace('>', '_');
    }

    private final String protectClassName(String className)
    {
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

        PrintStream printStream = new PrintStream(fileName);
        printStream.println("version: 1");
        printStream.println("");
        printStream.println("events: ticks");
        printStream.println("");
        // printStream.println("fl=" +
        // motherCall.getClassName().getClassName());
        // dump(printStream, motherCall);

        for (ClassName className : classMap.values())
        {
            printStream.println("fl=" + protectClassName(className.getClassName()));
            for (MethodCall methodCall : className.getMethodCalls())
            {
                dump(printStream, methodCall);
            }
            printStream.println("");
        }
        printStream.close();
    }

    private void dump(PrintStream printStream, MethodCall methodCall)
    {
        printStream.println("fn=" + protectMethodName(methodCall.getMethodName()));
        printStream.println("0 " + methodCall.getPrivateTime());
        for (Map.Entry<MethodCall, CallInfo> entry : methodCall.getSubCalls())
        {
            MethodCall subCall = entry.getKey();
            printStream.println("cfl=" + protectClassName(subCall.getClassName().getClassName()));
            printStream.println("cfn=" + protectMethodName(subCall.getMethodName()));
            printStream.println("calls=" + entry.getValue().getNumber() + " " + "0");

            long timeSpent = entry.getValue().getTimeSpent();
            if (timeSpent <= 0)
            {
                System.err.println("Spurious!dump() timeSpent=" + timeSpent + " in call : " + methodCall + "=>"
                        + subCall);
                // throw new IllegalArgumentException("Spurious timeSpent=" +
                // timeSpent + " in call : " + methodCall + "=>" + subCall);
                timeSpent = 1;
            }
            printStream.println("0 " + timeSpent);
        }
    }

}
