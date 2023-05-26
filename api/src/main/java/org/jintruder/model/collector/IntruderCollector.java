package org.jintruder.model.collector;

import org.jintruder.model.profiler.ClassMap;
import org.jintruder.model.profiler.TraceEventBucket;

public interface IntruderCollector
{
    int registerMethodReference(String className, String methodName);

    String getMethodName(int methodReference);

    void processBucket(TraceEventBucket bucket);

    ClassMap getClassMap();
}
