package com.jintruder.sampler;

import java.text.MessageFormat;

import org.junit.Assert;
import org.junit.Test;

import com.jintruder.model.ClassMap;
import com.jintruder.model.ClassMapPrettyPrinter;

public class TestThreadSampler
{
    private static void log(String pattern, Object... vars)
    {
        System.err.println(MessageFormat.format(pattern, vars));
    }

    @Test
    public void testThreadSampler()
    {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

        ClassMap classMap = new ClassMap();

        ThreadSampler sampler = new ThreadSampler();

        sampler.mergeStackTrace(stackTrace, classMap);

        log("Merged classMap: size={0}, entryPoints={1}", classMap.size(), classMap.getEntryPoints().size());

        Assert.assertEquals(1, classMap.getEntryPoints().size());

        log("Entrypoints: \n{0}", ClassMapPrettyPrinter.prettyPrintByEntryPoint(classMap));
    }
}
