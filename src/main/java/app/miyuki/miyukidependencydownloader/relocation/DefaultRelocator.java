package app.miyuki.miyukidependencydownloader.relocation;

import app.miyuki.miyukidependencydownloader.DependencyDownloader;
import app.miyuki.miyukidependencydownloader.classloader.IsolatedClassloader;
import app.miyuki.miyukidependencydownloader.dependency.Dependency;
import app.miyuki.miyukidependencydownloader.exception.DependencyRelocationException;
import app.miyuki.miyukidependencydownloader.repository.Repository;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class DefaultRelocator implements Relocator {

    private static final String JAR_RELOCATOR_CLASS = "me#lucko#jarrelocator#JarRelocator";

    private static final String RELOCATION_CLASS = "me#lucko#jarrelocator#Relocation";

    private static final String JAR_RELOCATOR_RUN_METHOD = "run";

    private final Constructor<?> jarRelocatorConstructor;

    private final Constructor<?> relocationConstructor;

    private final Method jarRelocatorRunMethod;


    private final IsolatedClassloader classLoader;

    private final DependencyDownloader dependencyDownloader;


    public DefaultRelocator(@NotNull DependencyDownloader dependencyDownloader) throws DependencyRelocationException {
        this.dependencyDownloader = dependencyDownloader;

        this.classLoader = DependencyDownloader.isolated(builder ->
                builder.classLoader(dependencyDownloader.getClassLoader())
                        .repositories(
                                Repository.of("https://repo.maven.apache.org/maven2/"),
                                Repository.of("https://repo1.maven.org/maven2/")
                        )
                        .dependencies(
                                Dependency.maven("org#ow2#asm", "asm-commons", "9.2"),
                                Dependency.maven("org#ow2#asm", "asm", "9.2"),
                                Dependency.maven("me#lucko", "jar-relocator", "1.7")
                        )
                        .relocations(new ArrayList<>())
                        .path(dependencyDownloader.getDefaultPath())
                        .executor(dependencyDownloader.getExecutorService())
        )
                .exceptionally(throwable -> {
                    throwable.printStackTrace();
                    return null;
                }).join();

        if (this.classLoader == null) {
            throw new IllegalStateException("Occurred an error while creating isolated classloader for JarRelocator");
        }

        try {
            Class<?> relocationClass = classLoader.loadClass(RELOCATION_CLASS.replace("#", "."));
            relocationConstructor = relocationClass.getDeclaredConstructor(String.class, String.class, Collection.class, Collection.class);

            Class<?> jarRelocatorClass = classLoader.loadClass(JAR_RELOCATOR_CLASS.replace("#", "."));

            this.jarRelocatorConstructor = jarRelocatorClass.getDeclaredConstructor(File.class, File.class, Collection.class);
            this.jarRelocatorConstructor.setAccessible(true);

            this.jarRelocatorRunMethod = jarRelocatorClass.getDeclaredMethod(JAR_RELOCATOR_RUN_METHOD);
            this.jarRelocatorRunMethod.setAccessible(true);
        } catch (Exception exception) {
            throw new DependencyRelocationException("Failed to load JarRelocator", exception);
        }
    }

    public boolean relocate(@NotNull Dependency dependency) {
        try {
            Path downloadPath = dependency.getDownloadPath(dependencyDownloader.getDefaultPath());
            if (!Files.exists(downloadPath))
                return false;

            Path relocationPath = dependency.getRelocationPath(dependencyDownloader.getDefaultPath());
            if (Files.exists(relocationPath))
                return true;

            List<Relocation> relocations = new ArrayList<>(dependency.getRelocations());
            relocations.addAll(dependencyDownloader.getRelocations());
            if (relocations.isEmpty())
                return false;

            List<Object> luckoRelocation = relocations
                    .stream()
                    .map(relocation -> {
                        try {
                            return relocationConstructor.newInstance(
                                    relocation.getFrom().replace("#", "."),
                                    relocation.getTo().replace("#", "."),
                                    relocation.getInclusions().stream().map(it -> it.replace("#", ".")).collect(Collectors.toSet()),
                                    relocation.getExclusions().stream().map(it -> it.replace("#", ".")).collect(Collectors.toSet())
                            );
                        } catch (InstantiationException | InvocationTargetException | IllegalAccessException e) {
                            throw new RuntimeException(e);
                        }
                    }).collect(Collectors.toList());

            Object relocator = this.jarRelocatorConstructor.newInstance(
                    downloadPath.toFile(),
                    relocationPath.toFile(),
                    luckoRelocation
            );

            this.jarRelocatorRunMethod.invoke(relocator);
            return true;
        } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
            return false;
        }
    }

    @Override
    public boolean isSupported() {
        return true;
    }

    @Override
    public void close() {
        try {
            this.classLoader.close();
        } catch (IOException exception) {
            throw new RuntimeException("Failed to close relocation classloader", exception);
        }
    }

}
