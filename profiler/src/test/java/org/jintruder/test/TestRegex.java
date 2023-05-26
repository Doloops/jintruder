package org.jintruder.test;

import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Test;

public class TestRegex
{
    @Test
    public void testCGLib()
    {
        String blacklistRegex = ".*CGLIB.*";

        String className = "com.bnpp.cardif.sugar.dao.oracle.folder.FolderOracleDAO$$EnhancerByCGLIB$$30059672";

        Pattern pattern = Pattern.compile(blacklistRegex);
        if (pattern.matcher(className).matches())
        {
            return;
        }
        Assert.fail("Should have matched !");
    }

    @Test
    public void testThreadNames()
    {
        String threadRegex = "SourceWorker-.*|TaskWorker-.*";

        Pattern pattern = Pattern.compile(threadRegex);
        Assert.assertTrue(pattern.matcher("SourceWorker-Default#0").matches());
        Assert.assertTrue(pattern.matcher("TaskWorker-Default#0").matches());
    }
}
