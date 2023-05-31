package org.jintruder.sampler;

import java.lang.instrument.Instrumentation;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.jintruder.model.JintruderConfig;
import org.jintruder.model.NamedThreadFactory;
import org.jintruder.model.sampler.CallStack;
import org.jintruder.model.sampler.StackTraceFilter;

public class JintruderPremain
{
    private static final ScheduledExecutorService SCHEDULER = Executors.newScheduledThreadPool(4,
            new NamedThreadFactory("jintruder"));

    public static void premain(String agentArgs, Instrumentation inst)
    {
        if (JintruderConfig.isEnableSampling())
        {
            startSampling();
        }
    }

    public static void startSampling()
    {
        ThreadSamplerToCallStack sampler = new ThreadSamplerToCallStack();
        CallStackToJson dumper = new CallStackToJson();
        StackTraceFilter filter = new StackTraceFilter(JintruderConfig.getSamplingThreadPattern(), null, null);
        sampler.watchMultipleThreads(SCHEDULER, filter, JintruderConfig.getSamplingInterval(), CALL_STACK);
        if (JintruderConfig.getDumpInterval() > 0)
            dumper.dumpPeriodically(SCHEDULER, CALL_STACK, JintruderConfig.getDumpInterval());
    }

    private static final CallStack CALL_STACK = new CallStack();

    public static final CallStack getCurrentCallStack()
    {
        return CALL_STACK;
    }

}
