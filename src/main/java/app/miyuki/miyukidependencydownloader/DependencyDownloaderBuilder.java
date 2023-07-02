package app.miyuki.miyukidependencydownloader;

import app.miyuki.miyukidependencydownloader.dependency.Dependency;
import app.miyuki.miyukidependencydownloader.dependency.impl.MavenDependency;
import app.miyuki.miyukidependencydownloader.downloader.Downloader;
import app.miyuki.miyukidependencydownloader.relocation.Relocation;
import app.miyuki.miyukidependencydownloader.repository.Repository;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

public class DependencyDownloaderBuilder {

    private Path defaultPath;

    private List<Repository> repositories;

    private List<Relocation> relocations;

    private List<Dependency> dependencies;

    private ClassLoader classLoader;

    private ExecutorService executorService;

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

    public DependencyDownloaderBuilder executor(@NotNull ExecutorService executorService) {
        this.executorService = executorService;
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

    DependencyDownloader build() {
        if (dependencies.isEmpty())
            throw new IllegalArgumentException("Dependencies is empty");

        if (repositories.isEmpty())
            throw new IllegalArgumentException("Repositories is empty");

        if (classLoader == null)
            throw new IllegalArgumentException("ClassLoader is null");

        if (defaultPath == null)
            throw new IllegalArgumentException("Path is null");

        List<Dependency> filteredDuplicateDependencies = dependencies.stream()
                .filter(MavenDependency.class::isInstance)
                .collect(Collectors.groupingBy(it -> ((MavenDependency) it).getGroup() + ":" + it.getArtifact()))
                .values()
                .stream()
                .map(it -> it.get(0))
                .collect(Collectors.toList());

        List<Repository> fixedRepositories = repositories.stream()
                .map(it -> it.getRepository().endsWith("/") ? it : Repository.of(it.getRepository() + "/"))
                .collect(Collectors.toList());

        return new DependencyDownloader(
                defaultPath,
                fixedRepositories,
                filteredDuplicateDependencies,
                relocations,
                classLoader,
                executorService
        );
    }


}
