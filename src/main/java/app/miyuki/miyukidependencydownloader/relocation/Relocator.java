package app.miyuki.miyukidependencydownloader.relocation;

import app.miyuki.miyukidependencydownloader.DependencyDownloader;
import app.miyuki.miyukidependencydownloader.classloader.IsolatedClassloader;
import app.miyuki.miyukidependencydownloader.dependency.Dependency;
import app.miyuki.miyukidependencydownloader.exception.DependencyRelocationException;
import app.miyuki.miyukidependencydownloader.repository.Repository;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Relocator implements AutoCloseable {

    private static final String JAR_RELOCATOR_CLASS = "me.lucko.jarrelocator.JarRelocator";
    private static final String JAR_RELOCATOR_RUN_METHOD = "run";

    private final Constructor<?> jarRelocatorConstructor;
    private final Method jarRelocatorRunMethod;

    private final Path defaultPath;

    private final List<Relocation> relocations;

    private final IsolatedClassloader classLoader;

    public Relocator(@NotNull Path defaultPath, List<Relocation> relocations) throws DependencyRelocationException {
        this.defaultPath = defaultPath;
        this.relocations = relocations;

        this.classLoader = DependencyDownloader.builder()
                .classLoader(Relocator.class.getClassLoader())
                .repositories(
                        Repository.of("https://repo.maven.apache.org/maven2/"),
                        Repository.of("https://repo1.maven.org/maven2/")
                )
                .dependencies(
                        Dependency.maven("org#ow2#asm", "asm-commons", "9.2"),
                        Dependency.maven("org#ow2#asm", "asm", "9.2"),
                        Dependency.maven("me#lucko", "jar-relocator", "1.5")
                )
                .path(defaultPath)
                .build()
                .isolate()
                .join();

        if (this.classLoader == null) {
            throw new IllegalStateException("Occurred an error while creating isolated classloader for JarRelocator");
        }

        try {
            val jarRelocatorClass = classLoader.loadClass(JAR_RELOCATOR_CLASS);

            this.jarRelocatorConstructor = jarRelocatorClass.getDeclaredConstructor(File.class, File.class, Map.class);
            this.jarRelocatorConstructor.setAccessible(true);

            this.jarRelocatorRunMethod = jarRelocatorClass.getDeclaredMethod(JAR_RELOCATOR_RUN_METHOD);
            this.jarRelocatorRunMethod.setAccessible(true);
        } catch (Exception exception) {
            throw new DependencyRelocationException("Failed to load JarRelocator", exception);
        }
    }

    public boolean relocate(@NotNull Dependency dependency) {
        val relocations = this.relocations.stream().collect(Collectors.toMap(Relocation::getFrom, Relocation::getTo));
        try {
            val downloadPath = dependency.getDownloadPath(defaultPath);
            if (!Files.exists(downloadPath))
                return false;

            val relocationPath = dependency.getDownloadPath(defaultPath);
            if (Files.exists(relocationPath))
                return true;

            Object relocator = this.jarRelocatorConstructor.newInstance(
                    dependency.getDownloadPath(defaultPath).toFile(),
                    dependency.getRelocationPath(defaultPath).toFile(),
                    relocations
            );
            this.jarRelocatorRunMethod.invoke(relocator);
            return true;
        } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
            return false;
        }
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
