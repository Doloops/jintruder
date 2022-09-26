package com.arondor.commons.jintruder.sink;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;

import com.arondor.commons.jintruder.collector.model.CallInfo;
import com.arondor.commons.jintruder.collector.model.ClassInfo;
import com.arondor.commons.jintruder.collector.model.ClassMap;
import com.arondor.commons.jintruder.collector.model.MethodInfo;

public class CacheGrindSink implements IntruderSink
{

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
    public void dumpAll(ClassMap classMap)
    {
        long epoch = System.currentTimeMillis();
        String dumpFile = "callgrind.out." + epoch;
        System.err.println("[INTRUDER] : Dumping events to : " + dumpFile);
        try
        {
            dump(dumpFile, classMap);
        }
        catch (IOException e)
        {
            System.err.println("[INTRUDER] : Could not dump to file " + dumpFile + " : " + e.getMessage());
            e.printStackTrace(System.err);
        }

    }

    private void dump(String fileName, ClassMap classMap) throws IOException
    {
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
