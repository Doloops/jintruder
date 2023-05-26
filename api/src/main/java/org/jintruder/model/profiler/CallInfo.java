package org.jintruder.model.profiler;

public class CallInfo
{
    private int lineNumber = 0;

    private int depth = 0;

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

    public int getLineNumber()
    {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber)
    {
        this.lineNumber = lineNumber;
    }

    public int getDepth()
    {
        return depth;
    }

    public void setDepth(int depth)
    {
        this.depth = depth;
    }

    @Override
    public int hashCode()
    {
        return lineNumber + depth;
    }

    @Override
    public boolean equals(Object o)
    {
        if (!(o instanceof CallInfo))
            return false;
        CallInfo other = (CallInfo) o;
        return lineNumber == other.lineNumber && depth == other.depth;
    }
}
