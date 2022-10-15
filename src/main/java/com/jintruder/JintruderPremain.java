package com.jintruder;

import java.lang.instrument.Instrumentation;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import com.jintruder.instrument.JintruderTransformer;
import com.jintruder.sampler.CallStack;
import com.jintruder.sampler.CallStackToJson;
import com.jintruder.sampler.ThreadSamplerToCallStack;

public class JintruderPremain
{
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4,
            new NamedThreadFactory("jintruder"));

    public static void premain(String agentArgs, Instrumentation inst)
    {
        if (JintruderConfig.isEnableDecoration())
            inst.addTransformer(new JintruderTransformer());
        if (JintruderConfig.isEnableSampling())
        {
            ThreadSamplerToCallStack sampler = new ThreadSamplerToCallStack();
            CallStack callStack = new CallStack();
            CallStackToJson dumper = new CallStackToJson();
            String pattern = ".*";
            sampler.watchMultipleThreads(scheduler, pattern, JintruderConfig.getSamplingInterval(), callStack);
            dumper.dumpPeriodically(scheduler, callStack, JintruderConfig.getDumpInterval());
        }
    }

}
