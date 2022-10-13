package com.jintruder.test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.reflect.InvocationTargetException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.jintruder.instrument.JintruderTracker;

import comjintruder.model.CallInfo;
import comjintruder.model.ClassInfo;
import comjintruder.model.ClassMap;
import comjintruder.model.MethodInfo;

public class TestPotato extends AbstractBaseIntruderTest
{
    @Before
    public void init()
    {
        JintruderTracker.reset();
    }

    @Test
    public void testPotatoDouble() throws FileNotFoundException, IOException, IllegalClassFormatException,
            ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, NoSuchMethodException, SecurityException
    {
        String className = "com.acme.Potato";

        executeClassMethod(className, "testDouble");

        ClassMap classMap = JintruderTracker.getClassMap();
        Assert.assertEquals(1, classMap.size());

        ClassInfo classInfo = classMap.get(className.replace('.', '/'));
        MethodInfo methodInfo = classInfo.getMethodMap().get("testDouble");

        CallInfo burnA = methodInfo.getSubCall("cpuburnA");
        Assert.assertEquals(1, burnA.getNumber());

        CallInfo burnB = methodInfo.getSubCall("cpuburnB");
        Assert.assertEquals(1, burnB.getNumber());

        Assert.assertTrue(methodInfo.getTotalTime() >= burnA.getTimeSpent() + burnB.getTimeSpent());
    }

    @Test
    public void testPotatoTestWithException() throws FileNotFoundException, IOException, IllegalClassFormatException,
            ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, NoSuchMethodException, SecurityException
    {
        String className = "com.acme.Potato";

        executeClassMethod(className, "testWithException");

        ClassMap classMap = JintruderTracker.getClassMap();
        Assert.assertEquals(1, classMap.size());

        ClassInfo classInfo = classMap.get(className.replace('.', '/'));
        MethodInfo methodInfo = classInfo.getMethodMap().get("testWithException");

        Assert.assertTrue(methodInfo.getTotalTime() > 0);

        CallInfo thrownButCaught = methodInfo.getSubCall("thrownButCaught");
        Assert.assertEquals(1, thrownButCaught.getNumber());
        Assert.assertTrue(thrownButCaught.getTimeSpent() > 0);

        Assert.assertTrue(methodInfo.getTotalTime() >= thrownButCaught.getTimeSpent());
    }

    @Test
    public void testPotatoTestALot() throws FileNotFoundException, IOException, IllegalClassFormatException,
            ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, NoSuchMethodException, SecurityException
    {
        String className = "com.acme.Potato";

        executeClassMethod(className, "testALot");

        ClassMap classMap = JintruderTracker.getClassMap();
        Assert.assertEquals(1, classMap.size());

        ClassInfo classInfo = classMap.get(className.replace('.', '/'));
        MethodInfo methodInfo = classInfo.getMethodMap().get("testALot");

        Assert.assertTrue(methodInfo.getTotalTime() > 0);

        CallInfo burnA = methodInfo.getSubCall("cpuburnA");
        Assert.assertEquals(2_000_000_000L, burnA.getNumber());
        Assert.assertTrue(burnA.getTimeSpent() > 0);

        Assert.assertTrue(methodInfo.getTotalTime() >= burnA.getTimeSpent());
    }

    @Test
    public void testTomato() throws FileNotFoundException, IOException, IllegalClassFormatException,
            ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, NoSuchMethodException, SecurityException
    {
        String potatoClassName = "com.acme.Potato";
        String tomatoClassName = "com.acme.Tomato";

        String classNames[] = { potatoClassName, tomatoClassName };

        executeClassMethod(classNames, "testTomato1");

        ClassMap classMap = JintruderTracker.getClassMap();
        Assert.assertEquals(2, classMap.size());

        ClassInfo classInfo = classMap.get(potatoClassName.replace('.', '/'));
        MethodInfo methodInfo = classInfo.getMethodMap().get("testTomato1");

        Assert.assertTrue(methodInfo.getTotalTime() > 0);

        Assert.assertEquals(1, methodInfo.getSubCalls().size());
    }
}
