package app.miyuki.miyukidependencydownloader;

import app.miyuki.miyukidependencydownloader.classloader.IsolatedClassloader;
import app.miyuki.miyukidependencydownloader.dependency.Dependency;
import app.miyuki.miyukidependencydownloader.downloader.Downloader;
import app.miyuki.miyukidependencydownloader.exception.DependencyDownloadException;
import app.miyuki.miyukidependencydownloader.exception.DependencyInjectException;
import app.miyuki.miyukidependencydownloader.inject.Injector;
import app.miyuki.miyukidependencydownloader.relocation.Relocation;
import app.miyuki.miyukidependencydownloader.relocation.Relocator;
import app.miyuki.miyukidependencydownloader.repository.Repository;
import lombok.Data;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Data
public class DependencyDownloader {

    public static DependencyDownloaderBuilder builder() {
        return new DependencyDownloaderBuilder();
    }

    private final @NotNull Path defaultPath;

    private final @NotNull Downloader downloader;

    private final @NotNull List<Repository> repositories;

    private final @NotNull List<Dependency> dependencies;

    private final @NotNull List<Relocation> relocations;

    private final @NotNull ClassLoader classLoader;

    public CompletableFuture<@Nullable IsolatedClassloader> isolate() {
        return download(false)
                .thenApply(downloadedDependencies -> {
                    if (downloadedDependencies.isEmpty()) {
                        return null;
                    }

                    IsolatedClassloader isolatedClassloader = new IsolatedClassloader(this.classLoader);

                    val sortedDependencies = downloadedDependencies.stream()
                            .sorted(Comparator.comparing(Dependency::getPriority))
                            .collect(Collectors.toList());

                    for (Dependency dependency : sortedDependencies) {
                        try {
                            isolatedClassloader.addURL(dependency.getDownloadPath(defaultPath).toUri().toURL());
                        } catch (MalformedURLException exception) {
                            throw new DependencyInjectException("Failed to add dependency to classloader", exception);
                        }
                    }

                    return isolatedClassloader;

                })
                .exceptionally(exception -> {
                    exception.printStackTrace();
                    return null;
                });
    }

    public CompletableFuture<Boolean> inject() {
        return download(true)
                .thenApply(downloadedDependencies -> {
                    val injector = new Injector(this.classLoader, this.defaultPath);

                    val sortedDependencies = downloadedDependencies.stream()
                            .sorted(Comparator.comparing(Dependency::getPriority))
                            .collect(Collectors.toList());

                    for (Dependency dependency : sortedDependencies) {
                        injector.inject(dependency);
                    }

                    return downloadedDependencies;
                })
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
                .thenApply(downloadedDependencies -> {
                    val pendentDependencies = this.dependencies
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
                })
                .exceptionally(exception -> {
                    exception.printStackTrace();
                    return new ArrayList<>();
                })
                .thenApplyAsync(downloadedDependencies -> {
                    if (relocate && !downloadedDependencies.isEmpty()) {
                        try (val relocator = new Relocator(this.defaultPath, this.relocations)) {
                            List<CompletableFuture<Boolean>> relocating = new ArrayList<>();
                            downloadedDependencies
                                    .forEach(dependency ->
                                            relocating.add(CompletableFuture.supplyAsync(() -> relocator.relocate(dependency)))
                                    );
                            CompletableFuture.allOf(relocating.toArray(new CompletableFuture[0])).join();

                        }
                    }
                    return downloadedDependencies;
                });
    }

}
