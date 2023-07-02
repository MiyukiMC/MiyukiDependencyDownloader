package app.miyuki.miyukidependencydownloader.dependency;

import app.miyuki.miyukidependencydownloader.dependency.impl.MavenDependency;
import app.miyuki.miyukidependencydownloader.dependency.impl.URLDependency;
import app.miyuki.miyukidependencydownloader.relocation.Relocation;
import app.miyuki.miyukidependencydownloader.repository.Repository;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class Dependency {

    public static Dependency maven(
            int priority,
            @NotNull String group,
            @NotNull String artifact,
            @NotNull String version,
            List<Relocation> relocations
    ) {
        return new MavenDependency(priority, group, artifact, version, relocations);
    }

    public static Dependency maven(
            @NotNull String group,
            @NotNull String artifact,
            @NotNull String version,
            List<Relocation> relocations
    ) {
        return new MavenDependency(0, group, artifact, version, relocations);
    }

    public static Dependency maven(
            int priority,
            @NotNull String group,
            @NotNull String artifact,
            @NotNull String version
    ) {
        return new MavenDependency(priority, group, artifact, version, new ArrayList<>());
    }

    public static Dependency maven(
            @NotNull String group,
            @NotNull String artifact,
            @NotNull String version
    ) {
        return new MavenDependency(0, group, artifact, version, new ArrayList<>());
    }

    public static Dependency url(
            int priority,
            @NotNull String name,
            @NotNull String version,
            @NotNull String url,
            List<Relocation> relocations
    ) {
        return new URLDependency(priority, name, version, url, relocations);
    }

    public static Dependency url(
            int priority,
            @NotNull String name,
            @NotNull String version,
            @NotNull String url
    ) {
        return new URLDependency(priority, name, version, url, new ArrayList<>());
    }

    protected final int priority;

    protected final String artifact;

    protected final String version;

    protected final List<Relocation> relocations;

    protected Dependency(int priority, @NotNull String artifact, @NotNull String version, List<Relocation> relocations) {
        this.priority = priority;
        this.artifact = artifact;
        this.version = version;
        this.relocations = relocations;
    }

    @Nullable
    public abstract String getUrl(@Nullable Repository repository);

    @NotNull
    public abstract Path getDownloadPath(@NotNull Path defaultPath);

    @NotNull
    public abstract Path getRelocationPath(@NotNull Path defaultPath);

    public int getPriority() {
        return priority;
    }

    public String getArtifact() {
        return artifact;
    }

    public String getVersion() {
        return version;
    }

    public List<Relocation> getRelocations() {
        return relocations;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Dependency that = (Dependency) o;
        return priority == that.priority && Objects.equals(artifact, that.artifact) && Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(priority, artifact, version);
    }

    @Override
    public String toString() {
        return "Dependency{" +
                "priority=" + priority +
                ", artifact='" + artifact + '\'' +
                ", version='" + version + '\'' +
                '}';
    }

}
