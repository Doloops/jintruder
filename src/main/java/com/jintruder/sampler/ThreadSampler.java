package com.jintruder.sampler;

import java.text.MessageFormat;

import com.jintruder.model.ClassInfo;
import com.jintruder.model.ClassMap;
import com.jintruder.model.MethodInfo;

public class ThreadSampler
{
    private static void log(String pattern, Object... vars)
    {
        System.err.println(MessageFormat.format(pattern, vars));
    }

    public void mergeStackTrace(StackTraceElement[] stackTrace, ClassMap classMap)
    {
        MethodInfo previousMethod = null;
        for (int index = stackTrace.length - 1; index > 0; index--)
        {
            StackTraceElement element = stackTrace[index];
            String className = element.getClassName();
            int lineNumber = element.getLineNumber();
            String methodName = element.getMethodName() + ":" + lineNumber;
            log("Stack {0}:{1}:{2}", className, methodName, lineNumber);

            ClassInfo currentClass = classMap.findClass(className);
            MethodInfo currentMethod = currentClass.findMethod(methodName);

            if (currentMethod == null)
            {
                currentMethod = currentClass.addMethod(0, methodName);
            }
            if (previousMethod != null)
            {
                previousMethod.addSubCall(currentMethod);
            }
            else
            {
                classMap.addEntryPoint(currentMethod);
            }
            previousMethod = currentMethod;
        }

    }
}
