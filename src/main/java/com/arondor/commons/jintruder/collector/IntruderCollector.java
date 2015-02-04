package com.arondor.commons.jintruder.collector;

public interface IntruderCollector
{

    int registerMethodReference(String className, String methodName);

    void dumpCollection();

    void addCall(long time, long pid, boolean enter, int methodReference);

}
