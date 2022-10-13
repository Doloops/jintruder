package comjintruder.model;

public class MethodStack
{
    private static final int MAX_STACK_SIZE = 8192;

    private final long startTimes[] = new long[MAX_STACK_SIZE];

    private final MethodInfo methodInfos[] = new MethodInfo[MAX_STACK_SIZE];

    private int cursor = 0;

    public final boolean isEmpty()
    {
        return cursor == 0;
    }

    private final void checkValidCursor()
    {
        if (cursor <= 0 || cursor >= MAX_STACK_SIZE)
        {
            throw new IllegalArgumentException("Invalid cursor value " + cursor);
        }
    }

    public final MethodInfo peekMethodCall()
    {
        checkValidCursor();
        if (methodInfos[cursor - 1] == null)
            throw new IllegalStateException("Invalid null method here !");
        return methodInfos[cursor - 1];
    }

    public final void push(MethodInfo methodInfo, long time)
    {
        if (cursor >= MAX_STACK_SIZE)
        {
            throw new StackOverflowError(
                    "Stack overflow ! cursor=" + cursor + ", max stack size set to " + MAX_STACK_SIZE);
        }
        startTimes[cursor] = time;
        methodInfos[cursor] = methodInfo;

        cursor++;
    }

    public final void setFinishTime(long time)
    {
        checkValidCursor();
        long startTime = startTimes[cursor - 1];
        long timeSpent = time - startTime;

        MethodInfo methodInfo = methodInfos[cursor - 1];

        if (timeSpent < 0)
        {
            System.err.println("Spurious ! addCall() timeSpent=" + timeSpent + " at methodInfo="
                    + methodInfo.getMethodName() + " (startTime=" + startTime + ", time=" + time + ")");
        }
        else
        {
            methodInfo.appendInclusiveTime(timeSpent);
            methodInfo.incrementNumberOfCalls();
            if (cursor > 1)
            {
                MethodInfo parent = methodInfos[cursor - 2];
                parent.appendCallerTime(methodInfo, timeSpent);
            }
        }
    }

    public final MethodInfo pop()
    {
        MethodInfo methodInfo = peekMethodCall();
        cursor--;
        return methodInfo;
    }

    public final void dumpStack()
    {
        for (int i = 0; i < cursor; i++)
        {
            System.err.println(" [" + i + "] " + methodInfos[i].getMethodName());
        }
    }

}
