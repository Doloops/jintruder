package com.jintruder.model;

import java.util.HashMap;

public class ClassMap extends HashMap<String, ClassInfo>
{
    private static final long serialVersionUID = 1791130845648915250L;

    public ClassInfo findClass(String className)
    {
        ClassInfo classInfo = get(className);
        if (classInfo == null)
        {
            classInfo = new ClassInfo(className);
            put(className, classInfo);
        }
        return classInfo;
    }
}
