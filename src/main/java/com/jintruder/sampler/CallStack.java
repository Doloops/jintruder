package com.jintruder.sampler;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class CallStack
{
    public static class SelfMap<T>
    {
        private final Map<T, T> map = new HashMap<T, T>();

        public T get(T key)
        {
            T value = map.get(key);
            if (value == null)
            {
                map.put(key, key);
                return key;
            }
            else
            {
                return value;
            }
        }

        public void set(T key)
        {
            map.put(key, key);
        }

        public Collection<T> values()
        {
            return map.values();
        }

    }

    public static class CallStackLevel
    {
        private final String className;

        private final String methodName;

        private final int depth;

        private long count;

        private final SelfMap<CallStackLevel> children = new SelfMap<CallStackLevel>();

        public CallStackLevel(String className, String methodName, int depth)
        {
            this.className = className;
            this.methodName = methodName;
            this.depth = depth;
        }

        public String getClassName()
        {
            return className;
        }

        public String getMethodName()
        {
            return methodName;
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

        public int getDepth()
        {
            return depth;
        }

        public String getClassAndMethodName()
        {
            return className + "." + methodName;
        }

        @Override
        public int hashCode()
        {
            return className.hashCode() + methodName.hashCode();
        }

        @Override
        public boolean equals(Object o)
        {
            if (!(o instanceof CallStackLevel))
            {
                return false;
            }
            CallStackLevel other = (CallStackLevel) o;
            return className.equals(other.className) && methodName.equals(other.methodName);
        }

        public CallStackLevel addChild(String className, String methodName, int depth)
        {
            CallStackLevel stack = new CallStackLevel(className, methodName, depth);
            return children.get(stack);
        }
    }

    private final SelfMap<CallStackLevel> entryPoints = new SelfMap<CallStackLevel>();

    public Collection<CallStackLevel> getEntryPoints()
    {
        return entryPoints.values();
    }

    public CallStackLevel addEntryPoint(String className, String methodName, int depth)
    {
        CallStackLevel stack = new CallStackLevel(className, methodName, depth);
        return entryPoints.get(stack);
    }

}
