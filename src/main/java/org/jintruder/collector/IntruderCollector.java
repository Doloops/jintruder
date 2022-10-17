package org.jintruder.collector;

import org.jintruder.instrument.TraceEventBucket;
import org.jintruder.model.ClassMap;

public interface IntruderCollector
{
    int registerMethodReference(String className, String methodName);

    String getMethodName(int methodReference);

    void processBucket(TraceEventBucket bucket);

    ClassMap getClassMap();
}
