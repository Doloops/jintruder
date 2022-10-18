package org.jintruder.sampler;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class CallStack
{
    public static class CallStackItem
    {
        private final String location;

        private long count;

        private final Map<String, CallStackItem> children = new HashMap<String, CallStackItem>();

        public CallStackItem(String location)
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

        public Collection<CallStackItem> getChildren()
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
            if (!(o instanceof CallStackItem))
            {
                return false;
            }
            CallStackItem other = (CallStackItem) o;
            return location.equals(other.location);
        }

        public CallStackItem addChild(String location)
        {
            CallStackItem stack = children.get(location);
            if (stack == null)
            {
                stack = new CallStackItem(location);
                children.put(location, stack);
            }
            return stack;
        }

        public long getSelfCount()
        {
            return count - children.values().stream().mapToLong(CallStackItem::getCount).sum();
        }

        public void setCount(long count)
        {
            this.count = count;
        }
    }

    private final Map<String, CallStackItem> entryPoints = new HashMap<String, CallStackItem>();

    public Collection<CallStackItem> getEntryPoints()
    {
        return entryPoints.values();
    }

    public CallStackItem addEntryPoint(String location)
    {
        CallStackItem stack = entryPoints.get(location);
        if (stack == null)
        {
            stack = new CallStackItem(location);
            entryPoints.put(location, stack);
        }
        return stack;
    }

}
