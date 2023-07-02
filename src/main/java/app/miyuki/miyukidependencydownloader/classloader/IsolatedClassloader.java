package app.miyuki.miyukidependencydownloader.classloader;

import java.net.URL;
import java.net.URLClassLoader;

public class IsolatedClassloader extends URLClassLoader {

    static {
        ClassLoader.registerAsParallelCapable();
    }

    public IsolatedClassloader(ClassLoader classLoader) {
        super(new URL[0], classLoader);
    }

    @Override
    public void addURL(URL url) {
        super.addURL(url);
    }

    @Override
    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        return super.loadClass(name, resolve);
    }

}
