// MavenModuleManager.java
// jist/core
//

package jist.core.java;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.jar.*;
import jist.core.*;
import jist.util.*;

public final class JarDependencies implements JistDependencies {

    private static final String FILE_SCHEME = "file";
    private static final String MAVEN_SCHEME = "maven";

    private static final String JIST_MODULE_IMPORT_ATTRIBUTE = "Jist-Module-Import";

    private final String _mavenPath;
    private final String _mavenRepositoryPath;

    private final HashSet<URI> _moduleURIs;
    private final List<Module> _modules;

    public JarDependencies(JistOptions options) {
        _mavenPath = options.getMavenPath();
        _mavenRepositoryPath = options.getMavenRepository();

        _moduleURIs = new HashSet<URI>();
        _modules = new ArrayList<Module>();
    }

    private String getModuleImport(String moduleJar) {
        JarFile jar = null;

        try {
            jar = new JarFile(moduleJar);

            Manifest manifest = jar.getManifest();
            if (manifest != null) {
                Attributes attributes = manifest.getMainAttributes();
                if (attributes != null) {
                    return attributes.getValue(JIST_MODULE_IMPORT_ATTRIBUTE);
                }
            }
        }
        catch (IOException e) {
        }
        finally {
            if (jar != null) {
                try {
                    jar.close();
                }
                catch (IOException e) {
                }
            }
        }

        return null;
    }

    private List<String> resolveModules() throws JistErrorException {
        ArrayList<String> jars = new ArrayList<String>();

        for (Module module : _modules) {
            String jar = module.resolve();
            jars.add(jar);
        }

        return jars;
    }

    private boolean supportsMavenModules() {
        return Strings.hasValue(_mavenRepositoryPath) && Strings.hasValue(_mavenPath);
    }

    @Override
    public void addModule(URI moduleURI) throws JistErrorException {
        String scheme = moduleURI.getScheme();
        boolean mavenURI = false;

        if (!scheme.equals(MAVEN_SCHEME) && !scheme.equals(FILE_SCHEME)) {
            throw new JistErrorException("The module url must either be a local file or a maven artifact.");
        }

        if (scheme.equals(MAVEN_SCHEME)) {
            if (!supportsMavenModules()) {
                throw new JistErrorException("Maven could not be found.");
            }

            mavenURI = true;
        }

        if (_moduleURIs.add(moduleURI)) {
            _modules.add(new Module(moduleURI, mavenURI));
        }
    }

    @Override
    public void resolveModules(JistSession session) throws JistErrorException {
        List<String> jars = resolveModules();

        if (jars.size() != 0) {
            StringBuilder sb = new StringBuilder();
            URL[] urls = new URL[jars.size()];

            for (int i = 0; i < urls.length; i++) {
                String jar = jars.get(i);

                try {
                    if (i != 0) {
                        sb.append(File.pathSeparatorChar);
                    }
                    sb.append(jar);

                    URL url = new URL("jar", "", "file://" + jar + "!/");
                    urls[i] = url;

                    String moduleImport = getModuleImport(jar);
                    if (Strings.hasValue(moduleImport)) {
                        session.addStaticImport(moduleImport);
                    }
                }
                catch (Exception e) {
                    throw new JistErrorException("Unable to load a required jar " + jar);
                }
            }

            String classPath = sb.toString();
            URLClassLoader classLoader = new URLClassLoader(urls);

            session.useDependencies(classPath, classLoader);
        }
    }

    private final class Module {

        public String _jar;
        public String _artifact;
        public boolean _requiresResolution;

        public Module(URI uri, boolean maven) {
            _jar = uri.getPath();

            if (maven) {
                String[] pathParts = uri.getPath().split("/");
                String groupId = pathParts[1];
                String artifactId = pathParts[2];
                String version = pathParts[3];

                String path = groupId.replace('.', File.separatorChar) + File.separator +
                              artifactId + File.separator + version + File.separator +
                              artifactId + "-" + version + ".jar";

                File file = new File(_mavenRepositoryPath, path);
                _jar = file.getPath();

                _artifact = groupId + ":" + artifactId + ":" + version;
                _requiresResolution = !file.exists();
            }
        }

        public String resolve() throws JistErrorException {
            if (_requiresResolution) {
                String[] commandParts = new String[] {
                    _mavenPath,
                    "org.apache.maven.plugins:maven-dependency-plugin:2.8:get",
                    "-DremoteRepositories=central::default::http://repo1.maven.apache.org/maven2",
                    "-Dtransitive=false",
                    "-Dartifact=" + _artifact
                };

                Runtime runtime = Runtime.getRuntime();
                try {
                    Process process = runtime.exec(commandParts);
                    process.waitFor();
                }
                catch (Exception e) {
                    throw new JistErrorException("Could not execute maven to resolve " + _artifact, e);
                }

                File file = new File(_jar);
                if (!file.exists()) {
                    throw new JistErrorException("Could not resolve the artifact " + _artifact + " to a local jar.");
                }
            }
            else {
                File file = new File(_jar);
                if (!file.exists()) {
                    throw new JistErrorException("The referenced jar " + _jar + " could not be found.");
                }
            }

            return _jar;
        }
    }
}