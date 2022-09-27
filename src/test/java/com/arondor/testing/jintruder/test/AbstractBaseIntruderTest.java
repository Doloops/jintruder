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

import com.arondor.commons.jintruder.IntruderTransformer;

public abstract class AbstractBaseIntruderTest
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

    public void executeClassMethod(String className, String methodName) throws FileNotFoundException, IOException,
            IllegalClassFormatException, ClassNotFoundException, InstantiationException, IllegalAccessException,
            IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException
    {
        byte[] bytes = IOUtils
                .toByteArray(new FileInputStream("target/test-classes/" + className.replace('.', '/') + ".class"));
        IntruderTransformer transformer = new IntruderTransformer();

        // String className = "com.acme.Potato";
        byte[] transformed = transformer.transform(getClass().getClassLoader(), className, getClass(), null, bytes);

        URL[] urls = new URL[0];
        Map<String, byte[]> map = new HashMap<>();

        map.put(className, transformed);
        try (ByteClassLoader byteClassLoader = new ByteClassLoader(urls, getClass().getClassLoader(), map))
        {

            Class<?> potatoClass = byteClassLoader.findClass(className);

            Object potato = potatoClass.getDeclaredConstructor().newInstance();

            java.lang.reflect.Method method = potatoClass.getDeclaredMethod(methodName, new Class[] {});

            method.invoke(potato);
        }
        finally
        {

        }
    }
}
