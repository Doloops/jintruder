package com.jintruder.instrument;

import org.junit.Assert;
import org.junit.Test;

public class TestTraceEventBucket
{
    @Test
    public void addEvent()
    {
        TraceEventBucket bucket = new TraceEventBucket(23);

        Assert.assertEquals(23, bucket.getThreadId());

        bucket.addEvent(123, 456);

        Assert.assertEquals(1, bucket.size());
        Assert.assertEquals(123, bucket.getMethodId(0));
        Assert.assertEquals(456, bucket.getTime(0));
        Assert.assertTrue(bucket.getEnter(0));

        bucket.addEvent(-789, 951);

        Assert.assertEquals(2, bucket.size());
        Assert.assertEquals(789, bucket.getMethodId(1));
        Assert.assertEquals(951, bucket.getTime(1));
        Assert.assertFalse(bucket.getEnter(1));

    }
}
