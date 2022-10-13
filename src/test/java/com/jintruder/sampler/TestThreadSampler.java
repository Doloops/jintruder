package com.jintruder.sampler;

import java.text.MessageFormat;

import org.junit.Assert;
import org.junit.Test;

import com.jintruder.model.ClassMap;
import com.jintruder.model.ClassMapPrettyPrinter;
import com.jintruder.sink.CacheGrindSink;

public class TestThreadSampler
{
    private static void log(String pattern, Object... vars)
    {
        System.err.println(MessageFormat.format(pattern, vars));
    }

    private static class StackTraceCapture
    {
        private StackTraceElement[] stackTrace;

        public void take()
        {
            stackTrace = Thread.currentThread().getStackTrace();
        }

        public StackTraceElement[] get()
        {
            return stackTrace;
        }
    }

    private void methodA(StackTraceCapture capture)
    {
        methodB(capture);
    }

    private void methodB(StackTraceCapture capture)
    {
        methodC(capture);
    }

    private void methodC(StackTraceCapture capture)
    {
        capture.take();
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

        log("Stack: \n{0}", ClassMapPrettyPrinter.prettyPrintByEntryPoint(classMap));

        StackTraceCapture capture = new StackTraceCapture();

        methodA(capture);

        sampler.mergeStackTrace(capture.get(), classMap);

        Assert.assertEquals(1, classMap.getEntryPoints().size());

        log("Stack: \n{0}", ClassMapPrettyPrinter.prettyPrintByEntryPoint(classMap));
    }

    @Test
    public void testSimpleThread() throws InterruptedException
    {
        Thread newThread = new Thread()
        {
            private void a()
            {
                b();
            }

            private void b()
            {
                c();
            }

            private void c()
            {
                try
                {
                    Thread.sleep(2_000);
                }
                catch (InterruptedException e)
                {
                }
            }

            @Override
            public void run()
            {
                a();
            }
        };

        ClassMap classMap = new ClassMap();
        ThreadSampler sampler = new ThreadSampler();

        sampler.watch(newThread, classMap, 10);

        newThread.start();
        newThread.join();

        log("Stack: \n{0}", ClassMapPrettyPrinter.prettyPrintByEntryPoint(classMap));
    }

    @Test
    public void testSimpleThreadWithTwoMethods() throws InterruptedException
    {
        Thread newThread = new Thread()
        {
            private void a()
            {
                b();
            }

            private void b()
            {
                for (int i = 0; i < 20; i++)
                {
                    c();
                    d();
                }
            }

            private void c()
            {
                try
                {
                    Thread.sleep(50);
                }
                catch (InterruptedException e)
                {
                }
            }

            private void d()
            {
                try
                {
                    Thread.sleep(50);
                }
                catch (InterruptedException e)
                {
                }
            }

            @Override
            public void run()
            {
                a();
            }
        };

        ClassMap classMap = new ClassMap();
        ThreadSampler sampler = new ThreadSampler();

        sampler.watch(newThread, classMap, 1);

        newThread.start();
        newThread.join();

        log("Stack: \n{0}", ClassMapPrettyPrinter.prettyPrintByEntryPoint(classMap));

        CacheGrindSink sink = new CacheGrindSink();
        sink.dumpAll(classMap);
    }
}
