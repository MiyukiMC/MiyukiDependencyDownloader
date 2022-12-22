package app.miyuki.miyukidependencydownloader.downloader;

import app.miyuki.miyukidependencydownloader.dependency.Dependency;
import app.miyuki.miyukidependencydownloader.repository.Repository;
import lombok.Cleanup;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class Downloader {

    private final List<Dependency> dependencies;

    private final List<Repository> repositories;

    private final Path defaultPath;

    public Downloader(@NotNull List<Dependency> dependencies, @NotNull List<Repository> repositories, @NotNull Path defaultPath) {
        this.dependencies = dependencies;
        this.repositories = repositories;
        this.defaultPath = defaultPath;
    }

    public CompletableFuture<List<Dependency>> downloadAll() {
        return CompletableFuture.supplyAsync(() -> {

            val downloadedDependencies = new ArrayList<Dependency>();
            val downloadingDependencies = new ArrayList<CompletableFuture<Void>>();

            for (Dependency dependency : dependencies) {

                downloadingDependencies.add(
                        CompletableFuture.runAsync(() -> {
                            for (Repository repository : repositories) {
                                if (download(dependency, repository, defaultPath)) {
                                    downloadedDependencies.add(dependency);
                                    break;
                                }
                            }
                        })
                );

            }
            CompletableFuture.allOf(downloadingDependencies.toArray(new CompletableFuture[0])).join();
            return downloadedDependencies;
        });
    }


    protected boolean download(@NotNull Dependency dependency, @NotNull Repository repository, @NotNull Path defaultPath) {
        val dependencyUrl = dependency.getUrl(repository);
        if (dependencyUrl == null)
            return false;

        val dependencyPath = dependency.getDownloadPath(defaultPath);
        if (Files.exists(dependencyPath))
            return true;

        HttpURLConnection connection = null;
        try {

            val url = new URL(dependencyUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(20000);
            connection.setReadTimeout(20000);
            connection.connect();
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
                return false;

            @Cleanup val inputStream = new BufferedInputStream(connection.getInputStream());

            val dependencyFolder = dependencyPath.getParent();
            if (Files.notExists(dependencyFolder))
                Files.createDirectories(dependencyFolder);

            Files.copy(inputStream, dependencyPath);
            return true;
        } catch (IOException exception) {
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

}
