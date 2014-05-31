// JavaExecutor.java
// jist/core
//

package jist.core.java;

import java.util.*;
import javax.tools.*;
import jist.util.*;

final class JavaClassFactory {

    private final JarDependencies _dependencies;
    private JavaClassManager _classManager;

    public JavaClassFactory(JarDependencies dependencies) {
        _dependencies = dependencies;
    }

    public boolean compile(List<JavaFile> compilationUnits) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        List<String> options = new ArrayList<String>();

        String classPath = _dependencies.getClassPath();
        if (Strings.hasValue(classPath)) {
            options.add("-classpath");
            options.add(classPath);
        }

        _classManager = new JavaClassManager(compiler.getStandardFileManager(null, null, null),
                                             _dependencies.getClassLoader());
        return compiler.getTask(null, _classManager, null, options, null, compilationUnits)
                       .call();
    }

    @SuppressWarnings("unchecked")
    public <T> Class<T> getClass(String fullName) {
        try {
            return (Class<T>)_classManager.getClassLoader(null).loadClass(fullName);
        }
        catch (ClassNotFoundException e) {
            return null;
        }
    }
}
