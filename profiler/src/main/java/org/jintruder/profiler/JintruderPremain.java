package org.jintruder.profiler;

import java.lang.instrument.Instrumentation;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.jintruder.model.JintruderConfig;
import org.jintruder.model.NamedThreadFactory;
import org.jintruder.model.sampler.CallStack;

public class JintruderPremain
{
    private static final ScheduledExecutorService SCHEDULER = Executors.newScheduledThreadPool(4,
            new NamedThreadFactory("jintruder"));

    static
    {
        JintruderConfig.enableProfiler();
    }

    public static void premain(String agentArgs, Instrumentation inst)
    {
        if (JintruderConfig.isEnableProfiler())
        {
            inst.addTransformer(new JintruderTransformer());
        }
    }

    private static final CallStack CALL_STACK = new CallStack();

    public static final CallStack getCurrentCallStack()
    {
        return CALL_STACK;
    }

}
