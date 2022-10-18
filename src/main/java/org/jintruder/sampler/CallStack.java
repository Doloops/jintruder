package org.jintruder.sampler;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class CallStack
{
    public static class CallStackLevel
    {
        private final String location;

        private long count;

        private final Map<String, CallStackLevel> children = new HashMap<String, CallStackLevel>();

        public CallStackLevel(String location)
        {
            this.location = location;
        }

        public long getCount()
        {
            return count;
        }

        public void incrementCount()
        {
            count++;
        }

        public Collection<CallStackLevel> getChildren()
        {
            return children.values();
        }

        public String getLocation()
        {
            return location;
        }

        @Override
        public int hashCode()
        {
            return location.hashCode();
        }

        @Override
        public boolean equals(Object o)
        {
            if (!(o instanceof CallStackLevel))
            {
                return false;
            }
            CallStackLevel other = (CallStackLevel) o;
            return location.equals(other.location);
        }

        public CallStackLevel addChild(String location)
        {
            CallStackLevel stack = children.get(location);
            if (stack == null)
            {
                stack = new CallStackLevel(location);
                children.put(location, stack);
            }
            return stack;
        }

        public long getSelfCount()
        {
            return count - children.values().stream().mapToLong(CallStackLevel::getCount).sum();
        }
    }

    private final Map<String, CallStackLevel> entryPoints = new HashMap<String, CallStackLevel>();

    public Collection<CallStackLevel> getEntryPoints()
    {
        return entryPoints.values();
    }

    public CallStackLevel addEntryPoint(String location)
    {
        CallStackLevel stack = entryPoints.get(location);
        if (stack == null)
        {
            stack = new CallStackLevel(location);
            entryPoints.put(location, stack);
        }
        return stack;
    }

}
