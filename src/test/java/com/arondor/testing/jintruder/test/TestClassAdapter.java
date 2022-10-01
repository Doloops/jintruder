package com.arondor.testing.jintruder.test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

import com.arondor.commons.jintruder.IntruderTracker;
import com.arondor.commons.jintruder.IntruderTransformer;
import com.arondor.commons.jintruder.collector.model.CallInfo;
import com.arondor.commons.jintruder.collector.model.ClassInfo;
import com.arondor.commons.jintruder.collector.model.ClassMap;
import com.arondor.commons.jintruder.collector.model.MethodInfo;

public class TestClassAdapter
{
    public static class ByteClassLoader extends URLClassLoader
    {
        private final Map<String, byte[]> extraClassDefs;

        public ByteClassLoader(URL[] urls, ClassLoader parent)
        {
            super(urls, parent);
            this.extraClassDefs = new HashMap<String, byte[]>();
        }

        @Override
        protected Class<?> findClass(final String className) throws ClassNotFoundException
        {
            try
            {
                rewriteClass(className);
            }
            catch (IOException | IllegalClassFormatException e)
            {
                throw new ClassNotFoundException("Could not get class " + className, e);
            }
            byte[] classBytes = this.extraClassDefs.remove(className);
            if (classBytes != null)
            {
                return defineClass(className, classBytes, 0, classBytes.length);
            }
            return super.findClass(className);
        }

        public void rewriteClass(String className)
                throws FileNotFoundException, IOException, IllegalClassFormatException
        {
            String classFile = "target/test-classes/" + className.replace('.', '/') + ".class";
            byte[] bytes = IOUtils.toByteArray(new FileInputStream(classFile));
            IntruderTransformer transformer = new IntruderTransformer();

            byte[] transformed = transformer.transform(getClass().getClassLoader(), className, getClass(), null, bytes);

            extraClassDefs.put(className, transformed);
        }
    }

    @Test
    public void testPotato_double() throws FileNotFoundException, IOException, IllegalClassFormatException,
            ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, NoSuchMethodException, SecurityException
    {
        URL[] urls = new URL[0];
        String className = "com.acme.Potato";
        String classNameWithSlash = className.replace('.', '/');

        ByteClassLoader byteClassLoader = new ByteClassLoader(urls, getClass().getClassLoader());

        Class<?> potatoClass = byteClassLoader.findClass(className);

        Object potato = potatoClass.getDeclaredConstructor().newInstance();

        java.lang.reflect.Method method = potatoClass.getDeclaredMethod("testDouble", new Class[] {});

        method.invoke(potato);

        ClassMap classMap = IntruderTracker.getClassMap();
        Assert.assertEquals(1, classMap.size());

        ClassInfo classInfo = classMap.get(classNameWithSlash);
        MethodInfo methodInfo = classInfo.getMethodMap().get("testDouble");

        CallInfo burnA = methodInfo.getSubCall(classNameWithSlash, "cpuburnA");
        Assert.assertEquals(1, burnA.getNumber());

        CallInfo burnB = methodInfo.getSubCall(classNameWithSlash, "cpuburnB");
        Assert.assertEquals(1, burnB.getNumber());

        Assert.assertTrue(methodInfo.getTotalTime() >= burnA.getTimeSpent() + burnB.getTimeSpent());
    }

    @Test
    public void testTomato1() throws FileNotFoundException, IOException, IllegalClassFormatException,
            ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, NoSuchMethodException, SecurityException
    {
        String className = "com.acme.Potato";
        String tomatoClassName = "com.acme.Tomato";

        URL[] urls = new URL[0];
        ByteClassLoader byteClassLoader = new ByteClassLoader(urls, getClass().getClassLoader());

        byteClassLoader.findClass(tomatoClassName);
        Class<?> potatoClass = byteClassLoader.findClass(className);

        Object potato = potatoClass.getDeclaredConstructor().newInstance();

        java.lang.reflect.Method method = potatoClass.getDeclaredMethod("testTomato1", new Class[] {});

        method.invoke(potato);

        ClassMap classMap = IntruderTracker.getClassMap();
        Assert.assertEquals(2, classMap.size());

        ClassInfo potatoClassInfo = classMap.get(className.replace('.', '/'));
        Assert.assertNotNull(potatoClassInfo);

        MethodInfo methodInfo = potatoClassInfo.getMethodMap().get("testTomato1");
        Assert.assertNotNull(method);

        ClassInfo tomatoClassInfo = classMap.get(tomatoClassName.replace('.', '/'));
        Assert.assertNotNull(tomatoClassInfo);

        CallInfo sleep1 = methodInfo.getSubCall(tomatoClassName.replace('.', '/'), "sleep1");
        Assert.assertEquals(1, sleep1.getNumber());

        Assert.assertTrue(methodInfo.getTotalTime() >= sleep1.getTimeSpent());
    }
}
