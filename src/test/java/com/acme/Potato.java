package com.acme;

import org.junit.Assert;
import org.junit.Test;

public class Potato
{
    @Test
    public void testSimple()
    {
        int j = 0;
        for (int i = 0; i < 1_000; i++)
        {
            j++;
        }
        Assert.assertEquals(1_000, j);
    }

    public void cpuburnA(long howmany)
    {
        long j = 0;
        for (long i = 0; i < howmany; i++)
        {
            j++;
        }
        Assert.assertEquals(howmany, j);
    }

    public void cpuburnB(long howmany)
    {
        long j = 0;
        for (long i = 0; i < howmany; i++)
        {
            j++;
        }
        Assert.assertEquals(howmany, j);
    }

    @Test
    public void testDouble()
    {
        cpuburnA(2_000_000_000L);
        cpuburnB(4_000_000_000L);
    }

    @Test
    public void testALot()
    {
        for (long i = 0; i < 2_000_000_000; i++)
        {
            cpuburnA(5);
            if (false)
            {
                if (i % 10_000_000 == 0)
                {
                    try
                    {
                        Thread.sleep(1);
                    }
                    catch (InterruptedException e)
                    {
                    }
                }
            }
        }
    }

    @Test
    public void testWithException()
    {
        try
        {
            thrownButCaught();
            Assert.fail("Should have caught !");
        }
        catch (RuntimeException e)
        {

        }
    }

    private void thrownButCaught()
    {
        Tomato tomato = new Tomato();
        tomato.sleep3();
        doThrow();
    }

    private void doThrow()
    {
        throw new RuntimeException("Fire !");
    }

    @Test
    public void testTomato1()
    {
        Tomato tomato = new Tomato();
        tomato.sleep1();
    }
}
