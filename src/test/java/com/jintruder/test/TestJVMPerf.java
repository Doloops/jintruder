package com.jintruder.test;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class TestJVMPerf
{
    @Test
    public void baseline()
    {
        final int modulo = 64 * 1024 * 1024;
        int j = 0;

        long tests = 4_000_000_000L;

        for (long nb = 0; nb < tests; nb++)
        {
            j = (int) (nb % modulo);
        }
        Assert.assertEquals((tests - 1) % modulo, j);
    }

    private void array(final long modulo)
    {
        int j[] = new int[(int) modulo];

        long tests = 4_000_000_000L;

        for (long nb = 0; nb < tests; nb++)
        {
            int v = (int) (nb % modulo);
            j[v] = v;
        }
        Assert.assertEquals((tests - 1) % modulo, j[(int) ((tests - 1L) % modulo)]);
    }

    private void doTestArray_Raw64()
    {
        final int modulo = 64;
        int j[] = new int[modulo];

        long tests = 4_000_000_000L;

        for (long nb = 0; nb < tests; nb++)
        {
            int v = (int) (nb % modulo);
            j[v] = v;
        }
        Assert.assertEquals((tests - 1) % modulo, j[(int) ((tests - 1L) % modulo)]);
    }

    @Test
    public void testArrayRaw_64()
    {
        doTestArray_Raw64();
    }

    private void measure_array(final int modulo)
    {
        long start = System.currentTimeMillis();
        array(modulo);
        System.err.println("Modulo " + modulo + " took " + (System.currentTimeMillis() - start) + "ms");
    }

    @Test
    public void testArray_64()
    {
        measure_array(64);
    }

    @Test
    public void testArray()
    {
        measure_array(64);
        measure_array(256);
        measure_array(1024);
        measure_array(64 * 1024 * 1024);

    }
}
