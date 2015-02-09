package com.arondor.commons.jintruder.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
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

@Deprecated
public class IntruderParser
{
    public IntruderParser()
    {

    }

    private Map<String, ClassInfo> classMap = new HashMap<String, ClassInfo>();

    protected ClassInfo findClassName(String className)
    {
        ClassInfo clazz = classMap.get(className);
        if (clazz == null)
        {
            clazz = new ClassInfo(className);
            classMap.put(className, clazz);
        }
        return clazz;
    }

    @Deprecated
    protected MethodInfo findMethodCall(String methodCompleteName)
    {
        int pfx = methodCompleteName.lastIndexOf(".");
        if (pfx == -1)
        {
            throw new IllegalArgumentException("mname=" + methodCompleteName);
        }
        String className = methodCompleteName.substring(0, pfx); // .replace('/',
                                                                 // '.');
        String methodName = methodCompleteName.substring(pfx + 1);
        return findClassName(className).findMethod(methodName);
    }

    private Map<Long, MethodStack> perThreadStack = new HashMap<Long, MethodStack>();

    protected MethodStack getPerThreadStack(long pid)
    {
        MethodStack methodStack = perThreadStack.get(pid);
        if (methodStack == null)
        {
            methodStack = new MethodStack();
            perThreadStack.put(pid, methodStack);
        }
        return methodStack;
    }

    public void parse(File file) throws IOException
    {
        BufferedReader br = null;
        try
        {
            br = new BufferedReader(new FileReader(file));
            doParse(br);
        }
        finally
        {
            if (br != null)
            {
                br.close();
            }
        }
    }

    private Map<Integer, MethodInfo> methodReferenceMap = new ConcurrentHashMap<Integer, MethodInfo>();

    @Deprecated
    public void registerMethodReference(int referenceId, String className, String methodName)
    {
        if (methodReferenceMap.containsKey(referenceId))
        {
            throw new IllegalArgumentException("Already registered : " + referenceId);
        }
        MethodInfo methodCall = findClassName(className).findMethod(methodName);
        methodReferenceMap.put(referenceId, methodCall);
    }

    @Deprecated
    public synchronized void addCall(long time, long pid, boolean enter, int referenceId)
    {
        MethodInfo methodCall = methodReferenceMap.get(referenceId);
        if (methodCall == null)
        {
            System.err.println("Could not resolve : " + referenceId);
            return;
        }
        addCall(time, pid, enter, methodCall);
    }

    @Deprecated
    public synchronized void addCall(long time, long pid, boolean enter, String method)
    {
        MethodInfo methodCall = method.equals(".") ? null : findMethodCall(method);
        addCall(time, pid, enter, methodCall);
    }

    @Deprecated
    public synchronized final void addCall(long time, long pid, boolean enter, MethodInfo methodCall)
    {
        if (enter && methodCall == null)
        {
            throw new IllegalArgumentException("Invalid !");
        }
        MethodStack methodStack = getPerThreadStack(pid);

        if (enter)
        {
            if (methodStack.isEmpty())
            {
                // System.err.println("Method is root of its stack : " +
                // methodCall + " (pid:" + pid + ")");
            }
            else
            {
                MethodInfo parent = methodStack.peek().getMethodCall();
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
                methodCall = methodStack.peek().getMethodCall();
            }
            else if (methodStack.peek().getMethodCall() != methodCall)
            {
                System.err.println("Corrupted stack : parent = " + methodStack.peek().getMethodCall() + ", methodCall="
                        + methodCall);
                // throw new IllegalArgumentException("Corrupted stack !!");
                return;
            }
            long startTime = methodStack.peek().getStartTime();
            long timeSpent = time - startTime;
            if (timeSpent < 0)
            {
                System.err.println("Spurious!addCall() timeSpent=" + timeSpent + " at methodCall=" + methodCall
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
                MethodInfo caller = methodStack.peek().getMethodCall();
                caller.appendCallerTime(methodCall, timeSpent);
            }
        }
    }

    private void doParse(BufferedReader br) throws NumberFormatException, IOException
    {
        String line;

        int lineNumber = 0;

        while ((line = br.readLine()) != null)
        {
            lineNumber++;

            if (lineNumber % 100000 == 0)
            {
                System.err.println("At line : " + lineNumber);
            }

            String comps[] = line.split(";");
            long time = Long.parseLong(comps[0]);
            long pid = Long.parseLong(comps[1]);

            boolean enter = comps[2].equals("E");

            String method = comps[3];

            addCall(time, pid, enter, method);
        }
    }

    private String protectMethodName(String methodName)
    {
        return methodName.replace('<', '_').replace('>', '_');
    }

    private String protectClassName(String className)
    {
        return className.replace('/', '.');
    }

    private void dump(PrintStream printStream, MethodInfo methodCall)
    {
        printStream.println("fn=" + protectMethodName(methodCall.getMethodName()));
        printStream.println("0 " + methodCall.getPrivateTime());
        for (Map.Entry<MethodInfo, CallInfo> entry : methodCall.getSubCalls())
        {
            MethodInfo subCall = entry.getKey();
            printStream.println("cfl=" + protectClassName(subCall.getClassInfo().getClassName()));
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

    public synchronized void dump(String fileName) throws FileNotFoundException
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

        for (ClassInfo className : classMap.values())
        {
            printStream.println("fl=" + protectClassName(className.getClassName()));
            for (MethodInfo methodCall : className.getMethodCalls())
            {
                dump(printStream, methodCall);
            }
            printStream.println("");
        }
        printStream.close();

    }
}
