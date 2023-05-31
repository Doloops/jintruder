package org.jintruder.model.sampler;

import java.util.regex.Pattern;

public class StackTraceFilter
{
    private final Pattern threadPattern;

    private final Pattern requiresMethodPattern;

    private final Pattern skipsMethodPattern;

    public StackTraceFilter(String threadString, String requiresMethodString, String skipsMethodString)
    {
        this.threadPattern = threadString != null ? Pattern.compile(threadString) : null;
        this.requiresMethodPattern = requiresMethodString != null ? Pattern.compile(requiresMethodString) : null;
        this.skipsMethodPattern = skipsMethodString != null ? Pattern.compile(skipsMethodString) : null;
    }

    public final boolean filterThread(Thread thread)
    {
        if (thread == null || thread.getName() == null)
            return false;
        if (threadPattern != null && !threadPattern.matcher(thread.getName()).matches())
            return false;
        return true;
    }

    public final boolean hasRequiredMethod()
    {
        return requiresMethodPattern != null;
    }

    public final boolean isRequiredMethod(String location)
    {
        return requiresMethodPattern == null || requiresMethodPattern.matcher(location).matches();
    }

    public final boolean isSkippedMethod(String location)
    {
        return skipsMethodPattern != null && skipsMethodPattern.matcher(location).matches();
    }
}
