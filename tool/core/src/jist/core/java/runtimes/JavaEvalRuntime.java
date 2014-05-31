// JavaEvalRuntime.java
// jist/core
//

package jist.core.java.runtimes;

import java.io.*;
import jist.core.*;
import jist.core.java.*;

public final class JavaEvalRuntime extends JavaRuntime {

    private static final String JAVA_SOURCE_TEMPLATE =
"public final class %s implements Runnable {\n" +
"    public void run() {\n" +
"        Object __value = %s;\n" +
"        if (__value == null) {\n" +
"            __value = \"<null>\";\n" +
"        }\n" +
"        System.out.println(\"Result: \" + __value);\n" +
"        System.out.println();\n" +
"    }\n" +
"}\n";

    @Override
    protected String createImplementation(Jist jist) throws IOException {
        JistSource source = loadSource(jist, null);
        return String.format(JAVA_SOURCE_TEMPLATE, getClassName(), source.getProcessedText());
    }

    @Override
    protected void runJist(Jist jist) {
        Class<Runnable> jistClass = getClass(getFullClassName());

        Runnable jistInstance;
        try {
            jistInstance = jistClass.newInstance();
            jistInstance.run();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
