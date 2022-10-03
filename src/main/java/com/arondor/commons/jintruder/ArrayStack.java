package com.arondor.commons.jintruder;

import java.util.Arrays;

public class ArrayStack<E>
{
    private int size = 0;

    private static final int DEFAULT_CAPACITY = 64;

    private final int maximumCapacity;

    private Object elements[];

    public ArrayStack(int maximumCapacity)
    {
        elements = new Object[DEFAULT_CAPACITY];
        this.maximumCapacity = maximumCapacity;
    }

    public final boolean push(E e)
    {
        if (size == maximumCapacity)
        {
            if (false)
                System.err.println("Will not push element " + e.getClass().getName()
                        + " because stack has reached maximum capacity " + size);
            return false;
        }
        if (size == elements.length)
        {
            int newSize = elements.length + DEFAULT_CAPACITY;
            elements = Arrays.copyOf(elements, newSize);
        }
        elements[size++] = e;
        return true;
    }

    public final boolean add(E e)
    {
        return push(e);
    }

    @SuppressWarnings("unchecked")
    public final E pop()
    {
        if (size == 0)
            return null;
        E e = (E) elements[--size];
        elements[size] = null;
        return e;
    }

    public final int size()
    {
        return size;
    }

    public final boolean isEmpty()
    {
        return size == 0;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < size; i++)
        {
            sb.append(elements[i].toString());
            if (i < size - 1)
            {
                sb.append(",");
            }
        }
        sb.append(']');
        return sb.toString();
    }
}
