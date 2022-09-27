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

        public ByteClassLoader(URL[] urls, ClassLoader parent, Map<String, byte[]> extraClassDefs)
        {
            super(urls, parent);
            this.extraClassDefs = new HashMap<String, byte[]>(extraClassDefs);
        }

        @Override
        protected Class<?> findClass(final String name) throws ClassNotFoundException
        {
            byte[] classBytes = this.extraClassDefs.remove(name);
            if (classBytes != null)
            {
                return defineClass(name, classBytes, 0, classBytes.length);
            }
            return super.findClass(name);
        }
    }

    @Test
    public void testLoadClass() throws FileNotFoundException, IOException, IllegalClassFormatException,
            ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, NoSuchMethodException, SecurityException
    {
        byte[] bytes = IOUtils.toByteArray(new FileInputStream("target/test-classes/com/acme/Potato.class"));
        IntruderTransformer transformer = new IntruderTransformer();

        String className = "com.acme.Potato";
        byte[] transformed = transformer.transform(getClass().getClassLoader(), className, getClass(), null, bytes);

        URL[] urls = new URL[0];
        Map<String, byte[]> map = new HashMap<>();

        map.put(className, transformed);
        ByteClassLoader byteClassLoader = new ByteClassLoader(urls, getClass().getClassLoader(), map);

        Class<?> potatoClass = byteClassLoader.findClass(className);

        Object potato = potatoClass.getDeclaredConstructor().newInstance();

        java.lang.reflect.Method method = potatoClass.getDeclaredMethod("testDouble", new Class[] {});

        method.invoke(potato);

        ClassMap classMap = IntruderTracker.getClassMap();
        Assert.assertEquals(1, classMap.size());

        ClassInfo classInfo = classMap.get(className.replace('.', '/'));
        MethodInfo methodInfo = classInfo.getMethodMap().get("testDouble");

        CallInfo burnA = methodInfo.getSubCall("cpuburnA");
        Assert.assertEquals(1, burnA.getNumber());

        CallInfo burnB = methodInfo.getSubCall("cpuburnB");
        Assert.assertEquals(1, burnB.getNumber());

        Assert.assertTrue(methodInfo.getTotalTime() >= burnA.getTimeSpent() + burnB.getTimeSpent());
    }
}
