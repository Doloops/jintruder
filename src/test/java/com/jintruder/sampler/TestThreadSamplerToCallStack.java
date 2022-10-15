package com.jintruder.sampler;

import java.text.MessageFormat;

import org.junit.Assert;
import org.junit.Test;

import com.jintruder.model.ClassMap;
import com.jintruder.model.ClassMapPrettyPrinter;

public class TestThreadSamplerToCallStack
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

        CallStack callStack = new CallStack();

        ThreadSamplerToCallStack sampler = new ThreadSamplerToCallStack();

        sampler.mergeStackTrace(stackTrace, callStack);

        log("Merged classMap: entryPoints={1}", callStack.getEntryPoints().size());

        Assert.assertEquals(1, callStack.getEntryPoints().size());

        log("Stack: \n{0}", CallStackPrettyPrinter.prettyPrintByEntryPoint(callStack));

        StackTraceCapture capture = new StackTraceCapture();

        methodA(capture);

        sampler.mergeStackTrace(capture.get(), callStack);

        Assert.assertEquals(1, callStack.getEntryPoints().size());

        log("Stack: \n{0}", CallStackPrettyPrinter.prettyPrintByEntryPoint(callStack));
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
        ThreadSamplerToModel sampler = new ThreadSamplerToModel();

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
                try
                {
                    Thread.sleep(50);
                }
                catch (InterruptedException e)
                {
                }
                b();
            }

            private void b()
            {
                for (int i = 0; i < 20; i++)
                {
                    try
                    {
                        Thread.sleep(2);
                    }
                    catch (InterruptedException e)
                    {
                    }
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

        CallStack callStack = new CallStack();
        ThreadSamplerToCallStack sampler = new ThreadSamplerToCallStack();

        sampler.watch(newThread, callStack, 1);

        newThread.start();
        newThread.join();

        log("Stack: \n{0}", CallStackPrettyPrinter.prettyPrintByEntryPoint(callStack));

        new CallStackToCallGrind().dumpAll(callStack);
    }
}
