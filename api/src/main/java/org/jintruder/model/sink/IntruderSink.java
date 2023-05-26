package org.jintruder.model.sink;

import org.jintruder.model.profiler.ClassMap;

public interface IntruderSink
{
    void dumpAll(ClassMap classMap);
}
