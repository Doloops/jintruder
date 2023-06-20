package org.jintruder.sampler;

import org.jintruder.model.sampler.CallStack;
import org.jintruder.model.sampler.StackTraceFilter;
import org.junit.Assert;
import org.junit.Test;

public class TestStackTraceFilter
{
    @Test
    public void testSkipMethod()
    {
        CallStack.Location toSkip = new CallStack.Location(
                "com.fast2.worker.SourceWorker$$Lambda$551/0x0000000840841c40", "apply", 23);

        String threadString = null;
        String requiresMethodString = null;
        String skipsMethodString = ".*\\$Lambda\\$.*/0x.*";
        StackTraceFilter filter = new StackTraceFilter(threadString, requiresMethodString, skipsMethodString);

        Assert.assertTrue(filter.isSkippedMethod(toSkip));
    }
}
