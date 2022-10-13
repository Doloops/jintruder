package com.jintruder.test;

public class TestSimple
{
    public static void main(String args[])
    {
        long start = System.currentTimeMillis();
        new TestSimple().test();
        long end = System.currentTimeMillis();
        System.out.println("Total spent : " + (end - start) + "ms");
    }

    private void test()
    {
        String ursule = "ursule";
        System.out.println("Writing ursule : " + ursule);

        testMulti(42);
        testMulti("Univers");
        test1(ursule);
        testRecursif(0, 827);

        testCyclic(0, 30);
    }

    private void testMulti(int i)
    {
        System.out.println("i=" + i);
    }

    private void testMulti(String s)
    {
        System.out.println("s=" + s);
    }

    private void test1(String ursule)
    {
        try
        {
            Thread.sleep(30);
        }
        catch (InterruptedException e)
        {
        }
        ursule += 67643;
        String res = test2(ursule);

        System.out.println("Post ursule : " + ursule);
        System.out.println("Res : " + res);
    }

    private String test2(String ursule)
    {
        try
        {
            Thread.sleep(20);
        }
        catch (InterruptedException e)
        {
        }
        System.out.println("Cucu " + ursule);
        return "ursule:" + ursule.toUpperCase();
    }

    public void testRecursif(int depth, int maxDepth)
    {
        try
        {
            Thread.sleep(8);
        }
        catch (InterruptedException e)
        {
        }
        if (depth == maxDepth)
        {
            return;
        }
        testRecursif(depth + 1, maxDepth);
    }

    private void testCyclic(int i, int j)
    {
        if (i >= j)
        {
            return;
        }
        if (i % 2 == 0)
        {
            testCyclicEven(i + 1, j);
        }
        else
        {
            testCyclicOdd(i + 1, j);
        }
    }

    private void testCyclicOdd(int i, int j)
    {
        try
        {
            Thread.sleep(2);
        }
        catch (InterruptedException e)
        {
        }
        testCyclic(i, j);
    }

    private void testCyclicEven(int i, int j)
    {
        try
        {
            Thread.sleep(3);
        }
        catch (InterruptedException e)
        {
        }
        testCyclic(i, j);
    }
}
