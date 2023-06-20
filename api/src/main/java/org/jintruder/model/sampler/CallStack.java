package org.jintruder.model.sampler;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class CallStack
{
    public static class Location
    {
        private final String className;

        private final String methodName;

        private final int line;

        private final int hashCode;

        public Location(String className, String methodName, int line)
        {
            this.className = className;
            this.methodName = methodName;
            this.line = line;
            this.hashCode = (className.hashCode() << 7) | methodName.hashCode() + (line << 28);
        }

        @Override
        public int hashCode()
        {
            return hashCode;
        }

        @Override
        public boolean equals(Object other_)
        {
            if (this == other_)
                return true;
            if (!(other_ instanceof Location))
                return false;
            Location other = (Location) other_;
            boolean eq = className.equals(other.className) && methodName.equals(other.methodName) && line == other.line;
            boolean strEq = toString().equals(other.toString());
            if (eq != strEq)
            {
                throw new IllegalArgumentException(
                        "Invalid equals() : this=" + toString() + ", other=" + other.toString());
            }
            return eq;
        }

        @Override
        public String toString()
        {
            return className + ":" + methodName + "(" + line + ")";
        }

        public static Location fromString(String value)
        {
            int clmnIndex = value.indexOf(':');
            int parenthesisIndex = value.indexOf('(', clmnIndex);
            int endIndex = value.indexOf(')', parenthesisIndex);
            if (clmnIndex == -1 || parenthesisIndex == -1 || endIndex == -1 || endIndex != value.length() - 1)
            {
                throw new IllegalArgumentException("Invalid Location string: " + value);
            }
            String className = value.substring(0, clmnIndex);
            String methodName = value.substring(clmnIndex + 1, parenthesisIndex);
            int line = Integer.parseInt(value.substring(parenthesisIndex + 1, endIndex));
            return new Location(className, methodName, line);
        }
    }

    private static class LocationMap
    {
        private final Map<Location, CallStackItem> children = new HashMap<Location, CallStackItem>();

        public Collection<CallStackItem> values()
        {
            return children.values();
        }

        public CallStackItem get(Location location)
        {
            return children.get(location);
        }

        public void put(Location location, CallStackItem item)
        {
            children.put(location, item);
        }
    }

    public static class CallStackItem
    {
        private final Location location;

        private long count;

        private final LocationMap children = new LocationMap();

        public CallStackItem(Location location)
        {
            this.location = location;
        }

        public long getCount()
        {
            return count;
        }

        public void setCount(long count)
        {
            this.count = count;
        }

        public void incrementCount()
        {
            count++;
        }

        public Collection<CallStackItem> getChildren()
        {
            return children.values();
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
            return location == other.location;
        }

        private CallStackItem addChild(Location location)
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
    }

    private final LocationMap entryPoints = new LocationMap();

    public Collection<CallStackItem> getEntryPoints()
    {
        return entryPoints.values();
    }

    public CallStackItem addEntryPoint(Location location)
    {
        CallStackItem stack = entryPoints.get(location);
        if (stack == null)
        {
            stack = new CallStackItem(location);
            entryPoints.put(location, stack);
        }
        return stack;
    }

    public CallStackItem addChild(CallStackItem item, Location location)
    {
        return item.addChild(location);
    }

    public Location getLocation(CallStackItem item)
    {
        return item.location;
    }
}
