package app.miyuki.miyukidependencydownloader.inject.impl;

import app.miyuki.miyukidependencydownloader.dependency.Dependency;
import app.miyuki.miyukidependencydownloader.exception.DependencyInjectException;
import app.miyuki.miyukidependencydownloader.inject.Injectable;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;

public class Reflection extends Injectable {

    private final Method addUrlMethod;


    public Reflection(@NotNull URLClassLoader classLoader, @NotNull Path defaultPath) {
        super(classLoader, defaultPath);

        Method addUrlMethod;

        try {
            addUrlMethod = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            addUrlMethod.setAccessible(true);
        } catch (Exception exception) {
            addUrlMethod = null;
        }

        this.addUrlMethod = addUrlMethod;
    }

    @Override
    public void inject(@NotNull Dependency dependency) throws DependencyInjectException {
        try {
            synchronized (addUrlMethod) {
                addUrlMethod.invoke(this.classLoader, dependency.getRelocationPath(defaultPath).toUri().toURL());
            }
        } catch (IllegalAccessException | InvocationTargetException | MalformedURLException exception) {
            throw new DependencyInjectException("Failed to inject dependency " + dependency.getArtifact(), exception);
        }
    }

    @Override
    public boolean isSupported() {
        return addUrlMethod != null;
    }

}
