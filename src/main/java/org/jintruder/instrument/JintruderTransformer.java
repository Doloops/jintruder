package org.jintruder.instrument;

import java.io.FileOutputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.jintruder.JintruderConfig;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

public class JintruderTransformer implements ClassFileTransformer
{
    private List<String> tracedClassPrefixes = new ArrayList<String>();

    private List<String> tracedClassBlacklist = new ArrayList<String>();

    private List<String> tracedClassRegexBlacklist = new ArrayList<String>();

    private final boolean isLog()
    {
        return JintruderConfig.isVerbose();
    }

    private final void log(String message)
    {
        System.err.println(message);
    }

    public JintruderTransformer()
    {
        String intruderClasses = JintruderConfig.getClassesWildcard();
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
        if (!JintruderConfig.isEnableDecoration())
        {
            return false;
        }

        if (className.startsWith("com/arondor/commons/jintruder") || className.startsWith("java/")
                || className.startsWith("sun/"))
        {
            return false;
        }

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
        if (isClassTraced(className))
        {
            try
            {
                ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
                ClassVisitor ca = new JintruderClassAdapter(cw);
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
                byte[] retVal = cw.toByteArray();

                if (JintruderConfig.isDumpBytecode())
                {
                    FileOutputStream fos = new FileOutputStream("/tmp/" + className.replace('/', '_') + ".class");
                    fos.write(retVal);
                    fos.close();
                }
                return retVal;
            }
            catch (TypeNotPresentException e)
            {
                log("Caught exception : " + e);
            }
            catch (RuntimeException e)
            {
                log("Caught exception : " + e);
                e.printStackTrace();
            }
            catch (Throwable e)
            {
                log("Caught exception : " + e);
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
        return classfileBuffer;
    }

}
