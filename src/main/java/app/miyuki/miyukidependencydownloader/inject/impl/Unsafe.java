package app.miyuki.miyukidependencydownloader.inject.impl;

import app.miyuki.miyukidependencydownloader.dependency.Dependency;
import app.miyuki.miyukidependencydownloader.exception.DependencyInjectException;
import app.miyuki.miyukidependencydownloader.inject.Injectable;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Collection;

public class Unsafe extends Injectable {

    private final sun.misc.Unsafe unsafe;

    private final Collection<URL> unopenedURLs;
    private final Collection<URL> pathURLs;

    public Unsafe(@NotNull URLClassLoader classLoader, @NotNull Path defaultPath) {
        super(classLoader, defaultPath);

        sun.misc.Unsafe unsafe;
        Collection<URL> unopenedURLs;
        Collection<URL> pathURLs;

        try {
            Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            unsafe = (sun.misc.Unsafe) unsafeField.get(null);

            Object ucp = fetchField(unsafe, URLClassLoader.class, classLoader, "ucp");

            unopenedURLs = (Collection<URL>) fetchField(unsafe, ucp.getClass(), ucp, "unopenedUrls");
            pathURLs = (Collection<URL>) fetchField(unsafe, ucp.getClass(), ucp, "path");
        } catch (Exception exception) {
            unsafe = null;
            unopenedURLs = null;
            pathURLs = null;
        }

        this.unsafe = unsafe;
        this.unopenedURLs = unopenedURLs;
        this.pathURLs = pathURLs;
    }

    @Override
    public void inject(@NotNull Dependency dependency) throws DependencyInjectException {
        try {
            URL url = dependency.getRelocationPath(defaultPath).toUri().toURL();
            synchronized (unopenedURLs) {
                unopenedURLs.add(url);
                pathURLs.add(url);
            }
        } catch (MalformedURLException exception) {
            throw new DependencyInjectException("Failed to inject dependency " + dependency.getArtifact(), exception);
        }
    }

    @Override
    public boolean isSupported() {
        return unsafe != null && unopenedURLs != null && pathURLs != null;
    }

    private Object fetchField(final sun.misc.Unsafe unsafe, final Class<?> clazz, final Object object, final String name) throws NoSuchFieldException {
        Field field = clazz.getDeclaredField(name);
        long offset = unsafe.objectFieldOffset(field);
        return unsafe.getObject(object, offset);
    }

}
