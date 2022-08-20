package app.miyuki.miyukidependencydownloader;

import app.miyuki.miyukidependencydownloader.dependency.Dependency;
import app.miyuki.miyukidependencydownloader.dependency.impl.MavenDependency;
import app.miyuki.miyukidependencydownloader.downloader.Downloader;
import app.miyuki.miyukidependencydownloader.relocation.Relocation;
import app.miyuki.miyukidependencydownloader.repository.Repository;
import lombok.SneakyThrows;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DependencyDownloaderBuilder {

    private Path defaultPath;

    private List<Repository> repositories;

    private List<Relocation> relocations;

    private List<Dependency> dependencies;

    private ClassLoader classLoader;

    DependencyDownloaderBuilder() {
        this.defaultPath = null;
        this.repositories = new ArrayList<>(
                Arrays.asList(
                        Repository.of("https://repo.maven.apache.org/maven2/"),
                        Repository.of("https://dl.google.com/dl/android/maven2/")
                )
        );
        this.relocations = new ArrayList<>();
        this.dependencies = new ArrayList<>();
        this.classLoader = ClassLoader.getSystemClassLoader().getParent();
    }

    public DependencyDownloaderBuilder path(@NotNull Path path) {
        this.defaultPath = path;
        return this;
    }


    public DependencyDownloaderBuilder dependency(@NotNull Dependency dependency) {
        dependencies.add(dependency);
        return this;
    }

    public DependencyDownloaderBuilder dependencies(@NotNull Dependency... dependencies) {
        this.dependencies = Arrays.asList(dependencies);
        return this;
    }

    public DependencyDownloaderBuilder dependencies(@NotNull List<Dependency> dependencies) {
        this.dependencies = dependencies;
        return this;
    }

    public DependencyDownloaderBuilder repository(@NotNull Repository repository) {
        repositories.add(repository);
        return this;
    }

    public DependencyDownloaderBuilder repositories(@NotNull Repository... repositories) {
        this.repositories = Arrays.asList(repositories);
        return this;
    }

    public DependencyDownloaderBuilder repositories(@NotNull List<Repository> repositories) {
        this.repositories = repositories;
        return this;
    }

    public DependencyDownloaderBuilder relocation(@NotNull Relocation relocation) {
        relocations.add(relocation);
        return this;
    }

    public DependencyDownloaderBuilder relocations(@NotNull Relocation... relocations) {
        this.relocations = Arrays.asList(relocations);
        return this;
    }

    public DependencyDownloaderBuilder relocations(@NotNull List<Relocation> relocations) {
        this.relocations = relocations;
        return this;
    }

    public DependencyDownloaderBuilder classLoader(@NotNull ClassLoader classLoader) {
        this.classLoader = classLoader;
        return this;
    }

    @SneakyThrows
    public DependencyDownloader build() {
        if (dependencies.isEmpty())
            throw new IllegalArgumentException("Dependencies is empty");

        if (repositories.isEmpty())
            throw new IllegalArgumentException("Repositories is empty");

        if (classLoader == null)
            throw new IllegalArgumentException("ClassLoader is null");

        if (defaultPath == null)
            throw new IllegalArgumentException("Path is null");

        val filteredDuplicateDependencies = new ArrayList<Dependency>();
        for (Dependency dependency : dependencies) {

            val duplicate = filteredDuplicateDependencies
                    .stream()
                    .filter(it -> it.getArtifact().equals(dependency.getArtifact()))
                    .filter(it -> !(it instanceof MavenDependency) || ((MavenDependency) it).getGroup().equals(((MavenDependency) dependency).getGroup()))
                    .findFirst();

            if (duplicate.isPresent())
                continue;

            filteredDuplicateDependencies.add(dependency);
        }

        val fixedRepositories = new ArrayList<Repository>();
        for (Repository repository : repositories) {
            if (repository.getRepository().endsWith("/"))
                fixedRepositories.add(repository);
            else
                fixedRepositories.add(Repository.of(repository.getRepository() + "/"));
        }

        val fixedRelocations = new ArrayList<Relocation>();
        for (Relocation relocation : relocations) {
            fixedRelocations.add(
                    Relocation.of(
                            relocation.getFrom().replace("#", "."),
                            relocation.getTo().replace("#", ".")
                    )
            );
        }

        return new DependencyDownloader(
                defaultPath,
                new Downloader(filteredDuplicateDependencies, fixedRepositories, defaultPath),
                fixedRepositories,
                filteredDuplicateDependencies,
                fixedRelocations,
                classLoader
        );
    }


}
