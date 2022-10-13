package com.jintruder.collector;

import com.jintruder.instrument.TraceEventBucket;

import comjintruder.model.ClassMap;

public interface IntruderCollector
{
    int registerMethodReference(String className, String methodName);

    String getMethodName(int methodReference);

    void processBucket(TraceEventBucket bucket);

    ClassMap getClassMap();
}
