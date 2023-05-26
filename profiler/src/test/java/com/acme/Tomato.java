package com.acme;

public class Tomato
{
    public void doSleep(int howmuch)
    {
        try
        {
            Thread.sleep(howmuch);
        }
        catch (InterruptedException e)
        {
        }
    }

    public void sleep1()
    {
        doSleep(100);
        ;
        sleep2();
    }

    public void sleep2()
    {
        doSleep(400);
        sleep3();
    }

    public void sleep3()
    {
        doSleep(50);
    }

}
