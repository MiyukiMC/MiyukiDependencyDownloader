package app.miyuki.miyukidependencydownloader.dependency;

import app.miyuki.miyukidependencydownloader.dependency.impl.MavenDependency;
import app.miyuki.miyukidependencydownloader.dependency.impl.URLDependency;
import app.miyuki.miyukidependencydownloader.repository.Repository;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

@Getter
public abstract class Dependency {

    public static Dependency maven(
            int priority,
            @NotNull String group,
            @NotNull String artifact,
            @NotNull String version
    ) {
        return new MavenDependency(priority, group, artifact, version);
    }

    public static Dependency maven(
            @NotNull String group,
            @NotNull String artifact,
            @NotNull String version
    ) {
        return new MavenDependency(0, group, artifact, version);
    }


    public static Dependency url(
            int priority,
            @NotNull String name,
            @NotNull String version,
            @NotNull String url
    ) {
        return new URLDependency(priority, name, version, url);
    }

    protected final int priority;

    protected final String artifact;

    protected final String version;

    protected Dependency(int priority, @NotNull String artifact, @NotNull String version) {
        this.priority = priority;
        this.artifact = artifact;
        this.version = version;
    }

    @Nullable
    abstract public String getUrl(@Nullable Repository repository);

    @NotNull
    abstract public Path getDownloadPath(@NotNull Path defaultPath);

    @NotNull
    abstract public Path getRelocationPath(@NotNull Path defaultPath);


}
