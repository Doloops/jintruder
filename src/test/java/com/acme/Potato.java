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

    public void cpuburnA(int howmany)
    {
        int j = 0;
        for (int i = 0; i < howmany; i++)
        {
            j++;
        }
        Assert.assertEquals(howmany, j);
    }

    public void cpuburnB(int howmany)
    {
        int j = 0;
        for (int i = 0; i < howmany; i++)
        {
            j++;
        }
        Assert.assertEquals(howmany, j);
    }

    @Test
    public void testDouble()
    {
        cpuburnA(10_000);
        cpuburnB(20_000);
    }

    @Test
    public void testTomato1()
    {
        Tomato tomato = new Tomato();
        tomato.sleep1();
    }
}
