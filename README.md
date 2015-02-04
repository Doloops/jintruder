# jintruder

The simplest way to profile Java applications !

JIntruder uses callgrind file format to output profile information (see http://valgrind.org/docs/manual/cl-format.html)

This allows you to use tools like KCacheGrind (http://kcachegrind.sourceforge.net/) to view profiling information.

To activate profiling, add to JVM arguments :
-javaagent:[path to jintruder.jar]

Class prefixes can be set with [jintruder.classes] system property, for example :
-Djintruder.classes=com.mycompany.class

Use [jintruder.dumpInterval] system property to periodically dump results in a callgrind.out.[timestamp] file.

For JBoss7, you must set -Djboss.modules.system.pkgs=$JBOSS_MODULES_SYSTEM_PKGS,com.arondor.commons.jintruder in your conf file.

