package com.jintruder.model;

public class CallInfo
{
    private long number = 0;

    private long timeSpent = 0;

    public CallInfo()
    {

    }

    public long getNumber()
    {
        return number;
    }

    public void setNumber(int number)
    {
        this.number = number;
    }

    public long getTimeSpent()
    {
        return timeSpent;
    }

    public void appendCalledTime(long timeSpent)
    {

        this.timeSpent += timeSpent;
    }

    public void addCalled()
    {
        this.number++;
    }
}
