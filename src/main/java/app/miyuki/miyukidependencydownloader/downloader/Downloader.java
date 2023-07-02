package app.miyuki.miyukidependencydownloader.downloader;

import app.miyuki.miyukidependencydownloader.DependencyDownloader;
import app.miyuki.miyukidependencydownloader.dependency.Dependency;
import app.miyuki.miyukidependencydownloader.helper.ConnectionHelper;
import app.miyuki.miyukidependencydownloader.repository.Repository;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class Downloader {

    private final DependencyDownloader dependencyDownloader;

    public Downloader(@NotNull DependencyDownloader dependencyDownloader) {
        this.dependencyDownloader = dependencyDownloader;
    }

    public CompletableFuture<List<Dependency>> downloadAll() {
        return CompletableFuture.supplyAsync(() -> {

            List<Dependency> downloadedDependencies = new ArrayList<>();

            for (List<Dependency> dependencies : groupByPriority(dependencyDownloader.getDependencies()).values()) {
                List<CompletableFuture<Void>> downloadingDependencies = new ArrayList<>();

                for (Dependency dependency : dependencies) {
                    downloadingDependencies.add(
                            CompletableFuture.runAsync(() -> {
                                for (Repository repository : dependencyDownloader.getRepositories()) {
                                    if (download(dependency, repository, dependencyDownloader.getDefaultPath())) {
                                        synchronized (downloadedDependencies) {
                                            downloadedDependencies.add(dependency);
                                        }
                                        break;
                                    }
                                }
                            }, dependencyDownloader.getExecutorService())
                    );

                    CompletableFuture.allOf(downloadingDependencies.toArray(new CompletableFuture[0])).join();
                }
            }

            return downloadedDependencies;
        }, dependencyDownloader.getExecutorService())
                .exceptionally(throwable -> {
                    throwable.printStackTrace();
                    return null;
                });
    }


    private Map<Integer, List<Dependency>> groupByPriority(List<Dependency> dependencies) {
        Map<Integer, List<@NotNull Dependency>> priorities = new HashMap<>();
        for (Dependency dependency : dependencies) {
            int priority = dependency.getPriority();
            priorities.computeIfAbsent(priority, k -> new ArrayList<>()).add(dependency);
        }
        return priorities;
    }

    private boolean download(@NotNull Dependency dependency, @NotNull Repository repository, @NotNull Path defaultPath) {
        String dependencyUrl = dependency.getUrl(repository);
        if (dependencyUrl == null)
            return false;

        Path dependencyPath = dependency.getDownloadPath(defaultPath);
        if (Files.exists(dependencyPath))
            return true;

        try {

            HttpURLConnection connection = ConnectionHelper.createConnection(dependencyUrl);

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
                return false;

            try (BufferedInputStream inputStream = new BufferedInputStream(connection.getInputStream())) {

                Path dependencyFolder = dependencyPath.getParent();
                if (Files.notExists(dependencyFolder))
                    Files.createDirectories(dependencyFolder);

                Files.copy(inputStream, dependencyPath);
                return true;
            }
        } catch (IOException exception) {
            exception.printStackTrace();
            return false;
        }
    }


}
