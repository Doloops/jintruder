package org.jintruder;

import java.lang.instrument.Instrumentation;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.jintruder.instrument.JintruderTransformer;
import org.jintruder.sampler.CallStack;
import org.jintruder.sampler.CallStackToJson;
import org.jintruder.sampler.ThreadSamplerToCallStack;

public class JintruderPremain
{
    private static final ScheduledExecutorService SCHEDULER = Executors.newScheduledThreadPool(4,
            new NamedThreadFactory("jintruder"));

    public static void premain(String agentArgs, Instrumentation inst)
    {
        if (JintruderConfig.isEnableDecoration())
            inst.addTransformer(new JintruderTransformer());
        if (JintruderConfig.isEnableSampling())
        {
            startSampling();
        }
    }

    private static void startSampling()
    {
        ThreadSamplerToCallStack sampler = new ThreadSamplerToCallStack();
        CallStackToJson dumper = new CallStackToJson();
        sampler.watchMultipleThreads(SCHEDULER, JintruderConfig.getSamplingThreadPattern(),
                JintruderConfig.getSamplingInterval(), CALL_STACK);
        if (JintruderConfig.getDumpInterval() > 0)
            dumper.dumpPeriodically(SCHEDULER, CALL_STACK, JintruderConfig.getDumpInterval());
    }

    private static final CallStack CALL_STACK = new CallStack();

    public static final CallStack getCurrentCallStack()
    {
        return CALL_STACK;
    }

}
