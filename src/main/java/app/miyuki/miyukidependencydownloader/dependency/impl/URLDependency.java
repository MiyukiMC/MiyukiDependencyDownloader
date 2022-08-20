package app.miyuki.miyukidependencydownloader.dependency.impl;

import app.miyuki.miyukidependencydownloader.dependency.Dependency;
import app.miyuki.miyukidependencydownloader.repository.Repository;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;


public class URLDependency extends Dependency {

    private final String url;

    public URLDependency(int priority, @NotNull String name, @NotNull String version, @NotNull String url) {
        super(priority, name, version);
        this.url = url;
    }


    @Override
    public String getUrl(@Nullable Repository repository) {
        return url;
    }

    @Override
    public @NotNull Path getDownloadPath(@NotNull Path defaultPath) {
        return defaultPath
                .resolve(artifact)
                .resolve(version)
                .resolve(artifact + "-" + version + ".jar");
    }

    @Override
    public @NotNull Path getRelocationPath(@NotNull Path defaultPath) {
        return defaultPath
                .resolve(artifact)
                .resolve(version)
                .resolve(artifact + "-" + version + ".relocated.jar");
    }

}
