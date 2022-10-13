package com.jintruder.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ClassMap
{
    private static final long serialVersionUID = 1791130845648915250L;

    private final Map<String, ClassInfo> classes = new HashMap<String, ClassInfo>();

    private final Set<MethodInfo> entryPoints = new HashSet<MethodInfo>();

    public ClassInfo findClass(String className)
    {
        ClassInfo classInfo = classes.get(className);
        if (classInfo == null)
        {
            classInfo = new ClassInfo(className);
            classes.put(className, classInfo);
        }
        return classInfo;
    }

    public Collection<ClassInfo> classInfos()
    {
        return classes.values();
    }

    public void clear()
    {
        classes.clear();
        entryPoints.clear();
    }

    public ClassInfo getClassInfo(String className)
    {
        return classes.get(className);
    }

    public int size()
    {
        return classes.size();
    }

    public Collection<MethodInfo> getEntryPoints()
    {
        return entryPoints;
    }

    public void addEntryPoint(MethodInfo methodInfo)
    {
        entryPoints.add(methodInfo);
    }
}
