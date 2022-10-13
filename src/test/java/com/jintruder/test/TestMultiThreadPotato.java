package com.jintruder.test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

import org.junit.Assert;
import org.junit.Test;

import com.jintruder.instrument.JintruderTracker;
import com.jintruder.model.ClassInfo;
import com.jintruder.model.ClassMap;
import com.jintruder.model.MethodInfo;

public class TestMultiThreadPotato extends AbstractBaseIntruderTest
{
    @Test
    public void testSimpleMultithread() throws FileNotFoundException, IOException, IllegalClassFormatException,
            ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, NoSuchMethodException, SecurityException, InterruptedException
    {
        String classNames[] = { "com.acme.Potato" };

        Runnable runnable = forgeClassMethodRunnable(classNames, "testSimple");

        int nbThreads = 10_000;

        CountDownLatch latch = new CountDownLatch(nbThreads);

        Semaphore sem = new Semaphore(6);

        for (int th = 0; th < nbThreads; th++)
        {
            try
            {
                sem.acquire();
            }
            catch (InterruptedException e)
            {
                System.err.println("Caught : " + e.getMessage());
                e.printStackTrace();
                Assert.fail();
            }
            new Thread()
            {
                @Override
                public void run()
                {
                    runnable.run();

                    try
                    {
                        Thread.sleep(1);
                    }
                    catch (InterruptedException e)
                    {
                    }

                    latch.countDown();
                    sem.release();
                }
            }.start();
        }

        latch.await();

        ClassMap classMap = JintruderTracker.getClassMap();

        ClassInfo classInfo = classMap.get(classNames[0].replace('.', '/'));
        Assert.assertNotNull(classInfo);

        MethodInfo methodInfo = classInfo.getMethodMap().get("testSimple");
        Assert.assertNotNull(methodInfo);

        Assert.assertEquals(nbThreads, methodInfo.getNumberOfCalls());
    }
}
