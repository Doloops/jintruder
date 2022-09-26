package com.arondor.commons.jintruder.sink;

import com.arondor.commons.jintruder.collector.model.ClassMap;

public interface IntruderSink
{
    void dumpAll(ClassMap classMap);
}
