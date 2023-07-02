package app.miyuki.miyukidependencydownloader;

import app.miyuki.miyukidependencydownloader.classloader.IsolatedClassloader;
import app.miyuki.miyukidependencydownloader.dependency.Dependency;
import app.miyuki.miyukidependencydownloader.downloader.Downloader;
import app.miyuki.miyukidependencydownloader.exception.DependencyDownloadException;
import app.miyuki.miyukidependencydownloader.inject.Injector;
import app.miyuki.miyukidependencydownloader.relocation.Relocation;
import app.miyuki.miyukidependencydownloader.relocation.DefaultRelocator;
import app.miyuki.miyukidependencydownloader.relocation.Relocator;
import app.miyuki.miyukidependencydownloader.relocation.ShadedRelocator;
import app.miyuki.miyukidependencydownloader.repository.Repository;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class DependencyDownloader {

    public static CompletableFuture<@Nullable IsolatedClassloader> isolated(Consumer<DependencyDownloaderBuilder> builder) {
        DependencyDownloaderBuilder dependencyDownloaderBuilder = new DependencyDownloaderBuilder();
        builder.accept(dependencyDownloaderBuilder);
        DependencyDownloader dependencyDownloader = dependencyDownloaderBuilder.build();
        return dependencyDownloader.isolate();
    }

    public static CompletableFuture<Boolean> inject(Consumer<DependencyDownloaderBuilder> builder) {
        DependencyDownloaderBuilder dependencyDownloaderBuilder = new DependencyDownloaderBuilder();
        builder.accept(dependencyDownloaderBuilder);
        DependencyDownloader dependencyDownloader = dependencyDownloaderBuilder.build();
        return dependencyDownloader.inject();
    }

    private final Path defaultPath;

    private final Downloader downloader;

    private final List<Repository> repositories;

    private final List<Dependency> dependencies;

    private final List<Relocation> relocations;

    private final ClassLoader classLoader;

    private final ExecutorService executorService;


    DependencyDownloader(@NotNull Path defaultPath, @NotNull List<Repository> repositories, @NotNull List<Dependency> dependencies, @NotNull List<Relocation> relocations, @NotNull ClassLoader classLoader, @NotNull ExecutorService executorService) {
        this.defaultPath = defaultPath;
        this.repositories = repositories;
        this.dependencies = dependencies;
        this.relocations = relocations;
        this.classLoader = classLoader;
        this.executorService = executorService;

        this.downloader = new Downloader(this);
    }

    private CompletableFuture<@Nullable IsolatedClassloader> isolate() {
        return download(false)
                .thenApplyAsync(downloadedDependencies -> {
                    if (downloadedDependencies.isEmpty()) {
                        return null;
                    }

                    IsolatedClassloader isolatedClassloader = new IsolatedClassloader(this.classLoader);

                    downloadedDependencies.forEach(dependency -> {
                        try {
                            isolatedClassloader.addURL(dependency.getDownloadPath(defaultPath).toUri().toURL());
                        } catch (MalformedURLException exception) {
                            exception.printStackTrace();
                        }
                    });

                    return isolatedClassloader;

                })
                .exceptionally(exception -> {
                    exception.printStackTrace();
                    return null;
                });
    }

    public CompletableFuture<Boolean> inject() {
        return download(true)
                .thenApplyAsync(downloadedDependencies -> {
                    Injector injector = new Injector(this.classLoader, this.defaultPath);

                    downloadedDependencies.forEach(injector::inject);

                    return downloadedDependencies;
                }, getExecutorService())
                .handle((injected, exception) -> {
                    if (exception != null) {
                        exception.printStackTrace();
                        return false;
                    }

                    return injected.size() == dependencies.size();
                });
    }


    private CompletableFuture<List<Dependency>> download(boolean relocate) {
        return downloader.downloadAll()
                .thenApplyAsync(downloadedDependencies -> {
                    List<Dependency> pendentDependencies = this.dependencies
                            .stream()
                            .filter(dependency -> !downloadedDependencies.contains(dependency))
                            .collect(Collectors.toList());

                    if (pendentDependencies.size() > 0) {
                        throw new DependencyDownloadException(
                                "Some dependencies were not downloaded!\n" +
                                        "Dependencies: " + pendentDependencies
                                        .stream()
                                        .map(Dependency::getArtifact)
                                        .collect(Collectors.joining(","))
                                        + "\n" +
                                        "Repositories: " + repositories
                                        .stream()
                                        .map(Repository::getRepository)
                                        .collect(Collectors.joining("\n"))
                        );
                    }

                    return downloadedDependencies;
                }, getExecutorService())
                .exceptionally(exception -> {
                    exception.printStackTrace();
                    return new ArrayList<>();
                })
                .thenApplyAsync(downloadedDependencies -> {
                    if (relocate && !downloadedDependencies.isEmpty()) {

                        Relocator relocator = new ShadedRelocator(this);
                        if (!relocator.isSupported()) {
                            relocator = new DefaultRelocator(this);
                        }

                        try (Relocator finalRelocator = relocator) {

                            List<CompletableFuture<Boolean>> relocating = new ArrayList<>();
                            downloadedDependencies
                                    .forEach(dependency ->
                                            relocating.add(CompletableFuture.supplyAsync(() -> finalRelocator.relocate(dependency)))
                                    );

                            CompletableFuture.allOf(relocating.toArray(new CompletableFuture[0])).join();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                    return downloadedDependencies;
                }, getExecutorService())
                .exceptionally(exception -> {
                    exception.printStackTrace();
                    return new ArrayList<>();
                });
    }

    public Path getDefaultPath() {
        return defaultPath;
    }

    public Downloader getDownloader() {
        return downloader;
    }

    public List<Repository> getRepositories() {
        return repositories;
    }

    public List<Dependency> getDependencies() {
        return dependencies;
    }

    public List<Relocation> getRelocations() {
        return relocations;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }
}
