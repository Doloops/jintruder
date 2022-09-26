package com.arondor.commons.jintruder.collector;

import com.arondor.commons.jintruder.TraceEventBucket;
import com.arondor.commons.jintruder.collector.model.ClassMap;

public interface IntruderCollector
{
    int registerMethodReference(String className, String methodName);

    String getMethodName(int methodReference);

    void processBucket(TraceEventBucket bucket);

    ClassMap getClassMap();
}
