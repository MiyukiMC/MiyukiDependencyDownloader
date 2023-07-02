package app.miyuki.miyukidependencydownloader.inject;

import app.miyuki.miyukidependencydownloader.dependency.Dependency;
import app.miyuki.miyukidependencydownloader.exception.DependencyInjectException;
import org.jetbrains.annotations.NotNull;

import java.net.URLClassLoader;
import java.nio.file.Path;

public abstract class Injectable {

    protected final URLClassLoader classLoader;

    protected final Path defaultPath;

    protected Injectable(@NotNull URLClassLoader classLoader, @NotNull Path defaultPath) {
        this.classLoader = classLoader;
        this.defaultPath = defaultPath;
    }

    public abstract void inject(@NotNull Dependency dependency) throws DependencyInjectException;

    public abstract boolean isSupported();

}
