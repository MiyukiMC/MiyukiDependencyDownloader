package app.miyuki.miyukidependencydownloader.inject;

import app.miyuki.miyukidependencydownloader.dependency.Dependency;
import app.miyuki.miyukidependencydownloader.exception.DependencyInjectException;
import app.miyuki.miyukidependencydownloader.exception.DependencyInjectNotSupportedException;
import app.miyuki.miyukidependencydownloader.inject.impl.Reflection;
import app.miyuki.miyukidependencydownloader.inject.impl.Unsafe;
import org.jetbrains.annotations.NotNull;

import java.net.URLClassLoader;
import java.nio.file.Path;

public class Injector {

    private final Reflection reflection;

    private final Unsafe unsafe;

    public Injector(@NotNull ClassLoader classLoader, @NotNull Path defaultPath) throws DependencyInjectNotSupportedException {

        if (!(classLoader instanceof URLClassLoader)) {
            throw new DependencyInjectNotSupportedException("ClassLoader is not URLClassLoader");
        }

        URLClassLoader urlClassLoader = (URLClassLoader) classLoader;
        this.reflection = new Reflection(urlClassLoader, defaultPath);
        this.unsafe = new Unsafe(urlClassLoader, defaultPath);
    }

    public void inject(@NotNull Dependency dependency) throws DependencyInjectException, DependencyInjectNotSupportedException {
        if (reflection.isSupported()) {
            reflection.inject(dependency);
        } else if (unsafe.isSupported()) {
            unsafe.inject(dependency);
        } else {
            throw new DependencyInjectNotSupportedException("Neither reflection nor unsafe is supported");
        }
    }


}
