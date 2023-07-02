package app.miyuki.miyukidependencydownloader.dependency.impl;

import app.miyuki.miyukidependencydownloader.dependency.Dependency;
import app.miyuki.miyukidependencydownloader.relocation.Relocation;
import app.miyuki.miyukidependencydownloader.repository.Repository;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;


public class URLDependency extends Dependency {

    private final String url;


    public URLDependency(int priority, @NotNull String name, @NotNull String version, @NotNull String url, List<@NotNull Relocation> relocations) {
        super(priority, name, version, relocations);
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
