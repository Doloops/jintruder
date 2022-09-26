package com.acme;

import org.junit.Test;

public class TryFinally
{
    public void mayThrow()
    {
        throw new RuntimeException("On fire !");
    }

    @Test
    public void testNoTry()
    {
        mayThrow();
    }

    @Test
    public void testTryFinally1()
    {
        try
        {
            mayThrow();
        }
        finally
        {
            System.out.println("Finally !");
        }
    }
}
