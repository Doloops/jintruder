package com.jintruder;

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

    public static boolean isEnableDecoration()
    {
        return getBooleanProperty("jintruder.decoration", false);
    }

    public static boolean isEnableSampling()
    {
        return getBooleanProperty("jintruder.sampling", true);
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
        return getIntegerProperty("jintruder.dumpInterval", 10_000);
    }

    public static int getSamplingInterval()
    {
        return getIntegerProperty("jintruder.samplingInterval", 10);
    }
}
