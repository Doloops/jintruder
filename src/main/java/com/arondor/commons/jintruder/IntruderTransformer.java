package com.arondor.commons.jintruder;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

public class IntruderTransformer implements ClassFileTransformer
{
    public static void premain(String agentArgs, Instrumentation inst)
    {
        inst.addTransformer(new IntruderTransformer());
    }

    private List<String> tracedClassPrefixes = new ArrayList<String>();

    private List<String> tracedClassBlacklist = new ArrayList<String>();

    private List<String> tracedClassRegexBlacklist = new ArrayList<String>();

    public static boolean INTRUDER_NO_DECORATION = false;

    public static boolean INTRUDER_LOG = false;

    private boolean getBooleanProperty(String name)
    {
        String sValue = System.getProperty(name);
        if (sValue != null)
        {
            return sValue.trim().equalsIgnoreCase("true");
        }
        return false;
    }

    private final boolean isLog()
    {
        return INTRUDER_LOG;
    }

    private final void log(String message)
    {
        System.err.println(message);
    }

    public IntruderTransformer()
    {
        INTRUDER_NO_DECORATION = getBooleanProperty("jintruder.nodecoration");
        INTRUDER_LOG = getBooleanProperty("jintruder.log");
        String intruderClasses = System.getProperty("jintruder.classes");
        if (intruderClasses == null || intruderClasses.trim().isEmpty())
        {
            intruderClasses = "com:org";
        }
        intruderClasses = intruderClasses.replace('.', '/');
        for (String clazz : intruderClasses.split(":"))
        {
            tracedClassPrefixes.add(clazz);
            if (isLog())
            {
                log("Tracing prefix : " + clazz);
            }
        }

        tracedClassBlacklist.add("org/objectweb");
        tracedClassBlacklist.add("org/xml");
        tracedClassBlacklist.add("com/arondor/common/management");
        tracedClassBlacklist.add("com/arondor/common/jintruder");
        tracedClassBlacklist.add("com/sun");

        tracedClassRegexBlacklist.add(".*CGLIB.*");
    }

    private final boolean isClassTraced(final String className)
    {
        if (INTRUDER_NO_DECORATION)
        {
            return false;
        }

        if (className.startsWith("com/arondor/commons/jintruder") || className.startsWith("java/")
                || className.startsWith("sun/"))
        {
            return false;
        }

        return doIsClassTraced(className);
    }

    private boolean doIsClassTraced(final String className)
    {
        for (String prefix : tracedClassPrefixes)
        {
            if (className.startsWith(prefix))
            {
                for (String blacklist : tracedClassBlacklist)
                {
                    if (className.startsWith(blacklist))
                    {
                        return false;
                    }
                }
                for (String blacklistRegex : tracedClassRegexBlacklist)
                {
                    if (isLog())
                    {
                        log("Matching pattern '" + blacklistRegex + "' against '" + className + "'");
                    }
                    Pattern pattern = Pattern.compile(blacklistRegex);
                    if (pattern.matcher(className).matches())
                    {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException
    {
        byte[] retVal = null;
        if (isClassTraced(className))
        {
            try
            {
                ClassWriter cw = new ClassWriter(0);
                ClassVisitor ca = new IntruderClassAdapter(cw);
                ClassReader cr = new ClassReader(classfileBuffer);

                if (isLog())
                {
                    log("Transforming Class : " + className + ", Cr access : 0x" + Integer.toHexString(cr.getAccess()));
                }
                if ((cr.getAccess() & (Opcodes.ACC_INTERFACE)) != 0)
                {
                    return null;
                }
                cr.accept(ca, 0);
                retVal = cw.toByteArray();
            }
            catch (RuntimeException e)
            {
                log("Catched : " + e);
                e.printStackTrace();
            }
            catch (Throwable e)
            {
                log("Catched : " + e);
                e.printStackTrace();
            }
            finally
            {
                if (isLog())
                {
                    log("Transformed class : " + className);
                }
            }
        }
        return retVal;
    }

}
