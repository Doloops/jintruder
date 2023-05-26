package org.jintruder.model.sampler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CallStack
{
    private static class LocationMap
    {
        private final Map<Integer, CallStackItem> children = new HashMap<Integer, CallStackItem>();

        public Collection<CallStackItem> values()
        {
            return children.values();
        }

        public CallStackItem get(int location)
        {
            return children.get(location);
        }

        public void put(int location, CallStackItem item)
        {
            children.put(location, item);
        }
    }

    public static class CallStackItem
    {
        private final int locationId;

        private long count;

        private final LocationMap children = new LocationMap();

        public CallStackItem(int locationId)
        {
            this.locationId = locationId;
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
            return locationId;
        }

        @Override
        public boolean equals(Object o)
        {
            if (!(o instanceof CallStackItem))
            {
                return false;
            }
            CallStackItem other = (CallStackItem) o;
            return locationId == other.locationId;
        }

        private CallStackItem addChild(int location)
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

    private final Map<String, Integer> locationsDirectory = new HashMap<String, Integer>();

    private final List<String> locations = new ArrayList<String>();

    private int getLocationId(String location)
    {
        Integer id = locationsDirectory.get(location);
        if (id == null)
        {
            locations.add(location);
            id = locations.size() - 1;
            locationsDirectory.put(location, id);
        }
        return id;
    }

    private final LocationMap entryPoints = new LocationMap();

    public Collection<CallStackItem> getEntryPoints()
    {
        return entryPoints.values();
    }

    public CallStackItem addEntryPoint(String location)
    {
        int locationId = getLocationId(location);
        CallStackItem stack = entryPoints.get(locationId);
        if (stack == null)
        {
            stack = new CallStackItem(locationId);
            entryPoints.put(locationId, stack);
        }
        return stack;
    }

    public CallStackItem addChild(CallStackItem item, String location)
    {
        int locationId = getLocationId(location);
        return item.addChild(locationId);
    }

    public String getLocation(CallStackItem item)
    {
        return locations.get(item.locationId);
    }
}
