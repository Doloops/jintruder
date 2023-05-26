package org.jintruder.model;

import java.text.MessageFormat;

public class JintruderConfig
{
    private static void log(String pattern, Object... vars)
    {
        System.err.println(MessageFormat.format(pattern, vars));
    }

    private static boolean getBooleanProperty(String name, boolean defaultValue)
    {
        String sValue = System.getProperty(name);
        if (sValue != null)
        {
            return sValue.trim().equalsIgnoreCase("true");
        }
        return defaultValue;
    }

    private static int getIntegerProperty(String name, int defaultValue)
    {
        String sInterval = System.getProperty(name);
        if (sInterval != null)
        {
            return Integer.parseInt(sInterval);
        }
        return defaultValue;
    }

    private static final boolean VERBOSE = getBooleanProperty("jintruder.log", false);

    public static boolean isVerbose()
    {
        return VERBOSE;
    }

    private static boolean DEFAULT_ENABLE_PROFILER = false;

    public static void enableProfiler()
    {
        DEFAULT_ENABLE_PROFILER = true;
    }

    public static boolean isEnableProfiler()
    {
        return getBooleanProperty("jintruder.decoration", DEFAULT_ENABLE_PROFILER);
    }

    private static boolean DEFAULT_ENABLE_SAMPLER = false;

    public static void enableSampler()
    {
        DEFAULT_ENABLE_SAMPLER = true;
    }

    public static boolean isEnableSampling()
    {
        return getBooleanProperty("jintruder.sampling", DEFAULT_ENABLE_SAMPLER);
    }

    public static boolean isDumpBytecode()
    {
        return getBooleanProperty("jintruder.dump.bytecode", false);
    }

    public static String getClassesWildcard()
    {
        return System.getProperty("jintruder.classes");
    }

    public static int getDumpInterval()
    {
        return getIntegerProperty("jintruder.dumpInterval", 0);
    }

    public static String getDumpFile()
    {
        return System.getProperty("jintruder.dumpFile", "dump.json");
    }

    public static int getSamplingInterval()
    {
        return getIntegerProperty("jintruder.samplingInterval", 10);
    }

    public static String getSamplingThreadPattern()
    {
        return System.getProperty("jintruder.threadsToWatch");
    }

}
