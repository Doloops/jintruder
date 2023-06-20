package org.jintruder.model.sampler;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.jintruder.model.sampler.CallStack.Location;

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

    private final Map<CallStack.Location, Boolean> requiredMethods = new HashMap<CallStack.Location, Boolean>();

    public final boolean isRequiredMethod(Location location)
    {
        if (requiresMethodPattern == null)
            return true;
        Boolean required = requiredMethods.get(location);
        if (required != null)
            return required;
        required = requiresMethodPattern.matcher(location.toString()).matches();
        requiredMethods.put(location, required);
        return required;
    }

    private final Map<CallStack.Location, Boolean> skippedMethods = new HashMap<CallStack.Location, Boolean>();

    public final boolean isSkippedMethod(Location location)
    {
        if (skipsMethodPattern == null)
            return false;
        Boolean skipped = skippedMethods.get(location);
        if (skipped != null)
            return skipped;
        skipped = skipsMethodPattern.matcher(location.toString()).matches();
        skippedMethods.put(location, skipped);
        return skipped;
    }
}
