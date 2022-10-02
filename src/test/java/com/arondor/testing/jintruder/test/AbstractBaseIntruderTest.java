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

    public void executeClassMethod(String className, String methodName) throws FileNotFoundException, IOException,
            IllegalClassFormatException, ClassNotFoundException, InstantiationException, IllegalAccessException,
            IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException
    {
        String classNames[] = new String[1];
        classNames[0] = className;
        executeClassMethod(classNames, methodName);
    }

    public void executeClassMethod(String classNames[], String methodName) throws FileNotFoundException, IOException,
            IllegalClassFormatException, ClassNotFoundException, InstantiationException, IllegalAccessException,
            IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException
    {
        forgeClassMethodRunnable(classNames, methodName).run();
    }

    public Runnable forgeClassMethodRunnable(String classNames[], String methodName)
            throws FileNotFoundException, IOException, IllegalClassFormatException, ClassNotFoundException,
            InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException,
            NoSuchMethodException, SecurityException
    {
        URL[] urls = new URL[0];
        try (ByteClassLoader byteClassLoader = new ByteClassLoader(urls, getClass().getClassLoader()))
        {
            Class<?> classes[] = new Class<?>[classNames.length];
            for (int i = 0; i < classNames.length; i++)
            {
                byteClassLoader.rewriteClass(classNames[i]);
                classes[i] = byteClassLoader.findClass(classNames[i]);
            }
            Class<?> clazz = classes[0];
            Object potato = clazz.getDeclaredConstructor().newInstance();

            java.lang.reflect.Method method = clazz.getDeclaredMethod(methodName, new Class[] {});

            return new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        method.invoke(potato);
                    }
                    catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
                    {
                        throw new RuntimeException("Caught : " + e.getClass().getName() + " : " + e.getMessage(), e);
                    }
                }
            };
        }
        finally
        {

        }
    }
}
